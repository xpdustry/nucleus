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

import com.google.auto.service.AutoService;
import fr.xpdustry.nucleus.api.event.PlayerReportEvent;
import fr.xpdustry.nucleus.discord.NucleusBot;
import java.awt.Color;
import org.javacord.api.entity.message.embed.EmbedBuilder;

@AutoService(NucleusBotService.class)
public final class ReportService implements NucleusBotService {

    @Override
    public void onNucleusBotReady(final NucleusBot bot) {
        bot.getMessenger().subscribe(PlayerReportEvent.class, event -> bot.getDiscordApi()
                .getServers()
                .iterator()
                .next()
                .getTextChannelById(bot.getConfiguration().getReportChannel())
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
