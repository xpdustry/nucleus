// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.metric;

import java.util.Map;

public interface MetricSink {

    default void sample(final String name, final MetricType type, final Number value) {
        this.sample(name, type, value, Map.of());
    }

    void sample(final String name, final MetricType type, final Number value, final Map<String, String> labels);
}
