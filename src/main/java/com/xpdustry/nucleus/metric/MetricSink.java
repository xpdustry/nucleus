// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.metric;

import java.util.Map;

public interface MetricSink {

    default void sample(final String name, final Number value) {
        this.sample(name, value, Map.of());
    }

    void sample(final String name, final Number value, final Map<String, String> labels);
}
