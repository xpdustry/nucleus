// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.metric;

import arc.Core;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@FunctionalInterface
public interface MetricCollector {

    void flush(final MetricSink sink);

    static MetricCollector withMainThread(final MetricCollector collector) {
        return sink -> CompletableFuture.runAsync(() -> collector.flush(sink), Core.app::post)
                .orTimeout(5, TimeUnit.SECONDS)
                .join();
    }
}
