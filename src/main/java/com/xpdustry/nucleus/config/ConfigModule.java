// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.config;

import com.xpdustry.nucleus.dependency.DependencyService;
import com.xpdustry.nucleus.dependency.Module;

public final class ConfigModule implements Module {
    @Override
    public void configure(final DependencyService.Binder binder) {
        binder.bindConstructor(ConfigKeyRegistry.class);
    }
}
