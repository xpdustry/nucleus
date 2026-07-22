// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.metric;

@FunctionalInterface
public interface MetricCollector {

    void flush(final MetricSink sink);
}
