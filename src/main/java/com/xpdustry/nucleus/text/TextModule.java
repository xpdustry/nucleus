// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.text;

import com.xpdustry.nucleus.dependency.DependencyModule;
import com.xpdustry.nucleus.dependency.DependencyService;

public final class TextModule implements DependencyModule {

    @Override
    public void configure(final DependencyService.Binder binder) {
        binder.bindConstructor(BadWordFinder.class, BadWordFinder.class);
    }
}
