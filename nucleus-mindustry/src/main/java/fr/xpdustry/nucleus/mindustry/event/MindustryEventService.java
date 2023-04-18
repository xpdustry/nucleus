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
package fr.xpdustry.nucleus.mindustry.event;

import arc.Events;
import fr.xpdustry.distributor.api.DistributorProvider;
import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.nucleus.api.event.Event;
import fr.xpdustry.nucleus.api.event.EventService;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.inject.Inject;

public final class MindustryEventService implements EventService {

    private final MindustryPlugin plugin;

    @Inject
    public MindustryEventService(final MindustryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void publish(final Event event) {
        final Queue<Class<?>> classes = new ArrayDeque<>();
        classes.add(event.getClass());
        final Set<Class<?>> visited = new HashSet<>();

        while (!classes.isEmpty()) {
            final var clazz = classes.remove();
            Events.fire(clazz, event);
            classes.addAll(Stream.of(clazz.getInterfaces())
                    .filter(inter -> Event.class.isAssignableFrom(inter) && visited.add(inter))
                    .toList());
            if (clazz.getSuperclass() != null && Event.class.isAssignableFrom(clazz.getSuperclass())) {
                classes.add(clazz.getSuperclass());
            }
        }
    }

    @Override
    public <E extends Event> void subscribe(final Class<E> clazz, final Consumer<E> subscriber) {
        DistributorProvider.get().getEventBus().subscribe(clazz, this.plugin, subscriber);
    }
}
