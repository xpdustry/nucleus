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
package fr.xpdustry.nucleus.discord.services;

import fr.xpdustry.javelin.JavelinSocket;
import fr.xpdustry.nucleus.common.event.PlayerReportEvent;
import fr.xpdustry.nucleus.discord.NucleusBotConfiguration;
import java.awt.*;
import javax.annotation.PostConstruct;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.stereotype.Service;

@Service
public final class ReportService {

    private final JavelinSocket socket;
    private final DiscordApi api;
    private final NucleusBotConfiguration configuration;

    public ReportService(
            final JavelinSocket socket, final DiscordApi api, final NucleusBotConfiguration configuration) {
        this.socket = socket;
        this.api = api;
        this.configuration = configuration;
    }

    @PostConstruct
    public void init() {
        this.socket.subscribe(PlayerReportEvent.class, event -> this.api
                .getServers()
                .iterator()
                .next()
                .getTextChannelById(this.configuration.getChannels().getReports())
                .orElseThrow()
                .sendMessage(new EmbedBuilder()
                        .setColor(Color.CYAN)
                        .setTitle("Player report from **" + event.getServerName() + "**")
                        .addField("Author", event.getPlayerName(), false)
                        .addField("Reported player name", event.getReportedPlayerName(), false)
                        .addField("Reported player ip", event.getReportedPlayerIp(), false)
                        .addField("Reported player uuid", event.getReportedPlayerUuid(), false)
                        .addField("Reason", event.getReason(), false)));
    }
}
