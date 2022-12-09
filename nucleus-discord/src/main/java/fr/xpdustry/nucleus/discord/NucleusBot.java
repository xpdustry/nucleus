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

import fr.xpdustry.javelin.JavelinSocket;
import fr.xpdustry.javelin.UserAuthenticator;
import fr.xpdustry.nucleus.api.message.Messenger;
import fr.xpdustry.nucleus.common.message.JavelinMessenger;
import fr.xpdustry.nucleus.discord.commands.AnnotationCommand;
import fr.xpdustry.nucleus.discord.interaction.SlashCommandRegistry;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.listener.GloballyAttachableListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = MongoAutoConfiguration.class) // I will configure it manually
@ConfigurationPropertiesScan("fr.xpdustry.nucleus.discord")
public class NucleusBot {

    private static final Logger logger = LoggerFactory.getLogger(NucleusBot.class);

    public static void main(final String[] args) {
        SpringApplication.run(NucleusBot.class, args);
    }

    @Bean
    public DiscordApi getDiscordApi(final NucleusBotConfiguration config)
            throws InterruptedException, ExecutionException, TimeoutException {
        return new DiscordApiBuilder()
                .setToken(config.getToken())
                .setUserCacheEnabled(true)
                .addIntents(
                        Intent.MESSAGE_CONTENT,
                        Intent.GUILDS,
                        Intent.GUILD_MEMBERS,
                        Intent.GUILD_MESSAGES,
                        Intent.GUILD_MESSAGE_REACTIONS,
                        Intent.DIRECT_MESSAGES)
                .login()
                .get(15L, TimeUnit.SECONDS);
    }

    @Bean
    public UserAuthenticator getUserAuthenticator() {
        return UserAuthenticator.create(Path.of(".", "users.bin.gz"));
    }

    @Bean
    public Messenger getMessenger(final NucleusBotConfiguration config, final UserAuthenticator authenticator) {
        final var socket = JavelinSocket.server(config.getJavelin().getPort(), 4, true, authenticator);
        final var messenger = new JavelinMessenger(socket, 10);
        messenger.start();
        return messenger;
    }

    @Bean
    public CommandLineRunner registerCommands(final List<AnnotationCommand> commands, final DiscordApi api) {
        return args -> {
            final var registry = new SlashCommandRegistry(api);
            commands.forEach(registry::register);
            registry.compile().join();
        };
    }

    @Bean
    public CommandLineRunner registerListeners(final DiscordApi discord, List<GloballyAttachableListener> listeners) {
        return args -> listeners.forEach(discord::addListener);
    }
}
