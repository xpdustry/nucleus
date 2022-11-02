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
import fr.xpdustry.nucleus.common.NucleusApplication;
import fr.xpdustry.nucleus.common.util.NucleusPlatform;
import fr.xpdustry.nucleus.discord.util.JavelinUserAuthenticator;
import java.nio.file.Path;
import java.util.function.Function;
import org.javacord.api.DiscordApi;

public final class NucleusBot implements NucleusApplication {

    private final NucleusBotConfig config;
    private final DiscordApi api;
    private final JavacordCommandManager<JavacordCommandSender> commands;
    private final AnnotationParser<JavacordCommandSender> annotations;
    private final JavelinUserAuthenticator authenticator;
    private final JavelinSocket server;

    public NucleusBot(final NucleusBotConfig config, final DiscordApi api) {
        this.config = config;
        this.api = api;
        this.commands = new JavacordCommandManager<>(
                this.api,
                CommandExecutionCoordinator.simpleCoordinator(),
                Function.identity(),
                Function.identity(),
                sender -> this.config.getCommandPrefix(),
                (sender, permission) ->
                        this.config.getBotOwners().contains(sender.getAuthor().getId()));
        this.annotations = new AnnotationParser<>(this.commands, JavacordCommandSender.class, params -> {
            final var builder = CommandMeta.simple().with(this.commands.createDefaultCommandMeta());
            if (params.has(StandardParameters.DESCRIPTION)) {
                builder.with(CommandMeta.DESCRIPTION, params.get(StandardParameters.DESCRIPTION, ""));
            }
            return builder.build();
        });
        this.authenticator = new JavelinUserAuthenticator(Path.of(".", "users.bin.gz"));
        this.server = JavelinSocket.server(this.config.getJavelinServerPort(), 4, this.authenticator);
    }

    public DiscordApi getDiscordApi() {
        return this.api;
    }

    public JavacordCommandManager<JavacordCommandSender> getCommandManager() {
        return this.commands;
    }

    public AnnotationParser<JavacordCommandSender> getAnnotationParser() {
        return this.annotations;
    }

    public JavelinUserAuthenticator getAuthenticator() {
        return this.authenticator;
    }

    @Override
    public JavelinSocket getSocket() {
        return this.server;
    }

    @Override
    public NucleusPlatform getPlatform() {
        return NucleusPlatform.DISCORD;
    }
}
