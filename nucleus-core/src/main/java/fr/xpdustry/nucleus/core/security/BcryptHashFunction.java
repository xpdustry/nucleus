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
package fr.xpdustry.nucleus.core.security;

import com.password4j.BcryptFunction;
import com.password4j.SecureString;
import com.password4j.types.Bcrypt;
import java.util.Arrays;
import java.util.Objects;

public final class BcryptHashFunction implements HashFunction {

    private final BetterBcryptFunction bcrypt = new BetterBcryptFunction();

    @Override
    public Hash hash(final char[] password, final String salt) {
        return new Password4jHash(bcrypt.hash(new SecureString(password), salt));
    }

    @Override
    public Hash hash(final char[] password) {
        return new Password4jHash(bcrypt.hash(new SecureString(password)));
    }

    @Override
    public String generateSalt() {
        return bcrypt.generateSalt();
    }

    private static final class BetterBcryptFunction extends BcryptFunction {

        public BetterBcryptFunction() {
            super(Bcrypt.B, 12);
        }

        @Override
        public String generateSalt() {
            return super.generateSalt();
        }
    }

    private static final class Password4jHash implements Hash {

        private final com.password4j.Hash hash;

        Password4jHash(final com.password4j.Hash hash) {
            this.hash = hash;
        }

        @Override
        public byte[] getHash() {
            return hash.getBytes();
        }

        @Override
        public String getSalt() {
            return hash.getSalt();
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(hash.getBytes()), hash.getSalt());
        }

        @Override
        public boolean equals(final Object obj) {
            return this == obj
                    || (obj instanceof Hash other
                            && Arrays.equals(getHash(), other.getHash())
                            && getSalt().equals(other.getSalt()));
        }

        @Override
        public String toString() {
            return "Password4jHash[" + Arrays.toString(getHash()) + ", " + getSalt() + ']';
        }
    }
}
