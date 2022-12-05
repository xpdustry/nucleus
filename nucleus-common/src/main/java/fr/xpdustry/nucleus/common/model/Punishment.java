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
package fr.xpdustry.nucleus.common.model;

import fr.xpdustry.nucleus.common.mongo.MongoEntity;
import fr.xpdustry.nucleus.common.mongo.MongoId;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface Punishment extends MongoEntity<MongoId> {

    Optional<User> getAuthor();

    User getTarget();

    Type getType();

    Reason getReason();

    Optional<String> getDetails();

    Instant getDate();

    Duration getDuration();

    enum Type {
        KICK,
        TEMP_BAN,
        BAN
    }

    enum Reason {
        GRIEF,
        TRASH_TALK,
        VPN,
        CUSTOM
    }
}
