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
import arc.util.Log;
import fr.xpdustry.distributor.api.plugin.ExtendedPlugin;
import fr.xpdustry.distributor.api.scheduler.PluginScheduler;
import fr.xpdustry.nucleus.mindustry.chat.DiscordBridge;
import fr.xpdustry.nucleus.mindustry.chat.NucleusChatFilter;
import fr.xpdustry.nucleus.mindustry.chat.NucleusChatProcessor;
import fr.xpdustry.nucleus.mindustry.commands.PlayerCommands;
import fr.xpdustry.nucleus.mindustry.commands.SharedCommands;
import fr.xpdustry.nucleus.mindustry.internal.NucleusPluginCommandManager;
import fr.xpdustry.nucleus.mindustry.translator.ChatTranslator;
import fr.xpdustry.nucleus.mindustry.translator.LibreTranslateTranslator;
import fr.xpdustry.nucleus.mindustry.translator.Translator;
import java.util.*;
import java.util.concurrent.TimeUnit;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Administration;
import org.aeonbits.owner.ConfigFactory;
import org.checkerframework.checker.nullness.qual.*;

public final class NucleusPlugin extends ExtendedPlugin {

    private final NucleusPluginCommandManager serverCommands = new NucleusPluginCommandManager(this);
    private final NucleusPluginCommandManager clientCommands = new NucleusPluginCommandManager(this);
    private final List<NucleusChatFilter> filters = new ArrayList<>();
    private final List<NucleusChatProcessor> processors = new ArrayList<>();
    private final PluginScheduler scheduler = PluginScheduler.create(this, 8);
    private final Translator translator = new LibreTranslateTranslator(this);
    private @MonotonicNonNull NucleusPluginConfiguration configuration;

    @Override
    public void onInit() {
        ConfigFactory.setProperty("plugin-directory", getDirectory().toFile().getPath());
        this.configuration = ConfigFactory.create(NucleusPluginConfiguration.class);
        this.addListener(new PlayerCommands(this));
        this.addListener(new DiscordBridge(this));
        this.addListener(new ChatTranslator(this, this.translator));
        this.addListener(this.scheduler);
        this.addListener(new SharedCommands(this));
    }

    @Override
    public void onLoad() {
        Vars.netServer.admins.addChatFilter(new NucleusChatInterceptor());

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

    public PluginScheduler getScheduler() {
        return scheduler;
    }

    private final class NucleusChatInterceptor implements Administration.ChatFilter {

        // TODO Improve the Chat API
        // TODO add thenCompose
        @Override
        public @Nullable String filter(final Player player, final String message) {
            scheduler.schedule().async().execute(() -> {
                if (filters.stream().allMatch(f -> f.filter(player, message))) {
                    Log.info("&fi@: @", "&lc" + player.plainName(), "&lw" + message);
                    Groups.player.each(receiver -> scheduler
                            .recipe(message)
                            .thenApplyAsync(m -> processors.stream()
                                    .reduce(m, (t, p) -> p.process(player, t, receiver), (t1, t2) -> t2))
                            .thenAccept(result -> Call.sendMessage(
                                    receiver.con(),
                                    Vars.netServer.chatFormatter.format(player, result),
                                    result,
                                    player))
                            .execute());
                }
            });
            return null;
        }
    }
}
