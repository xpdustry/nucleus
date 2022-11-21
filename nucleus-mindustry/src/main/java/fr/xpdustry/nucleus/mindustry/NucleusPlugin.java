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
package fr.xpdustry.nucleus.mindustry;

import arc.util.CommandHandler;
import fr.xpdustry.distributor.api.command.ArcCommandManager;
import fr.xpdustry.distributor.api.command.sender.CommandSender;
import fr.xpdustry.distributor.api.plugin.ExtendedPlugin;
import fr.xpdustry.nucleus.mindustry.chat.NucleusChatFilter;
import fr.xpdustry.nucleus.mindustry.chat.NucleusChatProcessor;
import fr.xpdustry.nucleus.mindustry.internal.NucleusPluginCommandManager;
import fr.xpdustry.nucleus.mindustry.listeners.ChatTranslator;
import fr.xpdustry.nucleus.mindustry.listeners.DiscordBridge;
import fr.xpdustry.nucleus.mindustry.listeners.PlayerCommands;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import org.aeonbits.owner.ConfigFactory;
import org.checkerframework.checker.nullness.qual.*;

public final class NucleusPlugin extends ExtendedPlugin {

    private final NucleusPluginCommandManager clientCommands = new NucleusPluginCommandManager(this);
    private final List<NucleusChatFilter> filters = new ArrayList<>();
    private final List<NucleusChatProcessor> processors = new ArrayList<>();
    private @MonotonicNonNull NucleusPluginConfiguration configuration;

    @Override
    public void onInit() {
        this.configuration = ConfigFactory.create(NucleusPluginConfiguration.class);
        this.addListener(new PlayerCommands(this));
        this.addListener(new DiscordBridge(this));
        this.addListener(new ChatTranslator(this));
        System.out.println(configuration.getTranslationToken());
    }

    @Override
    public void onLoad() {
        // TODO Improve the Chat API
        Vars.netServer.admins.addChatFilter((player, text) -> {
            if (this.filters.stream().allMatch(f -> f.filter(player, text))) {
                Groups.player.each(receiver -> {
                    var result = text;
                    for (final var processor : this.processors) {
                        result = processor.process(player, result, receiver);
                    }
                    Call.sendMessage(
                            receiver.con(), Vars.netServer.chatFormatter.format(player, result), result, player);
                });
            }
            return null;
        });
    }

    @Override
    public void onClientCommandsRegistration(final CommandHandler handler) {
        this.clientCommands.initialize(handler);
    }

    public ArcCommandManager<CommandSender> getClientCommands() {
        return clientCommands;
    }

    public NucleusPluginConfiguration getConfiguration() {
        return configuration;
    }

    public void addFilter(final NucleusChatFilter filter) {
        filters.add(filter);
    }

    public void addProcessor(final NucleusChatProcessor processor) {
        processors.add(processor);
    }

    public List<NucleusChatFilter> getFilters() {
        return Collections.unmodifiableList(filters);
    }

    public List<NucleusChatProcessor> getProcessors() {
        return Collections.unmodifiableList(processors);
    }
}
