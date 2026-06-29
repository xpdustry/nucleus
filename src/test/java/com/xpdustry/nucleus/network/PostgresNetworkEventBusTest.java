// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.network;

import com.xpdustry.nucleus.config.ConfigManagerImpl;
import com.xpdustry.nucleus.config.ConfigPropertyKey;
import com.xpdustry.nucleus.database.PostgresDatabaseImpl;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

// TODO AI SLOP
final class PostgresNetworkEventBusTest {

    @TempDir
    private Path home;

    @Test
    void test_simple() throws InterruptedException {
        final var root = new ConfigManagerImpl().set(ConfigPropertyKey.DATABASE_EMBEDDED, true);
        final var database = new PostgresDatabaseImpl(root, this.home.resolve("postgres"));

        final var client1 =
                new PostgresNetworkEventBus(root.clone().set(ConfigPropertyKey.SERVER_NAME, "client1"), database);
        final var client2 =
                new PostgresNetworkEventBus(root.clone().set(ConfigPropertyKey.SERVER_NAME, "client2"), database);

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

        try {
            client1.publish(new TestEvent("Hello world"));

            assertThat(delivered.await(10L, TimeUnit.SECONDS)).isTrue();
            assertThat(deliveries)
                    .containsExactlyInAnyOrder(
                            new Delivery("client1", "client1", new TestEvent("Hello world")),
                            new Delivery("client2", "client1", new TestEvent("Hello world")));
        } finally {
            client2.onExit();
            client1.onExit();
            database.onExit();
        }
    }

    private record Delivery(String recipient, String sender, TestEvent event) {}

    private record TestEvent(String message) implements NetworkEvent {}
}
