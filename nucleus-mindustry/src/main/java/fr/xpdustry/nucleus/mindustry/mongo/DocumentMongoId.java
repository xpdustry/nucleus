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

import fr.xpdustry.nucleus.common.mongo.MongoId;
import java.util.Base64;
import org.bson.types.ObjectId;

public final class DocumentMongoId implements MongoId {

    final ObjectId objectId;

    public DocumentMongoId(final ObjectId objectId) {
        this.objectId = objectId;
    }

    public DocumentMongoId() {
        this.objectId = new ObjectId();
    }

    @Override
    public String toHexString() {
        return objectId.toHexString();
    }

    @Override
    public String toB64String() {
        return Base64.getEncoder().encodeToString(objectId.toByteArray());
    }

    @Override
    public byte[] toByteArray() {
        return objectId.toByteArray();
    }
}
