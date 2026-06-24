// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.function;

import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface ThrowingFunction<I, O extends @Nullable Object, T extends Throwable> {
    O apply(final I input) throws T;
}
