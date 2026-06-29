// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.network;

@FunctionalInterface
public interface NetworkEventSubscriber<E> {

    void onNetworkEvent(final String sender, final E event);
}
