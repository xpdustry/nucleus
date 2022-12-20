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
package fr.xpdustry.nucleus.discord;

import fr.xpdustry.javelin.UserAuthenticator;
import fr.xpdustry.nucleus.core.message.Messenger;
import org.javacord.api.DiscordApi;
import org.slf4j.Logger;

public final class NucleusBot {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(NucleusBot.class);

    private final NucleusBotConfiguration configuration;
    private final DiscordApi discordApi;
    private final Messenger messenger;
    private final UserAuthenticator authenticator;

    public NucleusBot(
            final NucleusBotConfiguration configuration,
            final DiscordApi discordApi,
            final Messenger messenger,
            final UserAuthenticator authenticator) {
        this.configuration = configuration;
        this.discordApi = discordApi;
        this.messenger = messenger;
        this.authenticator = authenticator;
    }

    public NucleusBotConfiguration getConfiguration() {
        return configuration;
    }

    public DiscordApi getDiscordApi() {
        return discordApi;
    }

    public Messenger getMessenger() {
        return messenger;
    }

    public UserAuthenticator getAuthenticator() {
        return authenticator;
    }

    public void shutdown() {
        logger.info("Shutting down the bot...");
        discordApi.disconnect();
        messenger.close();
        logger.info("Bot has been shut down.");
    }
}
