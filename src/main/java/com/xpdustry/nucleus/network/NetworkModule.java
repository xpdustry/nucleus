// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.network;

import com.xpdustry.nucleus.dependency.DependencyService;
import com.xpdustry.nucleus.dependency.Module;

public final class NetworkModule implements Module {

    @Override
    public void configure(final DependencyService.Binder binder) {
        binder.bindConstructor(NetworkEventBus.class, PostgresNetworkEventBus.class);
    }
}
