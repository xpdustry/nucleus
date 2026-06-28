// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.function;

@FunctionalInterface
public interface ThrowingFunction<I, O, T extends Throwable> {
    O apply(final I input) throws T;
}
