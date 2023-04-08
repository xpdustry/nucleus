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
package fr.xpdustry.nucleus.discord.listener;

import fr.xpdustry.nucleus.api.annotation.NucleusAutoService;
import fr.xpdustry.nucleus.api.application.lifecycle.LifecycleListener;
import fr.xpdustry.nucleus.api.message.MessageService;
import fr.xpdustry.nucleus.api.moderation.ModerationActionMessage;
import fr.xpdustry.nucleus.api.moderation.PlayerReportMessage;
import fr.xpdustry.nucleus.discord.service.DiscordService;
import java.awt.Color;
import javax.inject.Inject;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;

// TODO Temporary system until we have a proper database
@NucleusAutoService
public final class MindustryReportListener implements LifecycleListener {

    private final DiscordService discordService;
    private final MessageService messageService;

    @Inject
    public MindustryReportListener(final DiscordService discordService, final MessageService messageService) {
        this.discordService = discordService;
        this.messageService = messageService;
    }

    @Override
    public void onLifecycleInit() {
        this.messageService.subscribe(PlayerReportMessage.class, event -> new MessageBuilder()
                .addEmbed(new EmbedBuilder()
                        .setColor(Color.CYAN)
                        .setTitle("Player report from " + event.getServerIdentifier())
                        .addField("Author", event.getReporterName(), false)
                        .addField("Reported player name", event.getReportedPlayerName(), false)
                        .addField("Reported player ip", "||" + event.getReportedPlayerIp() + "||", false)
                        .addField("Reported player uuid", "||" + event.getReportedPlayerUuid() + "||", false)
                        .addField("Reason", event.getReason(), false))
                .addActionRow(
                        Button.success("temp:report:kick", "Kick"),
                        Button.danger("temp:report:ban", "Ban"),
                        Button.secondary("temp:report:ignore", "Ignore"))
                .send(this.discordService.getReportChannel())
                .thenAccept(message -> message.addButtonClickListener(button -> {
                    final String verb;
                    if (!button.getButtonInteraction().getCustomId().equals("temp:report:ignore")) {
                        final var type =
                                button.getButtonInteraction().getCustomId().equals("temp:report:ban")
                                        ? ModerationActionMessage.Type.BAN
                                        : ModerationActionMessage.Type.KICK;
                        verb = type == ModerationActionMessage.Type.BAN ? "Banned" : "Kicked";

                        this.messageService.publish(ModerationActionMessage.builder()
                                .setAuthor(
                                        button.getButtonInteraction().getUser().getDiscriminatedName())
                                .setTargetUuid(event.getReportedPlayerUuid())
                                .setTargetIp(event.getReportedPlayerIp())
                                .setType(type)
                                .build());
                    } else {
                        verb = "Ignored";
                    }
                    // Remove all buttons until https://github.com/Javacord/Javacord/pull/1195 is merged
                    button.getButtonInteraction()
                            .getMessage()
                            .createUpdater()
                            .setEmbed(message.getEmbeds().get(0).toBuilder()
                                    .setFooter(verb + " by "
                                            + button.getButtonInteraction()
                                                    .getUser()
                                                    .getDiscriminatedName()))
                            .removeAllComponents()
                            .applyChanges();
                })));
    }
}
