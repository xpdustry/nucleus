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
package fr.xpdustry.nucleus.core.message;

import fr.xpdustry.javelin.JavelinEvent;
import fr.xpdustry.javelin.JavelinSocket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("UnusedVariable") // <- Error prone is choking or what?
public final class JavelinMessenger implements Messenger {

    private final Map<String, PendingRequest<?>> callbacks = new ConcurrentHashMap<>();
    private final Set<Class<? extends Request<?>>> responders = new HashSet<>();

    private final JavelinSocket socket;
    private final int timeout;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public JavelinMessenger(final JavelinSocket socket, final int timeout) {
        this.socket = socket;
        this.timeout = timeout;

        this.socket.subscribe(NucleusResponse.class, response -> {
            final PendingRequest pending = callbacks.remove(response.uuid());
            if (pending != null) {
                pending.callback().complete(response.response());
            }
        });

        Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("JavelinMessengerCleaner");
                    thread.setDaemon(true);
                    return thread;
                })
                .scheduleWithFixedDelay(
                        () -> {
                            final long now = System.currentTimeMillis();
                            final var iterator = callbacks.entrySet().iterator();
                            while (iterator.hasNext()) {
                                final var pending = iterator.next().getValue();
                                if (now - pending.timestamp() > this.timeout * 1000L) {
                                    pending.callback().completeExceptionally(new TimeoutException());
                                    iterator.remove();
                                }
                            }
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
    public <R> CompletableFuture<R> request(final Request<R> request) {
        final var uuid = UUID.randomUUID().toString();
        final var callback = new CompletableFuture<R>();
        callbacks.put(uuid, new PendingRequest<>(callback, System.currentTimeMillis()));
        socket.sendEvent(new NucleusRequest(uuid, request));
        return callback.copy();
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
    public <R extends Request<M>, M> void respond(final Class<R> clazz, final Function<R, M> responder) {
        if (!responders.add(clazz)) {
            throw new IllegalArgumentException("Responder already registered for " + clazz);
        }
        socket.subscribe(NucleusRequest.class, event -> {
            if (clazz.isAssignableFrom(event.request().getClass())) {
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

    private record NucleusRequest(String uuid, Request<?> request) implements JavelinEvent {}

    private record NucleusResponse(String uuid, Object response) implements JavelinEvent {}

    private record PendingRequest<R>(CompletableFuture<R> callback, long timestamp) {}
}
