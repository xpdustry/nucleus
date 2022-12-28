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

import fr.xpdustry.javelin.JavelinSocket;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class JavelinMessengerTest {

    private JavelinMessenger server;
    private JavelinMessenger client;

    @BeforeEach
    public void setup() {
        this.server = new JavelinMessenger(JavelinSocket.server(10000, 1, true, (username, password) -> true), 10);
        this.client = new JavelinMessenger(JavelinSocket.client(URI.create("ws://localhost:10000"), 1), 10);

        this.server.start();
        this.client.start();
    }

    @AfterEach
    public void teardown() {
        this.server.close();
        this.client.close();
    }

    @Test
    public void testMessage() {
        final var future = new CompletableFuture<TestMessage>();
        this.server.subscribe(TestMessage.class, future::complete);
        this.client.send(new TestMessage("Hello world!"));

        Assertions.assertTimeout(Duration.ofSeconds(3), () -> {
            Assertions.assertEquals("Hello world!", future.get().payload());
        });
    }

    @Test
    public void testRequest() {
        this.server.respond(SumRequest.class, request -> request.a + request.b);
        final var future = this.client.request(new SumRequest(1, 2));

        Assertions.assertTimeout(Duration.ofSeconds(3), () -> {
            Assertions.assertEquals(3, future.get());
        });
    }

    private record SumRequest(int a, int b) implements Request<Integer> {}

    private record TestMessage(String payload) implements Message {}
}
