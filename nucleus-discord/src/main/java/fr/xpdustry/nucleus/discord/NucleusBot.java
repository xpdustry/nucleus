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

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.arguments.parser.StandardParameters;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.javacord.JavacordCommandManager;
import cloud.commandframework.javacord.sender.JavacordCommandSender;
import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.javelin.JavelinSocket;
import fr.xpdustry.javelin.UserAuthenticator;
import fr.xpdustry.nucleus.discord.commands.AnnotationCommand;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.listener.GloballyAttachableListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
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
    public JavacordCommandManager<JavacordCommandSender> getCommandManager(
            final DiscordApi api, final NucleusBotConfiguration config) {
        return new JavacordCommandManager<>(
                api,
                CommandExecutionCoordinator.simpleCoordinator(),
                Function.identity(),
                Function.identity(),
                sender -> config.getPrefix(),
                (sender, permission) -> api.getOwnerId()
                        .map(id -> id == sender.getAuthor().getId())
                        .orElse(false));
    }

    @Bean
    public AnnotationParser<JavacordCommandSender> getCommandAnnotationParser(
            final JavacordCommandManager<JavacordCommandSender> manager) {
        return new AnnotationParser<>(manager, JavacordCommandSender.class, params -> {
            final var builder = CommandMeta.simple().with(manager.createDefaultCommandMeta());
            if (params.has(StandardParameters.DESCRIPTION)) {
                builder.with(CommandMeta.DESCRIPTION, params.get(StandardParameters.DESCRIPTION, ""));
            }
            return builder.build();
        });
    }

    @Bean
    public UserAuthenticator getUserAuthenticator() {
        return UserAuthenticator.create(Path.of(".", "users.bin.gz"));
    }

    @Bean
    public JavelinSocket getJavelinSocket(final NucleusBotConfiguration config, final UserAuthenticator authenticator) {
        final var socket = JavelinSocket.server(config.getJavelin().getPort(), 4, true, authenticator);
        socket.start().orTimeout(15L, TimeUnit.SECONDS).join();
        return socket;
    }

    @Bean
    public CommandLineRunner registerCommands(
            final AnnotationParser<JavacordCommandSender> parser, final List<AnnotationCommand> commands) {
        return args -> commands.forEach(parser::parse);
    }

    @Bean
    public CommandLineRunner registerListeners(final DiscordApi discord, List<GloballyAttachableListener> listeners) {
        return args -> listeners.forEach(discord::addListener);
    }
}
