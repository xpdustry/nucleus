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
package fr.xpdustry.nucleus.discord.service;

import fr.xpdustry.nucleus.core.event.BanBroadcastEvent;
import fr.xpdustry.nucleus.core.event.ImmutableBanBroadcastEvent;
import fr.xpdustry.nucleus.core.event.PlayerReportEvent;
import fr.xpdustry.nucleus.discord.NucleusBot;
import fr.xpdustry.nucleus.discord.NucleusBotUtil;
import java.awt.Color;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;

// TODO Temporary system until we have a proper database
public final class ReportService implements NucleusDiscordService {

    private final NucleusBot bot;

    public ReportService(final NucleusBot bot) {
        this.bot = bot;
    }

    @Override
    public void onNucleusDiscordInit() {
        bot.getMessenger().subscribe(PlayerReportEvent.class, event -> new MessageBuilder()
                .addEmbed(new EmbedBuilder()
                        .setColor(Color.CYAN)
                        .setTitle("Player report from " + event.getServerName())
                        .addField("Author", event.getPlayerName(), false)
                        .addField("Reported player name", event.getReportedPlayerName(), false)
                        .addField("Reported player ip", event.getReportedPlayerIp(), false)
                        .addField("Reported player uuid", event.getReportedPlayerUuid(), false)
                        .addField("Reason", event.getReason(), false))
                .addActionRow(
                        Button.success("temp:report:kick", "Kick"),
                        Button.danger("temp:report:ban", "Ban"),
                        Button.secondary("temp:report:ignore", "Ignore"))
                .send(bot.getReportChannel())
                .thenAccept(message -> message.addButtonClickListener(button -> {
                    final String verb;
                    if (!button.getButtonInteraction().getCustomId().equals("temp:report:ignore")) {
                        final var type =
                                button.getButtonInteraction().getCustomId().equals("temp:report:ban")
                                        ? BanBroadcastEvent.Type.BAN
                                        : BanBroadcastEvent.Type.KICK;
                        verb = type == BanBroadcastEvent.Type.BAN ? "Banned" : "Kicked";

                        bot.getMessenger()
                                .send(ImmutableBanBroadcastEvent.builder()
                                        .author(button.getButtonInteraction()
                                                .getUser()
                                                .getDiscriminatedName())
                                        .targetUuid(event.getReportedPlayerUuid())
                                        .targetIp(event.getReportedPlayerIp())
                                        .type(type)
                                        .build());
                    } else {
                        verb = "Ignored";
                    }
                    button.getButtonInteraction()
                            .createImmediateResponder()
                            .setContent(verb + " by "
                                    + button.getButtonInteraction().getUser().getMentionTag())
                            .setAllowedMentions(NucleusBotUtil.noMentions())
                            .respond()
                            // Remove all buttons until https://github.com/Javacord/Javacord/pull/1195 is merged
                            .thenCompose(response -> button.getButtonInteraction()
                                    .getMessage()
                                    .createUpdater()
                                    .removeAllComponents()
                                    .applyChanges());
                })));
    }
}
