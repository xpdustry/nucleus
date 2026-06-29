// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.network;

public interface NetworkEventBus {

    <E extends NetworkEvent> void subscribe(final Class<E> event, final NetworkEventSubscriber<E> subscriber);

    <E extends NetworkEvent> void publish(final E event);
}
