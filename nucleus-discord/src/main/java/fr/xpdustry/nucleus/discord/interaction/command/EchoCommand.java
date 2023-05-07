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
package fr.xpdustry.nucleus.discord.interaction.command;

import fr.xpdustry.nucleus.common.inject.EnableScanning;
import fr.xpdustry.nucleus.discord.interaction.InteractionContext;
import fr.xpdustry.nucleus.discord.interaction.InteractionDescription;
import fr.xpdustry.nucleus.discord.interaction.InteractionListener;
import fr.xpdustry.nucleus.discord.interaction.Option;
import fr.xpdustry.nucleus.discord.interaction.SlashInteraction;
import fr.xpdustry.nucleus.discord.util.Messages;

@EnableScanning
@SlashInteraction("echo")
@InteractionDescription("Echoes a message.")
public final class EchoCommand implements InteractionListener {

    @SlashInteraction.Handler
    public void onEcho(final InteractionContext context, final @Option("message") String message) {
        context.responder()
                .setContent(message)
                .setAllowedMentions(Messages.noMentions())
                .respond();
    }
}
