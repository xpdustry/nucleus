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
import fr.xpdustry.nucleus.common.NucleusApplication;
import fr.xpdustry.nucleus.common.util.NucleusPlatform;
import java.util.function.Function;
import org.javacord.api.DiscordApi;

public final class NucleusBot implements NucleusApplication {

    private final NucleusBotConfig config;
    private final DiscordApi api;
    private final JavacordCommandManager<JavacordCommandSender> commands;
    private final AnnotationParser<JavacordCommandSender> annotations;

    public NucleusBot(final NucleusBotConfig config, final DiscordApi api) {
        this.config = config;
        this.api = api;
        this.commands = new JavacordCommandManager<>(
                this.api,
                CommandExecutionCoordinator.simpleCoordinator(),
                Function.identity(),
                Function.identity(),
                sender -> this.config.getCommandPrefix(),
                (sender, permission) -> false);
        this.annotations = new AnnotationParser<>(this.commands, JavacordCommandSender.class, params -> {
            final var builder = CommandMeta.simple().with(this.commands.createDefaultCommandMeta());
            if (params.has(StandardParameters.DESCRIPTION)) {
                builder.with(CommandMeta.DESCRIPTION, params.get(StandardParameters.DESCRIPTION, ""));
            }
            return builder.build();
        });
    }

    public DiscordApi getDiscordApi() {
        return api;
    }

    public JavacordCommandManager<JavacordCommandSender> getCommandManager() {
        return commands;
    }

    public AnnotationParser<JavacordCommandSender> getAnnotationParser() {
        return annotations;
    }

    @Override
    public NucleusPlatform getPlatform() {
        return NucleusPlatform.DISCORD;
    }
}
