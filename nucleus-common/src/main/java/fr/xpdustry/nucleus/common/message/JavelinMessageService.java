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
import fr.xpdustry.nucleus.common.application.NucleusListener;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class JavelinMessageService implements MessageService, NucleusListener {

    private final JavelinSocket socket;

    public JavelinMessageService(final JavelinSocket socket) {
        this.socket = socket;
    }

    @Override
    public void publish(final Message message) {
        if (this.socket.getStatus() == JavelinSocket.Status.OPEN) {
            this.socket.sendEvent(new NucleusMessage(message));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <M extends Message> void subscribe(final Class<M> clazz, final Consumer<M> subscriber) {
        this.socket.subscribe(NucleusMessage.class, event -> {
            if (clazz.isAssignableFrom(event.message().getClass())) {
                subscriber.accept((M) event.message());
            }
        });
    }

    @Override
    public boolean isOperational() {
        return this.socket.getStatus() == JavelinSocket.Status.OPEN;
    }

    @Override
    public void onNucleusInit() {
        this.socket.start().orTimeout(15L, TimeUnit.SECONDS).join();
    }

    @Override
    public void onNucleusExit() {
        this.socket.close().orTimeout(15L, TimeUnit.SECONDS).join();
    }

    private record NucleusMessage(Message message) implements JavelinEvent {}
}
