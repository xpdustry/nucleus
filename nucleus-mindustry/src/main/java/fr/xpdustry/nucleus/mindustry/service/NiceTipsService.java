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
package fr.xpdustry.nucleus.mindustry.service;

import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import java.util.List;
import java.util.concurrent.TimeUnit;
import mindustry.Vars;
import mindustry.gen.Call;

public final class NiceTipsService implements PluginListener {

    private static final List<Tip> TIPS = List.of(
            new Tip("Did you know we have a discord server?", "You can join it with the command [accent]/discord[]."),
            new Tip("Did you know we have a website?", "You can visit it with the command [accent]/website[]."),
            new Tip(
                    "Did you know we have several mindustry servers?",
                    "You can switch to any of them with the command [accent]/switch [[server][]."),
            new Tip(
                    "The votekick command is disabled in this server.",
                    "You can still report players with the command [cyan]/report <player> <reason>[] [lightgray](You "
                            + "don't need to type the whole name, the first letters are enough)[]."));

    private final NucleusPlugin nucleus;
    private int counter = -1;

    public NiceTipsService(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public void onPluginInit() {
        this.nucleus
                .getScheduler()
                .schedule()
                .sync()
                .repeatInterval(5L, TimeUnit.MINUTES)
                .execute(() -> {
                    if (Vars.state.isPlaying()) {
                        final var tip = TIPS.get(counter = (counter + 1) % TIPS.size());
                        Call.sendMessage(
                                "[cyan]>>> [accent]Nice tip: " + tip.title() + "\n[lightgray]" + tip.content());
                    }
                });
    }

    private record Tip(String title, String content) {}
}
