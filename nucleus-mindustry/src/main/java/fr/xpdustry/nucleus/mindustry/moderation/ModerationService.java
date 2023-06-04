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
package fr.xpdustry.nucleus.mindustry.moderation;

import fr.xpdustry.nucleus.common.database.model.Punishment;
import fr.xpdustry.nucleus.common.database.model.Punishment.Kind;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import mindustry.gen.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface ModerationService {

    CompletableFuture<Punishment> punish(
            final @Nullable Player sender, final Player target, final Kind kind, final String reason);

    CompletableFuture<Punishment> punish(
            final @Nullable Player sender,
            final Player target,
            final Kind kind,
            final String reason,
            final Duration duration);
}
