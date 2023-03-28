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
package fr.xpdustry.nucleus.discord.interaction;

import java.util.concurrent.CompletableFuture;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

public record InteractionContext(SlashCommandInteraction interaction) {

    @SuppressWarnings({"UnusedReturnValue", "AnnotateFormatMethod"})
    public CompletableFuture<InteractionOriginalResponseUpdater> sendMessage(
            final String content, final Object... args) {
        return responder().setContent(String.format(content, args)).respond();
    }

    @SuppressWarnings({"UnusedReturnValue", "AnnotateFormatMethod"})
    public CompletableFuture<InteractionOriginalResponseUpdater> sendEphemeralMessage(
            final String content, final Object... args) {
        return responder()
                .setContent(String.format(content, args))
                .setFlags(MessageFlag.EPHEMERAL)
                .respond();
    }

    @SuppressWarnings("UnusedReturnValue")
    public CompletableFuture<InteractionOriginalResponseUpdater> sendEphemeralMessage(final EmbedBuilder embed) {
        return responder().addEmbed(embed).setFlags(MessageFlag.EPHEMERAL).respond();
    }

    public InteractionImmediateResponseBuilder responder() {
        return interaction.createImmediateResponder();
    }
}
