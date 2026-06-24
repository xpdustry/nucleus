// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.function;

@FunctionalInterface
public interface ThrowingConsumer<V, T extends Throwable> {
    void accept(final V value) throws T;
}
