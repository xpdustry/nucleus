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
import fr.xpdustry.nucleus.mindustry.action.BlockInspector;
import fr.xpdustry.nucleus.mindustry.chat.*;
import fr.xpdustry.nucleus.mindustry.commands.PlayerCommands;
import fr.xpdustry.nucleus.mindustry.commands.SaveCommands;
import fr.xpdustry.nucleus.mindustry.commands.SharedCommands;
import fr.xpdustry.nucleus.mindustry.internal.NucleusPluginCommandManager;
import fr.xpdustry.nucleus.mindustry.translator.ChatTranslator;
import fr.xpdustry.nucleus.mindustry.translator.LibreTranslateTranslator;
import fr.xpdustry.nucleus.mindustry.translator.Translator;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import mindustry.net.Administration;
import org.aeonbits.owner.ConfigFactory;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class NucleusPlugin extends ExtendedPlugin {

    private final NucleusPluginCommandManager serverCommands = new NucleusPluginCommandManager(this);
    private final NucleusPluginCommandManager clientCommands = new NucleusPluginCommandManager(this);
    private final ChatManagerImpl chatManager = new ChatManagerImpl(this);
    private final PluginScheduler scheduler = PluginScheduler.create(this, 8);
    private final Translator translator = new LibreTranslateTranslator(this);
    private @MonotonicNonNull NucleusPluginConfiguration configuration;

    @Override
    public void onInit() {
        ConfigFactory.setProperty("plugin-directory", getDirectory().toFile().getPath());
        this.configuration = ConfigFactory.create(NucleusPluginConfiguration.class);

        this.addListener(new PlayerCommands(this));
        this.addListener(new DiscordBridge(this));
        this.addListener(this.chatManager);
        this.addListener(this.scheduler);
        this.addListener(new ChatTranslator(this, this.translator));
        this.addListener(new SharedCommands(this));
        this.addListener(new BlockInspector(this));
        this.addListener(new SaveCommands(this));
    }

    @Override
    public void onLoad() {
        Administration.Config.serverName.set("[cyan]<[white] Xpdustry [cyan]\uF821[white] "
                + configuration.getServerDisplayName() + " [cyan]>[white]");

        Administration.Config.motd.set(
                "[cyan]>>>[] Bienvenue sur [cyan]Xpdustry[], le seul serveur mindustry français. N'hésitez pas à nous rejoindre sur Discord avec la commande [cyan]/discord[].");

        final var random = new Random();
        this.scheduler.schedule().repeatPeriod(1L, TimeUnit.MINUTES).execute(() -> {
            final var quote = this.configuration
                    .getQuotes()
                    .get(random.nextInt(this.configuration.getQuotes().size()));
            Administration.Config.desc.set("\"" + quote + "\" [white]https://discord.xpdustry.fr");
        });
    }

    @Override
    public void onClientCommandsRegistration(final CommandHandler handler) {
        this.clientCommands.initialize(handler);
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
}
