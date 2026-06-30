// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.message;

import com.google.gson.Gson;
import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.nucleus.config.ConfigManager;
import com.xpdustry.nucleus.config.ConfigPropertyKey;
import com.xpdustry.nucleus.database.PostgresDatabase;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jspecify.annotations.Nullable;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MessagePublisher implements PluginListener {

    private static final String CHANNEL_NAME = "nucleus_network_event_v1";
    private static final Logger log = LoggerFactory.getLogger(MessagePublisher.class);

    private final ConfigManager config;
    private final PostgresDatabase database;

    private final Gson gson = new Gson();
    private final Map<String, List<MessageSubscriber<?>>> subscribers = new ConcurrentHashMap<>();

    private final ExecutorService dispatcher = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("nucleus-network-event-dispatcher").factory());

    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("nucleus-network-event-poller").factory());

    private @Nullable Future<?> polling = null;

    public MessagePublisher(final ConfigManager config, final PostgresDatabase database) {
        this.config = config;
        this.database = database;
    }

    public <E extends Message> void subscribe(final Class<E> event, final MessageSubscriber<E> subscriber) {
        this.subscribers
                .computeIfAbsent(event.getName(), _ -> new CopyOnWriteArrayList<>())
                .add(subscriber);
    }

    public <E extends Message> void publish(final E event) {
        final var payload = new StringBuilder();
        try {
            payload.append(event.getClass().getName());
            payload.append('|');
            payload.append(this.config.get(ConfigPropertyKey.SERVER_NAME));
            payload.append('|');
            payload.append(this.gson.toJson(event));
        } catch (final Exception e) {
            log.error(
                    "Failed to serialize an event of type {}", event.getClass().getName(), e);
            return;
        }
        if (payload.length() > 2000) {
            log.error(
                    "Failed to serialize event of type {}, payload is too large, got {}",
                    event.getClass().getName(),
                    payload.length());
            return;
        }
        this.database.withHandle(handle -> handle.prepareStatement("SELECT pg_notify(?, ?)")
                .push(CHANNEL_NAME)
                .push(payload.toString())
                .executeSelect(_ -> Boolean.TRUE));
    }

    private void poll(final CompletableFuture<Boolean> running) {
        try (final var connection = this.database.newOrphanConnection()) {
            connection.setAutoCommit(true);
            try (final var statement = connection.createStatement()) {
                statement.execute("LISTEN \"" + CHANNEL_NAME + "\"");
            }
            running.complete(true);
            final var unwrapped = connection.unwrap(PGConnection.class);
            while (!Thread.currentThread().isInterrupted()) {
                final var notifications = unwrapped.getNotifications(1000);
                if (notifications == null) {
                    continue;
                }
                for (final var notification : notifications) {
                    this.dispatcher.execute(() -> this.dispatch(notification));
                }
            }
        } catch (final Exception e) {
            log.error("An error occurred while polling nucleus network events", e);
            running.completeExceptionally(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends Message> void dispatch(final PGNotification notification) {
        if (!notification.getName().equals(CHANNEL_NAME)) {
            return;
        }
        final var parts = notification.getParameter().split("\\|", 3);
        if (parts.length != 3) {
            return;
        }
        final var subscribers = this.subscribers.get(parts[0]);
        if (subscribers == null) {
            return;
        }
        final E event;
        try {
            event = this.gson.fromJson(parts[2], (Class<E>) Class.forName(parts[0]));
        } catch (final ClassNotFoundException e) {
            return;
        } catch (final Exception e) {
            log.error("Failed to deserialize an event of type {}", parts[0], e);
            return;
        }
        for (final var subscriber : subscribers) {
            ((MessageSubscriber<E>) subscriber).onMessage(parts[1], event);
        }
    }

    @Override
    public void onInit() {
        final var running = new CompletableFuture<Boolean>();
        this.polling = this.poller.scheduleWithFixedDelay(() -> this.poll(running), 0, 5, TimeUnit.SECONDS);
        try {
            running.get(5L, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting the nucleus network event listener", e);
        } catch (final ExecutionException | TimeoutException e) {
            this.onExit();
            throw new IllegalStateException("Failed to start the nucleus network event listener", e);
        }
    }

    @Override
    public void onExit() {
        Objects.requireNonNull(this.polling, "polling").cancel(true);
        this.poller.close();
        this.dispatcher.close();
    }
}
