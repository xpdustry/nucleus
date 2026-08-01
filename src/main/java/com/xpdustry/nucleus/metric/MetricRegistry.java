// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.metric;

public interface MetricRegistry {

    void register(final MetricCollector collector);
}
