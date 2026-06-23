// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.pipeline;

import com.xpdustry.foundation.util.Priority;

public interface ProcessorPipeline<I, O> {

    void register(final String name, final Priority priority, final Processor<I, O> processor);

    O pump(final I context);
}
