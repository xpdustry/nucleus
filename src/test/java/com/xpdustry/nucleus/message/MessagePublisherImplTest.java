// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.message;

import com.xpdustry.foundation.scheduler.MindustryTask;
import com.xpdustry.nucleus.annotation.AiSlop;
import com.xpdustry.nucleus.config.ConfigManager;
import com.xpdustry.nucleus.config.ConfigPropertyKey;
import com.xpdustry.nucleus.database.PostgresDatabaseImpl;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

@AiSlop
final class MessagePublisherImplTest {

    @TempDir
    private Path home;

    @Test
    void test_simple() throws InterruptedException {
        final var root = new ConfigManager().set(ConfigPropertyKey.DATABASE_EMBEDDED, true);
        final var database = new PostgresDatabaseImpl(root, this.home.resolve("postgres"));

        final var client1 = new MessagePublisher(
                root.fork().set(ConfigPropertyKey.SERVER_NAME, "client1"), Runnable::run, database);
        final var client2 = new MessagePublisher(
                root.fork().set(ConfigPropertyKey.SERVER_NAME, "client2"), Runnable::run, database);

        final var polling = new AtomicBoolean(true);
        final var task = new MindustryTask() {
            @Override
            public State state() {
                return polling.get() ? State.SCHEDULED : State.CANCELLED;
            }

            @Override
            public void cancel() {
                polling.set(false);
            }
        };

        database.onInit();
        client1.onInit();
        client2.onInit();

        final var deliveries = new ConcurrentLinkedQueue<Delivery>();
        final var delivered = new CountDownLatch(2);
        client1.subscribe(TestEvent.class, (sender, event) -> {
            deliveries.add(new Delivery("client1", sender, event));
            delivered.countDown();
        });
        client2.subscribe(TestEvent.class, (sender, event) -> {
            deliveries.add(new Delivery("client2", sender, event));
            delivered.countDown();
        });

        final var poller1 = Thread.ofVirtual().start(() -> client1.onNotificationPoll(task));
        final var poller2 = Thread.ofVirtual().start(() -> client2.onNotificationPoll(task));

        try {
            awaitListenerCount(database, 2, Duration.ofSeconds(10));
            client1.publish(new TestEvent("Hello world"));

            assertThat(delivered.await(10L, TimeUnit.SECONDS)).isTrue();
            assertThat(deliveries)
                    .containsExactlyInAnyOrder(
                            new Delivery("client1", "client1", new TestEvent("Hello world")),
                            new Delivery("client2", "client1", new TestEvent("Hello world")));
        } finally {
            task.cancel();
            poller1.interrupt();
            poller2.interrupt();
            poller1.join();
            poller2.join();
            client2.onExit();
            client1.onExit();
            database.onExit();
        }
    }

    private static void awaitListenerCount(
            final PostgresDatabaseImpl database, final int expected, final Duration timeout)
            throws InterruptedException {
        final var deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            final var count = database.withTransaction(transaction -> transaction
                    .prepareStatement("SELECT count(*) FROM pg_stat_activity WHERE query = ?")
                    .bind("LISTEN \"nucleus_message_v1\"")
                    .executeSelect(result -> result.getInt(1))
                    .getFirst());
            if (count == expected) {
                return;
            }
            Thread.sleep(10L);
        }
        throw new AssertionError("Timed out waiting for " + expected + " PostgreSQL message listeners");
    }

    private record Delivery(String recipient, String sender, TestEvent event) {}

    private record TestEvent(String message) implements Message {}
}
