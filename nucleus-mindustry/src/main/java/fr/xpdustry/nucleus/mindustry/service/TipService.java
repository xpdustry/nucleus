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

import fr.xpdustry.distributor.api.DistributorProvider;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import mindustry.Vars;
import mindustry.gen.Call;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public final class TipService implements PluginListener {

    private final Object lock = new Object();
    private final List<Tip> tips = new ArrayList<>();
    private int counter = -1;

    private final NucleusPlugin nucleus;
    private final ConfigurationLoader<?> loader;

    public TipService(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
        this.loader = YamlConfigurationLoader.builder()
                .url(nucleus.getConfiguration().getTipsUrl())
                .build();
    }

    @Override
    public void onPluginInit() {
        DistributorProvider.get()
                .getPluginScheduler()
                .scheduleAsync(this.nucleus)
                .repeat(1L, TimeUnit.HOURS)
                .execute(this::load);

        DistributorProvider.get()
                .getPluginScheduler()
                .scheduleSync(this.nucleus)
                .delay(1L, TimeUnit.MINUTES)
                .repeat(5L, TimeUnit.MINUTES)
                .execute(this::show);
    }

    private void load() {
        try {
            final List<Tip> result = loader.load().childrenList().stream()
                    .map(child -> new Tip(
                            child.node("title").getString(),
                            child.node("content").getString()))
                    .toList();
            synchronized (lock) {
                counter = -1;
                tips.clear();
                tips.addAll(result);
            }
        } catch (final ConfigurateException e) {
            this.nucleus.getLogger().error("Failed to load tips.", e);
        }
    }

    private void show() {
        synchronized (lock) {
            if (Vars.state.isPlaying() && !tips.isEmpty()) {
                final var tip = tips.get(counter = (counter + 1) % tips.size());
                Call.sendMessage("[cyan]>>> [accent]Nice tip: " + tip.title() + "\n[lightgray]" + tip.content());
            }
        }
    }

    private record Tip(String title, String content) {}
}
