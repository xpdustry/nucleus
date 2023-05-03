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

import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.discord.configuration.NucleusDiscordConfiguration;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.server.Server;
import org.slf4j.Logger;

public final class SimpleDiscordService implements DiscordService, NucleusListener {

    private final NucleusDiscordConfiguration configuration;
    private @MonotonicNonNull DiscordApi discordApi;

    @Inject
    private Logger logger;

    @Inject
    public SimpleDiscordService(final NucleusDiscordConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void onNucleusInit() {
        this.logger.info("Connecting to the Discord API...");
        this.discordApi = new DiscordApiBuilder()
                .setToken(configuration.getToken())
                .setUserCacheEnabled(true)
                .addIntents(
                        Intent.MESSAGE_CONTENT,
                        Intent.GUILDS,
                        Intent.GUILD_MEMBERS,
                        Intent.GUILD_MESSAGES,
                        Intent.GUILD_MESSAGE_REACTIONS,
                        Intent.DIRECT_MESSAGES)
                .login()
                .orTimeout(15L, TimeUnit.SECONDS)
                .join();
    }

    @Override
    public void onNucleusExit() {
        this.discordApi.disconnect().orTimeout(15L, TimeUnit.SECONDS).join();
    }

    @Override
    public DiscordApi getDiscordApi() {
        return this.discordApi;
    }

    @Override
    public Server getMainServer() {
        return this.discordApi.getServers().iterator().next();
    }

    @Override
    public TextChannel getSystemChannel() {
        return getMainServer()
                .getTextChannelById(this.configuration.getSystemChannel())
                .orElseThrow();
    }

    @Override
    public TextChannel getReportChannel() {
        return getMainServer()
                .getTextChannelById(this.configuration.getReportChannel())
                .orElseThrow();
    }
}
