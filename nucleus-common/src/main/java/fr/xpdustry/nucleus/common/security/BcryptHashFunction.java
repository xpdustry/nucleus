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
package fr.xpdustry.nucleus.common.security;

import com.password4j.BcryptFunction;
import com.password4j.SecureString;
import com.password4j.types.Bcrypt;
import fr.xpdustry.nucleus.api.hash.Hash;
import fr.xpdustry.nucleus.api.hash.HashFunction;
import java.nio.charset.StandardCharsets;

public final class BcryptHashFunction implements HashFunction {

    private final BcryptFunction bcrypt = BcryptFunction.getInstance(Bcrypt.B, 12);

    @Override
    public Hash hash(final char[] password, final byte[] salt) {
        final var hash = this.bcrypt.hash(new SecureString(password), new String(salt, StandardCharsets.UTF_8));
        return Hash.of(hash.getBytes(), salt);
    }

    @Override
    public Hash hash(final char[] password) {
        final var hash = this.bcrypt.hash(new SecureString(password));
        return Hash.of(hash.getBytes(), hash.getSalt().getBytes(StandardCharsets.UTF_8));
    }
}
