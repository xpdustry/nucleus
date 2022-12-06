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
package fr.xpdustry.nucleus.common.database;

import fr.xpdustry.nucleus.api.database.MongoBinary;
import fr.xpdustry.nucleus.api.database.MongoId;
import org.bson.BsonBinary;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class MongoWrapperCodecProvider implements CodecProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T> @Nullable Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        if (MongoId.class.isAssignableFrom(clazz)) {
            return (Codec<T>) MongoIdCodec.INSTANCE;
        } else if (MongoBinary.class.isAssignableFrom(clazz)) {
            return (Codec<T>) MongoBinaryCodec.INSTANCE;
        }
        return null;
    }

    public static final class MongoBinaryCodec implements Codec<MongoBinary> {

        static final MongoBinaryCodec INSTANCE = new MongoBinaryCodec();

        @Override
        public MongoBinary decode(BsonReader reader, DecoderContext ctx) {
            return new BsonMongoBinary(reader.readBinaryData().getData());
        }

        @Override
        public void encode(BsonWriter writer, MongoBinary value, EncoderContext ctx) {
            writer.writeBinaryData(new BsonBinary(value.getBytes()));
        }

        @Override
        public Class<MongoBinary> getEncoderClass() {
            return MongoBinary.class;
        }
    }

    public static final class MongoIdCodec implements Codec<MongoId> {

        static final MongoIdCodec INSTANCE = new MongoIdCodec();

        @Override
        public MongoId decode(BsonReader reader, DecoderContext ctx) {
            return new BsonMongoId(reader.readObjectId());
        }

        @Override
        public void encode(BsonWriter writer, MongoId value, EncoderContext ctx) {
            writer.writeObjectId(value instanceof BsonMongoId doc ? doc.objectId : new ObjectId(value.toHexString()));
        }

        @Override
        public Class<MongoId> getEncoderClass() {
            return MongoId.class;
        }
    }
}
