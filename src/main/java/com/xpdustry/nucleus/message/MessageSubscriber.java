// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.message;

@FunctionalInterface
public interface MessageSubscriber<E> {

    void onMessage(final String sender, final E event);
}
