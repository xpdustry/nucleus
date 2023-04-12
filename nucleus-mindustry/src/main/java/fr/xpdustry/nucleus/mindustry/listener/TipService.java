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
package fr.xpdustry.nucleus.mindustry.listener;

import fr.xpdustry.distributor.api.scheduler.MindustryTimeUnit;
import fr.xpdustry.distributor.api.scheduler.TaskHandler;
import fr.xpdustry.nucleus.api.annotation.NucleusAutoListener;
import fr.xpdustry.nucleus.api.application.lifecycle.LifecycleListener;
import fr.xpdustry.nucleus.mindustry.NucleusPluginConfiguration;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import mindustry.Vars;
import mindustry.gen.Call;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

@NucleusAutoListener
public final class TipService implements LifecycleListener {

    private final Object lock = new Object();
    private final List<Tip> tips = new ArrayList<>();
    private int counter = -1;

    private final NucleusPluginConfiguration configuration;
    private final ConfigurationLoader<?> loader;

    @Inject
    private Logger logger;

    @Inject
    public TipService(final NucleusPluginConfiguration configuration) {
        this.configuration = configuration;
        this.loader = YamlConfigurationLoader.builder()
                .url(this.configuration.getTipsUrl())
                .build();
    }

    @TaskHandler(interval = 1L, unit = MindustryTimeUnit.HOURS, async = true)
    public void onTipsReload() {
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
            this.logger.debug("Loaded {} tips from {}.", result.size(), this.configuration.getTipsUrl());
        } catch (final ConfigurateException e) {
            this.logger.error("Failed to load tips.", e);
        }
    }

    @TaskHandler(interval = 5L, unit = MindustryTimeUnit.MINUTES)
    public void onTipDisplay() {
        synchronized (lock) {
            if (Vars.state.isPlaying() && !tips.isEmpty()) {
                final var tip = tips.get(counter = (counter + 1) % tips.size());
                Call.sendMessage("[cyan]>>> [accent]Nice tip: " + tip.title() + "\n[lightgray]" + tip.content());
            }
        }
    }

    private record Tip(String title, String content) {}
}
