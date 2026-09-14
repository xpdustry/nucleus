// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.metric;

import com.xpdustry.nucleus.dependency.DependencyModule;
import com.xpdustry.nucleus.dependency.DependencyService;

public final class MetricModule implements DependencyModule {

    @Override
    public void configure(final DependencyService.Binder binder) {
        binder.bindConstructor(MetricRegistry.class, PostgresMetricRegistry.class);
        binder.bindConstructor(MindustryMetricCollector.class);
        binder.bindConstructor(MetricCollectorHandlerProcessor.class);
    }
}
