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

import arc.Core;
import fr.xpdustry.distributor.api.DistributorProvider;
import fr.xpdustry.distributor.api.plugin.AbstractMindustryPlugin;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.nucleus.api.application.ClasspathScanner;
import fr.xpdustry.nucleus.api.application.NucleusInjector;
import fr.xpdustry.nucleus.api.application.lifecycle.LifecycleListener;
import fr.xpdustry.nucleus.api.application.lifecycle.LifecycleListenerRepository;
import fr.xpdustry.nucleus.api.event.Event;
import fr.xpdustry.nucleus.api.event.EventService;
import fr.xpdustry.nucleus.common.NucleusCommonModule;
import fr.xpdustry.nucleus.common.lifecycle.SimpleNucleusInjector;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.slf4j.Logger;

public final class NucleusPlugin extends AbstractMindustryPlugin {

    private @MonotonicNonNull NucleusInjector injector = null;

    public NucleusInjector getInjector() {
        return injector;
    }

    @Override
    public void onLoad() {
        final LifecycleListenerRepository repository =
                listener -> this.addListener(new PluginListenerAdapter(listener));
        this.injector =
                new SimpleNucleusInjector(repository, new NucleusCommonModule(), new NucleusMindustryModule(this));

        final var logger = this.injector.getInstance(Logger.class);
        final var scanner = this.injector.getInstance(ClasspathScanner.class);
        final var events = this.injector.getInstance(EventService.class);

        logger.info("Registering listeners...");
        scanner.getListeners(LifecycleListener.class).forEach(clazz -> {
            logger.info("> Listener {}", clazz.getSimpleName());
            repository.register(this.injector.getInstance(clazz));
        });

        // Transmits the nucleus events to the distributor event bus since distributor doesn't propagate events up
        // the class hierarchy
        events.subscribe(
                Event.class,
                e -> Core.app.post(() -> DistributorProvider.get().getEventBus().post(e)));
    }

    @Override
    public void addListener(final PluginListener listener) {
        if (this.getListeners().contains(listener)) {
            return;
        }
        super.addListener(listener);
        if (listener instanceof PluginListenerAdapter adapter) {
            DistributorProvider.get().getPluginScheduler().parse(this, adapter.listener);
            DistributorProvider.get().getEventBus().parse(this, adapter.listener);
        }
    }

    private static final class PluginListenerAdapter implements PluginListener {

        private final LifecycleListener listener;

        private PluginListenerAdapter(final LifecycleListener listener) {
            this.listener = listener;
        }

        @Override
        public void onPluginLoad() {
            listener.onLifecycleInit();
        }

        @Override
        public void onPluginExit() {
            listener.onLifecycleExit();
        }

        @Override
        public int hashCode() {
            return listener.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final PluginListenerAdapter that = (PluginListenerAdapter) obj;
            return listener.equals(that.listener);
        }
    }
}
