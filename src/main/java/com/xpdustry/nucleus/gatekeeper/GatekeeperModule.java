// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.gatekeeper;

import com.xpdustry.nucleus.dependency.DependencyModule;
import com.xpdustry.nucleus.dependency.DependencyService;

public final class GatekeeperModule implements DependencyModule {

    @Override
    public void configure(final DependencyService.Binder binder) {
        binder.bindConstructor(GatekeeperPipeline.class, GatekeeperPipeline.class);
        binder.bindConstructor(GatekeeperController.class, GatekeeperController.class);
    }
}
