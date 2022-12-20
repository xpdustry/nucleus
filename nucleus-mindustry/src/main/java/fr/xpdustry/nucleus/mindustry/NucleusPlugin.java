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
import fr.xpdustry.distributor.api.plugin.ExtendedPlugin;
import fr.xpdustry.distributor.api.scheduler.PluginScheduler;
import fr.xpdustry.javelin.JavelinPlugin;
import fr.xpdustry.nucleus.core.message.JavelinMessenger;
import fr.xpdustry.nucleus.core.message.Messenger;
import fr.xpdustry.nucleus.core.translation.DeeplTranslator;
import fr.xpdustry.nucleus.core.translation.Translator;
import fr.xpdustry.nucleus.mindustry.action.BlockInspector;
import fr.xpdustry.nucleus.mindustry.chat.ChatManager;
import fr.xpdustry.nucleus.mindustry.chat.ChatManagerImpl;
import fr.xpdustry.nucleus.mindustry.commands.PlayerCommands;
import fr.xpdustry.nucleus.mindustry.commands.SaveCommands;
import fr.xpdustry.nucleus.mindustry.service.ChatTranslationService;
import fr.xpdustry.nucleus.mindustry.service.ConventionService;
import fr.xpdustry.nucleus.mindustry.service.DiscordBridgeService;
import fr.xpdustry.nucleus.mindustry.util.NucleusPluginCommandManager;
import org.aeonbits.owner.ConfigFactory;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class NucleusPlugin extends ExtendedPlugin {

    private final NucleusPluginCommandManager serverCommands = new NucleusPluginCommandManager(this);
    private final NucleusPluginCommandManager clientCommands = new NucleusPluginCommandManager(this);
    private final ChatManagerImpl chatManager = new ChatManagerImpl(this);
    private final PluginScheduler scheduler = PluginScheduler.create(this, 8);
    private @MonotonicNonNull Translator translator;
    private @MonotonicNonNull Messenger messenger;
    private @MonotonicNonNull NucleusPluginConfiguration configuration;

    @Override
    public void onInit() {
        ConfigFactory.setProperty("plugin-directory", getDirectory().toFile().getPath());
        this.configuration = ConfigFactory.create(NucleusPluginConfiguration.class);

        this.addListener(new ConventionService(this));
        this.addListener(new PlayerCommands(this));
        this.addListener(new DiscordBridgeService(this));
        this.addListener(this.chatManager);
        this.addListener(this.scheduler);
        this.addListener(new ChatTranslationService(
                this,
                this.translator =
                        new DeeplTranslator(configuration.getTranslationToken(), scheduler.getAsyncExecutor())));
        this.addListener(new BlockInspector(this));
        this.addListener(new SaveCommands(this));
    }

    @Override
    public void onServerCommandsRegistration(final CommandHandler handler) {
        this.serverCommands.initialize(handler);
    }

    @Override
    public void onClientCommandsRegistration(final CommandHandler handler) {
        this.clientCommands.initialize(handler);
    }

    @Override
    public void onLoad() {
        this.messenger = new JavelinMessenger(JavelinPlugin.getJavelinSocket(), 10);
    }

    public NucleusPluginCommandManager getServerCommands() {
        return serverCommands;
    }

    public NucleusPluginCommandManager getClientCommands() {
        return clientCommands;
    }

    public NucleusPluginConfiguration getConfiguration() {
        return configuration;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public PluginScheduler getScheduler() {
        return scheduler;
    }

    public Messenger getMessenger() {
        return messenger;
    }

    public Translator getTranslator() {
        return translator;
    }
}
