// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.message;

import com.google.gson.Gson;
import com.xpdustry.foundation.annotation.ScheduledTaskHandler;
import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.foundation.scheduler.MindustryTask;
import com.xpdustry.foundation.scheduler.MindustryTimeUnit;
import com.xpdustry.nucleus.concurrent.Async;
import com.xpdustry.nucleus.config.ConfigManager;
import com.xpdustry.nucleus.config.ConfigPropertyKey;
import com.xpdustry.nucleus.database.PostgresDatabase;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MessagePublisher implements PluginListener {

    private static final Logger log = LoggerFactory.getLogger(MessagePublisher.class);
    private static final String CHANNEL_NAME = "nucleus_message_v1";

    private final ConfigManager config;
    private final Executor executor;
    private final PostgresDatabase database;
    private final Gson gson = new Gson();
    private final Map<String, List<MessageSubscriber<?>>> subscribers = new ConcurrentHashMap<>();

    public MessagePublisher(final ConfigManager config, final Executor executor, final PostgresDatabase database) {
        this.config = config;
        this.executor = executor;
        this.database = database;
    }

    public <M extends Message> void subscribe(final Class<M> type, final MessageSubscriber<M> subscriber) {
        this.subscribers
                .computeIfAbsent(type.getName(), _ -> new CopyOnWriteArrayList<>())
                .add(subscriber);
    }

    public <M extends Message> void publish(final M message) {
        final var payload = new StringBuilder();
        try {
            payload.append(message.getClass().getName());
            payload.append('|');
            payload.append(this.config.get(ConfigPropertyKey.SERVER_NAME));
            payload.append('|');
            payload.append(this.gson.toJson(message));
        } catch (final Exception e) {
            log.error(
                    "Failed to serialize an message of type {}",
                    message.getClass().getName(),
                    e);
            return;
        }
        if (payload.length() > 2000) {
            log.error(
                    "Failed to serialize message of type {}, payload is too large, got {}",
                    message.getClass().getName(),
                    payload.length());
            return;
        }
        this.database.withTransaction(handle -> handle.prepareStatement("SELECT pg_notify(?, ?)")
                .bind(CHANNEL_NAME)
                .bind(payload.toString())
                .executeSelect(_ -> Boolean.TRUE));
    }

    @ScheduledTaskHandler(initialDelay = 0, delay = 5, unit = MindustryTimeUnit.SECONDS)
    @Async
    void onNotificationPoll(final MindustryTask task) {
        try (final var connection = this.database.newOrphanConnection()) {
            connection.setAutoCommit(true);
            try (final var statement = connection.createStatement()) {
                statement.execute("LISTEN \"" + CHANNEL_NAME + "\"");
            }
            final var unwrapped = connection.unwrap(PGConnection.class);
            while (!Thread.currentThread().isInterrupted() && task.state() == MindustryTask.State.SCHEDULED) {
                final var notifications = unwrapped.getNotifications(1000);
                if (notifications == null) {
                    continue;
                }
                for (final var notification : notifications) {
                    this.executor.execute(() -> this.dispatch(notification));
                }
            }
        } catch (final Exception e) {
            log.error("An error occurred while polling nucleus messages", e);
        }
    }

    @SuppressWarnings("unchecked")
    private <M extends Message> void dispatch(final PGNotification notification) {
        if (!notification.getName().equals(CHANNEL_NAME)) {
            return;
        }
        final var parts = notification.getParameter().split("\\|", 3);
        if (parts.length != 3) {
            return;
        }

        final var payloadType = parts[0];
        final var sender = parts[1];
        final var payload = parts[2];

        final var subscribers = this.subscribers.get(payloadType);
        if (subscribers == null) {
            return;
        }

        final M message;
        try {
            message = this.gson.fromJson(payload, (Class<M>) Class.forName(payloadType));
        } catch (final ClassNotFoundException e) {
            return;
        } catch (final Exception e) {
            log.error("Failed to deserialize message of type {}", payloadType, e);
            return;
        }

        for (final var subscriber : subscribers) {
            try {
                ((MessageSubscriber<M>) subscriber).onMessage(sender, message);
            } catch (final Exception e) {
                log.error("{} failed to handle message {} from {}", subscriber, message, sender);
            }
        }
    }
}
