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
package fr.xpdustry.nucleus.common.event;

import fr.xpdustry.nucleus.api.application.NucleusRuntime;
import fr.xpdustry.nucleus.api.event.Event;
import fr.xpdustry.nucleus.api.event.EventService;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javax.inject.Inject;
import net.kyori.event.EventBus;

public final class KyoriEventService implements EventService {

    private final EventBus<Event> bus = EventBus.create(Event.class);
    private final Executor executor;

    @Inject
    public KyoriEventService(final NucleusRuntime runtime) {
        this.executor = runtime.getAsyncExecutor();
    }

    @Override
    public void publish(final Event event) {
        executor.execute(() -> this.bus.post(event));
    }

    @Override
    public <E extends Event> void subscribe(final Class<E> clazz, final Consumer<E> subscriber) {
        this.bus.subscribe(clazz, subscriber::accept);
    }
}
