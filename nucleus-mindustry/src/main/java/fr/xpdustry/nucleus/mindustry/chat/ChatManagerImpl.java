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
package fr.xpdustry.nucleus.mindustry.chat;

import arc.util.Log;
import fr.xpdustry.distributor.api.DistributorProvider;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public class ChatManagerImpl implements ChatManager, PluginListener {

    private final List<ChatFilter> filters = new ArrayList<>();
    private final List<ChatProcessor> processors = new ArrayList<>();
    private final NucleusPlugin plugin;

    public ChatManagerImpl(final NucleusPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendMessage(
            final Player player,
            final String message,
            final Predicate<Player> filter,
            final Function<String, String> formatter) {
        this.sendMessage(player, message, filter, formatter, false);
    }

    @Override
    public void addFilter(final ChatFilter filter) {
        this.filters.add(filter);
    }

    @Override
    public void addProcessor(final ChatProcessor processor) {
        this.processors.add(processor);
    }

    @Override
    public void onPluginInit() {
        Vars.netServer.admins.addChatFilter((player, message) -> {
            DistributorProvider.get()
                    .getPluginScheduler()
                    .scheduleAsync(plugin)
                    .execute(() -> this.sendMessage(player, message, p -> true, s -> s, true));
            return null;
        });
    }

    private void sendMessage(
            final Player player,
            final String message,
            final Predicate<Player> filter,
            final Function<String, String> formatter,
            final boolean log) {
        if (!filters.stream().allMatch(f -> f.filter(player, message))) {
            return;
        }
        if (log) {
            Log.info("&fi@: @", "&lc" + player.plainName(), "&lw" + message);
        }
        Groups.player.each(filter::test, receiver -> DistributorProvider.get()
                .getPluginScheduler()
                .recipe(plugin, message)
                .thenApplyAsync(
                        m -> processors.stream().reduce(m, (t, p) -> p.process(player, t, receiver), (t1, t2) -> t2))
                .thenAccept(result -> Call.sendMessage(
                        receiver.con(),
                        formatter.apply(Vars.netServer.chatFormatter.format(player, result)),
                        result,
                        player))
                .execute());
    }
}
