// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.network;

import com.xpdustry.nucleus.dependency.DependencyModule;
import com.xpdustry.nucleus.dependency.DependencyService;

public final class NetworkModule implements DependencyModule {

    @Override
    public void configure(final DependencyService.Binder binder) {
        binder.bindConstructor(InetAddressInfoProvider.class, InetAddressInfoProvider.class);
        binder.bindConstructor(InetAddressWhitelist.class, InetAddressWhitelist.class);
    }
}
