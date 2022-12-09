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

import fr.xpdustry.nucleus.discord.interaction.InteractionContext;
import fr.xpdustry.nucleus.discord.interaction.InteractionDescription;
import fr.xpdustry.nucleus.discord.interaction.Option;
import fr.xpdustry.nucleus.discord.interaction.SlashInteraction;
import org.javacord.api.entity.message.mention.AllowedMentionsBuilder;
import org.springframework.stereotype.Component;

@SlashInteraction("echo")
@InteractionDescription("Echoes a message.")
@Component
public final class EchoCommand implements AnnotationCommand {

    @SlashInteraction.Handler
    public void onEchoCommand(final InteractionContext context, final @Option("message") String message) {
        context.interaction()
                .createImmediateResponder()
                .setAllowedMentions(new AllowedMentionsBuilder()
                        .setMentionEveryoneAndHere(false)
                        .setMentionRoles(false)
                        .setMentionUsers(false)
                        .build())
                .setContent(message)
                .respond();
    }
}
