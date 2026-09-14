// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.message;

import com.xpdustry.nucleus.dependency.DependencyModule;
import com.xpdustry.nucleus.dependency.DependencyService;

public final class MessageModule implements DependencyModule {

    @Override
    public void configure(final DependencyService.Binder binder) {
        binder.bindConstructor(MessagePublisher.class, MessagePublisher.class);
    }
}
