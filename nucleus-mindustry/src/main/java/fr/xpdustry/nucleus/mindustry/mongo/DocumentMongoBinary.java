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
package fr.xpdustry.nucleus.mindustry.mongo;

import fr.xpdustry.nucleus.common.mongo.MongoBinary;
import org.bson.types.Binary;

public final class DocumentMongoBinary implements MongoBinary {

    final Binary binary;

    public DocumentMongoBinary(final Binary binary) {
        this.binary = binary;
    }

    public DocumentMongoBinary(final byte[] bytes) {
        this.binary = new Binary(bytes);
    }

    @Override
    public byte[] getBytes() {
        return binary.getData();
    }

    @Override
    public int getLength() {
        return binary.length();
    }

    @Override
    public String toString() {
        return binary.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != getClass()) return false;
        return binary.equals(((DocumentMongoBinary) obj).binary);
    }

    @Override
    public int hashCode() {
        return binary.hashCode();
    }
}
