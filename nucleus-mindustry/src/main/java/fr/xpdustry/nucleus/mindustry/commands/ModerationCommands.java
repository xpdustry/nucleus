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
package fr.xpdustry.nucleus.mindustry.commands;

import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.distributor.api.command.argument.PlayerArgument;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.common.database.model.Punishment.Kind;
import fr.xpdustry.nucleus.common.inject.EnableScanning;
import fr.xpdustry.nucleus.mindustry.annotation.ClientSide;
import fr.xpdustry.nucleus.mindustry.command.NucleusPluginCommandManager;
import fr.xpdustry.nucleus.mindustry.moderation.ModerationService;
import javax.inject.Inject;

@EnableScanning
public final class ModerationCommands implements NucleusListener {

    private final ModerationService moderationService;
    private final NucleusPluginCommandManager clientCommandManager;

    @Inject
    public ModerationCommands(
            final ModerationService moderationService,
            final @ClientSide NucleusPluginCommandManager clientCommandManager) {
        this.moderationService = moderationService;
        this.clientCommandManager = clientCommandManager;
    }

    @Override
    public void onNucleusInit() {
        this.clientCommandManager.command(this.clientCommandManager
                .commandBuilder("mute")
                .permission("nucleus.moderation.mute")
                .meta(CommandMeta.DESCRIPTION, "Mute a player.")
                .argument(PlayerArgument.of("player"))
                .argument(StringArgument.of("reason"))
                .handler(ctx -> this.moderationService
                        .punish(ctx.getSender().getPlayer(), ctx.get("player"), Kind.MUTE, ctx.get("reason"))
                        .thenRun(() -> ctx.getSender().sendMessage("Player muted."))
                        .exceptionally(throwable -> {
                            ctx.getSender().sendMessage("An error occurred while muting the player.");
                            return null;
                        })));
    }
}
