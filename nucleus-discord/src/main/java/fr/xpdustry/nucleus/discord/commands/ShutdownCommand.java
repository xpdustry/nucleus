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
package fr.xpdustry.nucleus.discord.commands;

import fr.xpdustry.nucleus.discord.NucleusBot;
import fr.xpdustry.nucleus.discord.interaction.InteractionContext;
import fr.xpdustry.nucleus.discord.interaction.InteractionDescription;
import fr.xpdustry.nucleus.discord.interaction.InteractionPermission;
import fr.xpdustry.nucleus.discord.interaction.SlashInteraction;
import org.javacord.api.entity.permission.PermissionType;

@SlashInteraction("shutdown")
@InteractionDescription("Shutdown the bot.")
@InteractionPermission(PermissionType.ADMINISTRATOR)
public final class ShutdownCommand {

    private final NucleusBot bot;

    public ShutdownCommand(final NucleusBot bot) {
        this.bot = bot;
    }

    @SlashInteraction.Handler
    public void onShutdown(final InteractionContext context) {
        context.sendMessage("The bot has been scheduled for shutdown.").thenRunAsync(bot::shutdown);
    }
}
