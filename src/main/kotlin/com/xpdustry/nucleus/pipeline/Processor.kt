// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.pipeline;

@FunctionalInterface
public interface Processor<I, O> {

    O process(final I context);
}
