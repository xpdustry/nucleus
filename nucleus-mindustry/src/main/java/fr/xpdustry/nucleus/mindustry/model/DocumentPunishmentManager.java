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
package fr.xpdustry.nucleus.mindustry.model;

import com.mongodb.client.MongoCollection;
import fr.xpdustry.nucleus.common.model.Punishment;
import fr.xpdustry.nucleus.common.model.PunishmentManager;
import fr.xpdustry.nucleus.common.model.User;
import fr.xpdustry.nucleus.common.model.UserManager;
import fr.xpdustry.nucleus.common.mongo.MongoId;
import fr.xpdustry.nucleus.mindustry.mongo.DocumentMongoId;
import fr.xpdustry.nucleus.mindustry.mongo.DocumentMongoManager;
import fr.xpdustry.nucleus.mindustry.mongo.MongoEntityCodec;
import java.time.Duration;
import org.bson.Document;

public final class DocumentPunishmentManager extends DocumentMongoManager<Punishment, MongoId>
        implements PunishmentManager {

    public DocumentPunishmentManager(final MongoCollection<Document> collection, final UserManager userManager) {
        super(collection, new Codec(userManager));
    }

    static final class Codec implements MongoEntityCodec<Punishment> {

        private final UserManager userManager;

        Codec(UserManager userManager) {
            this.userManager = userManager;
        }

        @Override
        public Document encode(final Punishment entity) {
            return new Document()
                    .append("author", entity.getAuthor().map(User::getMongoId).orElse(null))
                    .append("target", entity.getTarget().getMongoId())
                    .append("type", entity.getType())
                    .append("reason", entity.getReason())
                    .append("details", entity.getDetails().orElse(null))
                    .append("date", entity.getDate())
                    .append("duration", entity.getDuration());
        }

        @Override
        public Punishment decode(final Document entity) {
            return new DocumentPunishment(
                    new DocumentMongoId(entity.getObjectId("_id")),
                    // TODO Lazy load author and target
                    entity.containsKey("author")
                            ? userManager.findById(entity.getString("author")).orElseThrow()
                            : null,
                    userManager.findById(entity.getString("author")).orElseThrow(),
                    Punishment.Type.valueOf(entity.getString("type")),
                    Punishment.Reason.valueOf(entity.getString("reason")),
                    entity.getString("details"),
                    entity.getDate("date").toInstant(),
                    Duration.ofMillis(entity.getLong("duration")));
        }
    }
}
