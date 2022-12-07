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
package fr.xpdustry.nucleus.common.message;

import fr.xpdustry.javelin.JavelinEvent;
import fr.xpdustry.javelin.JavelinSocket;
import fr.xpdustry.nucleus.api.message.Message;
import fr.xpdustry.nucleus.api.message.Messenger;
import fr.xpdustry.nucleus.api.message.Request;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("UnusedVariable") // <- Error prone is choking or what?
public class JavelinMessenger implements Messenger {

    private final Map<UUID, PendingRequest<?>> callbacks = new ConcurrentHashMap<>();
    private final Set<Class<? extends Request<?>>> responders = new HashSet<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("JavelinMessengerCleaner");
        thread.setDaemon(true);
        return thread;
    });

    private final JavelinSocket socket;
    private final int timeout;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public JavelinMessenger(final JavelinSocket socket, final int timeout) {
        this.socket = socket;
        this.timeout = timeout;

        this.socket.subscribe(NucleusResponse.class, response -> {
            final PendingRequest pending = callbacks.get(response.uuid());
            if (pending != null) {
                pending.callback().accept(response.response());
            }
        });

        this.executor.scheduleWithFixedDelay(
                () -> {
                    final long now = System.currentTimeMillis();
                    callbacks
                            .entrySet()
                            .removeIf(entry -> now - entry.getValue().timestamp() > (this.timeout * 1000L));
                },
                1L,
                1L,
                TimeUnit.SECONDS);
    }

    @Override
    public void send(final Message message) {
        socket.sendEvent(new NucleusMessage(message));
    }

    @Override
    public <R extends Message> void request(final Request<R> request, final Consumer<R> callback) {
        final var uuid = UUID.randomUUID();
        callbacks.put(uuid, new PendingRequest<>(callback, System.currentTimeMillis()));
        socket.sendEvent(new NucleusRequest(uuid, request));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <M extends Message> void subscribe(final Class<M> clazz, final Consumer<M> subscriber) {
        socket.subscribe(NucleusMessage.class, event -> {
            if (clazz.isAssignableFrom(event.message().getClass())) {
                subscriber.accept((M) event.message());
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Request<M>, M extends Message> void respond(
            final Class<R> clazz, final Function<R, M> responder) {
        if (!responders.add(clazz)) {
            throw new IllegalArgumentException("Responder already registered for " + clazz);
        }
        socket.subscribe(NucleusRequest.class, event -> {
            if (clazz.equals(event.request().getClass())) {
                socket.sendEvent(new NucleusResponse(event.uuid(), responder.apply((R) event.request())));
            }
        });
    }

    @Override
    public boolean isOpen() {
        return socket.getStatus() == JavelinSocket.Status.OPEN;
    }

    @Override
    public void start() {
        socket.start().orTimeout(15L, TimeUnit.SECONDS).join();
    }

    @Override
    public void close() {
        socket.close().orTimeout(15L, TimeUnit.SECONDS).join();
    }

    private record NucleusMessage(Message message) implements JavelinEvent {}

    private record NucleusRequest(UUID uuid, Request<?> request) implements JavelinEvent {}

    private record NucleusResponse(UUID uuid, Message response) implements JavelinEvent {}

    private record PendingRequest<R>(Consumer<R> callback, long timestamp) {}
}
