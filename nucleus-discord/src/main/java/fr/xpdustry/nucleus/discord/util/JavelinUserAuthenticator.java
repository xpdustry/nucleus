/*
 * Nucleus, the software collection powering Xpdustry.
 * Copyright (C) 2022  Xpdustry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.xpdustry.nucleus.discord.util;

import fr.xpdustry.javelin.JavelinAuthenticator;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class JavelinUserAuthenticator implements JavelinAuthenticator {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int DERIVED_KEY_LENGTH = 256;
    private static final int ITERATIONS = 100000;

    private final Map<String, EncryptedPassword> users = new ConcurrentHashMap<>();
    private final Path file;

    public JavelinUserAuthenticator(final Path file) {
        this.file = file;

        if (Files.exists(this.file)) {
            try (final var input = new DataInputStream(new GZIPInputStream(Files.newInputStream(this.file)))) {
                final var entries = input.readInt();
                for (var i = 0; i < entries; i++) {
                    final var username = input.readUTF();
                    final var passLen = input.readUnsignedByte();
                    final var pass = input.readNBytes(passLen);
                    final var saltLen = input.readUnsignedByte();
                    final var salt = input.readNBytes(saltLen);
                    this.users.put(username, new EncryptedPassword(pass, salt));
                }
            } catch (final IOException e) {
                throw new RuntimeException("Unable to load user authenticator users.", e);
            }
        }
    }

    // TODO Move the UserAuthenticator, and it's simple implementation to Javelin core
    @SuppressWarnings("NullableProblems")
    @Override
    public boolean authenticate(final String username, final char[] password) {
        final var encrypted = this.users.get(username);
        return encrypted != null && encrypted.equals(getEncryptedPassword(password, encrypted.salt));
    }

    public void saveUser(final String username, final char[] password) {
        final var salt = generateSalt();
        this.users.put(username, getEncryptedPassword(password, salt));
        save();
    }

    public boolean existsUser(final String username) {
        return this.users.containsKey(username);
    }

    public long countUsers() {
        return this.users.size();
    }

    public List<String> findAllUsers() {
        return List.copyOf(this.users.keySet());
    }

    public void deleteUser(final String username) {
        if (this.users.remove(username) != null) {
            save();
        }
    }

    public void deleteAllUsers() {
        this.users.clear();
        save();
    }

    private synchronized void save() {
        try (final var output = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(this.file)))) {
            output.writeInt(this.users.size());
            for (final var entry : this.users.entrySet()) {
                output.writeUTF(entry.getKey());
                output.writeByte(entry.getValue().pass.length);
                output.write(entry.getValue().pass);
                output.writeByte(entry.getValue().salt.length);
                output.write(entry.getValue().salt);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Unable to save user authenticator users.", e);
        }
    }

    private EncryptedPassword getEncryptedPassword(final char[] password, final byte[] salt) {
        try {
            final var spec = new PBEKeySpec(password, salt, ITERATIONS, DERIVED_KEY_LENGTH);
            final var factory = SecretKeyFactory.getInstance(ALGORITHM);
            return new EncryptedPassword(factory.generateSecret(spec).getEncoded(), salt);
        } catch (final NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to generate an encrypted password.", e);
        }
    }

    private byte[] generateSalt() {
        try {
            final var random = SecureRandom.getInstance("SHA1PRNG");
            final var salt = new byte[8]; // NIST recommends minimum 4 bytes. We use 8.
            random.nextBytes(salt);
            return salt;
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate a salt.", e);
        }
    }

    private static final class EncryptedPassword {

        private final byte[] pass;
        private final byte[] salt;

        private EncryptedPassword(final byte[] pass, final byte[] salt) {
            this.pass = pass;
            this.salt = salt;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(pass), Arrays.hashCode(salt));
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof EncryptedPassword encrypted
                    && Arrays.equals(salt, encrypted.salt)
                    && Arrays.equals(pass, encrypted.pass);
        }
    }
}
