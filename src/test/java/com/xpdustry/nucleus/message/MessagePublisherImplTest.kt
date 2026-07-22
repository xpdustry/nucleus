// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.message

import com.xpdustry.nucleus.config.ConfigManager
import com.xpdustry.nucleus.config.ConfigPropertyKey
import com.xpdustry.nucleus.database.PostgresDatabaseImpl
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

internal class MessagePublisherImplTest {
    @TempDir private lateinit var home: Path

    @Test
    @Throws(InterruptedException::class)
    fun test_simple() {
        val root = ConfigManager().set(ConfigPropertyKey.DATABASE_EMBEDDED, true)
        val database = PostgresDatabaseImpl(root, this.home.resolve("postgres"))

        val client1 = MessagePublisher(root.fork().set(ConfigPropertyKey.SERVER_NAME, "client1"), database)
        val client2 = MessagePublisher(root.fork().set(ConfigPropertyKey.SERVER_NAME, "client2"), database)

        database.onInit()
        client1.onInit()
        client2.onInit()

        val deliveries = ConcurrentLinkedQueue<Delivery>()
        val delivered = CountDownLatch(2)
        client1.subscribe<TestEvent>(
            TestEvent::class.java,
            MessageSubscriber<TestEvent> { sender: String, event: TestEvent? ->
                deliveries.add(Delivery("client1", sender, event!!))
                delivered.countDown()
            },
        )
        client2.subscribe<TestEvent>(
            TestEvent::class.java,
            MessageSubscriber<TestEvent> { sender: String, event: TestEvent? ->
                deliveries.add(Delivery("client2", sender, event!!))
                delivered.countDown()
            },
        )

        try {
            client1.publish(TestEvent("Hello world"))

            Assertions.assertThat(delivered.await(10L, TimeUnit.SECONDS)).isTrue()
            Assertions.assertThat<Delivery>(deliveries)
                .containsExactlyInAnyOrder(
                    Delivery("client1", "client1", TestEvent("Hello world")),
                    Delivery("client2", "client1", TestEvent("Hello world")),
                )
        } finally {
            client2.onExit()
            client1.onExit()
            database.onExit()
        }
    }

    @JvmRecord private data class Delivery(val recipient: String, val sender: String, val event: TestEvent)

    @JvmRecord private data class TestEvent(val message: String) : Message
}
