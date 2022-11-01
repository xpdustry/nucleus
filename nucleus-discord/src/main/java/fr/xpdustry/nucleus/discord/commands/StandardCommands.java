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

import cloud.commandframework.CommandHelpHandler.IndexHelpTopic;
import cloud.commandframework.CommandHelpHandler.MultiHelpTopic;
import cloud.commandframework.CommandHelpHandler.VerboseHelpTopic;
import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.javacord.sender.JavacordCommandSender;
import fr.xpdustry.nucleus.discord.NucleusBot;
import java.awt.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.message.mention.AllowedMentionsBuilder;

public final class StandardCommands {

    private final NucleusBot bot;

    public StandardCommands(final NucleusBot bot) {
        this.bot = bot;
    }

    @CommandDescription("Echoes the input text.")
    @CommandMethod("echo <text>")
    public void onEchoCommand(final JavacordCommandSender sender, final @Greedy @Argument("text") String text) {
        new MessageBuilder()
                .setAllowedMentions(new AllowedMentionsBuilder()
                        .setMentionEveryoneAndHere(false)
                        .setMentionRoles(false)
                        .setMentionUsers(false)
                        .build())
                .setContent(text)
                .send(sender.getTextChannel());
    }

    @CommandDescription("Bing chilling!")
    @CommandMethod("ping")
    public void onPingCommand(final JavacordCommandSender sender) {
        this.bot.getDiscordApi().measureRestLatency().thenCompose(latency -> new MessageBuilder()
                .append("pong with **")
                .append(latency.toMillis())
                .append("** milliseconds of latency!")
                .replyTo(sender.getMessage())
                .send(sender.getTextChannel()));
    }

    @CommandDescription("We need some milk!")
    @CommandMethod("help [query]")
    public void onHelpCommand(
            final JavacordCommandSender sender, final @Nullable @Greedy @Argument("query") String query) {
        final var embed = new EmbedBuilder().setColor(Color.CYAN);

        if (query == null) {
            embed.setTitle("Behold, the commands of **Nucleus**! The Xpdustry Discord bot.")
                    .setThumbnail(this.bot.getDiscordApi().getYourself().getAvatar());

            for (final var command :
                    this.bot.getCommandManager().createCommandHelpHandler().getAllCommands()) {
                embed.addField(command.getSyntaxString(), command.getDescription());
            }
        } else {
            final var topic =
                    this.bot.getCommandManager().createCommandHelpHandler().queryHelp(sender, query);

            if (topic instanceof IndexHelpTopic<JavacordCommandSender> index) {
                if (index.isEmpty()) {
                    embed.setDescription("Command not found.");
                } else {
                    embed.setDescription("Found commands for **%s**.".formatted(query));
                    for (final var entry : index.getEntries()) {
                        embed.addField(entry.getSyntaxString(), entry.getDescription());
                    }
                }
            } else if (topic instanceof VerboseHelpTopic<JavacordCommandSender> verbose) {
                final var components = verbose.getCommand().getComponents();

                embed.setTitle(components.get(0).getArgument().getName());
                embed.setDescription(verbose.getDescription());

                // Skips the first component since it's the name of the command
                for (int i = 1; i < components.size(); i++) {
                    final var component = components.get(0);
                    embed.addField(
                            component.getArgument().getName(),
                            component.getArgumentDescription().getDescription());
                }
            } else {
                // If it's neither of the above, we can safely cast to the third topic type
                final var multi = (MultiHelpTopic<JavacordCommandSender>) topic;
                embed.setTitle(multi.getLongestPath());

                final var builder = new StringBuilder();
                final var suggestions = multi.getChildSuggestions().iterator();
                while (suggestions.hasNext()) {
                    builder.append(" - ").append(suggestions.next());
                    if (suggestions.hasNext()) {
                        builder.append('\n');
                    }
                }

                embed.setDescription(builder.toString());
            }
        }

        sender.getMessage().reply(embed);
    }
}
