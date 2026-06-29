// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.network;

import com.google.gson.Gson;
import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.nucleus.config.ConfigKeyRegistry;
import com.xpdustry.nucleus.config.StandardConfigKeys;
import com.xpdustry.nucleus.database.PostgresService;
import com.xpdustry.nucleus.dependency.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PostgresNetworkEventBus implements NetworkEventBus, PluginListener {

    private static final String CHANNEL_NAME = "nucleus_network_event_v1";
    private static final Logger log = LoggerFactory.getLogger(PostgresNetworkEventBus.class);

    private final ConfigKeyRegistry config;
    private final PostgresService database;
    private final Gson gson = new Gson();

    @SuppressWarnings("rawtypes")
    private final Map<String, List<NetworkEventSubscriber>> subscribers = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
            4, Thread.ofPlatform().daemon().name("nucleus-network-event-worker").factory());

    @Inject
    public PostgresNetworkEventBus(final ConfigKeyRegistry config, final PostgresService database) {
        this.config = config;
        this.database = database;
    }

    @Override
    public void onInit() {
        this.executor.scheduleWithFixedDelay(this::poll, 0, 5, TimeUnit.SECONDS);
    }

    @Override
    public void onExit() {
        this.executor.close();
    }

    private void poll() {
        try (final var connection = this.database.newOrphanConnection()) {
            try (final var statement = connection.createStatement()) {
                statement.execute("LISTEN \"" + CHANNEL_NAME + "\"");
            }
            final var unwrapped = connection.unwrap(PGConnection.class);
            while (!Thread.currentThread().isInterrupted()) {
                final var notifications = unwrapped.getNotifications(1000);
                if (notifications == null) {
                    continue;
                }
                for (final var notification : notifications) {
                    this.executor.execute(() -> this.dispatch(notification));
                }
            }
        } catch (final Exception e) {
            log.error("An error occurred while polling nucleus network events", e);
        }
    }

    @Override
    public <E extends NetworkEvent> void subscribe(final Class<E> event, final NetworkEventSubscriber<E> subscriber) {
        this.subscribers
                .computeIfAbsent(event.getSimpleName(), _ -> new CopyOnWriteArrayList<>())
                .add(subscriber);
    }

    @Override
    public <E extends NetworkEvent> void publish(final E event) {
        final var payload = new StringBuilder();
        try {
            payload.append(event.getClass().getName());
            payload.append('|');
            payload.append(this.config.get(StandardConfigKeys.NODE_NAME));
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
                .executeSingleUpdate());
    }

    @SuppressWarnings("unchecked")
    private <E extends NetworkEvent> void dispatch(final PGNotification notification) {
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
            subscriber.onNetworkEvent(parts[1], event);
        }
    }
}
