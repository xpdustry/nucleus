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
import fr.xpdustry.distributor.api.DistributorProvider;
import fr.xpdustry.distributor.api.plugin.AbstractMindustryPlugin;
import fr.xpdustry.javelin.JavelinPlugin;
import fr.xpdustry.nucleus.core.NucleusApplication;
import fr.xpdustry.nucleus.core.message.JavelinMessenger;
import fr.xpdustry.nucleus.core.message.Messenger;
import fr.xpdustry.nucleus.core.translation.DeeplTranslator;
import fr.xpdustry.nucleus.core.translation.NoopTranslator;
import fr.xpdustry.nucleus.core.translation.Translator;
import fr.xpdustry.nucleus.core.util.NucleusConfigurationUpgrader;
import fr.xpdustry.nucleus.core.util.NucleusPlatform;
import fr.xpdustry.nucleus.core.util.NucleusVersion;
import fr.xpdustry.nucleus.mindustry.action.BlockInspector;
import fr.xpdustry.nucleus.mindustry.chat.ChatManager;
import fr.xpdustry.nucleus.mindustry.chat.ChatManagerImpl;
import fr.xpdustry.nucleus.mindustry.commands.ReportCommand;
import fr.xpdustry.nucleus.mindustry.commands.SaveCommand;
import fr.xpdustry.nucleus.mindustry.commands.StandardPlayerCommands;
import fr.xpdustry.nucleus.mindustry.commands.SwitchCommand;
import fr.xpdustry.nucleus.mindustry.service.AutoUpdateService;
import fr.xpdustry.nucleus.mindustry.service.BanBroadcastService;
import fr.xpdustry.nucleus.mindustry.service.ChatTranslationService;
import fr.xpdustry.nucleus.mindustry.service.ConventionService;
import fr.xpdustry.nucleus.mindustry.service.DiscordBridgeService;
import fr.xpdustry.nucleus.mindustry.service.HubService;
import fr.xpdustry.nucleus.mindustry.service.TipService;
import fr.xpdustry.nucleus.mindustry.util.NucleusPluginCommandManager;
import java.io.IOException;
import java.util.concurrent.Executor;
import org.aeonbits.owner.ConfigFactory;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class NucleusPlugin extends AbstractMindustryPlugin implements NucleusApplication {

    private final NucleusPluginCommandManager serverCommands = new NucleusPluginCommandManager(this);
    private final NucleusPluginCommandManager clientCommands = new NucleusPluginCommandManager(this);
    private final ChatManagerImpl chatManager = new ChatManagerImpl(this);
    private @MonotonicNonNull Translator translator;
    private @MonotonicNonNull Messenger messenger;
    private @MonotonicNonNull NucleusPluginConfiguration configuration;

    @Override
    public void onInit() {
        final var upgrader = new NucleusConfigurationUpgrader();
        try {
            upgrader.upgrade(getDirectory().toFile().toPath().resolve("config.properties"));
        } catch (final IOException e) {
            throw new RuntimeException("Failed to upgrade the config file.", e);
        }

        ConfigFactory.setProperty("plugin-directory", getDirectory().toFile().getPath());
        this.configuration = ConfigFactory.create(NucleusPluginConfiguration.class);

        final Executor asyncExecutor = runnable -> DistributorProvider.get()
                .getPluginScheduler()
                .scheduleAsync(this)
                .execute(runnable);

        this.translator = !configuration.getTranslationToken().isEmpty()
                ? new DeeplTranslator(configuration.getTranslationToken(), asyncExecutor)
                : new NoopTranslator();

        this.addListener(new ConventionService(this));
        this.addListener(new DiscordBridgeService(this));
        this.addListener(this.chatManager);
        this.addListener(new ChatTranslationService(this, this.translator));
        this.addListener(new BlockInspector(this));
        this.addListener(new TipService(this));
        this.addListener(new BanBroadcastService(this));
        this.addListener(new AutoUpdateService(this));
        if (this.getConfiguration().isHubEnabled()) {
            this.addListener(new HubService(this));
        }

        this.addListener(new StandardPlayerCommands(this));
        this.addListener(new SaveCommand(this));
        this.addListener(new ReportCommand(this));
        this.addListener(new SwitchCommand(this));
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

    public ChatManager getChatManager() {
        return chatManager;
    }

    public Messenger getMessenger() {
        return messenger;
    }

    public Translator getTranslator() {
        return translator;
    }

    @Override
    public NucleusVersion getVersion() {
        return NucleusVersion.parse(getDescriptor().getVersion());
    }

    @Override
    public NucleusPlatform getPlatform() {
        return NucleusPlatform.MINDUSTRY;
    }

    @Override
    public NucleusPluginConfiguration getConfiguration() {
        return configuration;
    }
}
