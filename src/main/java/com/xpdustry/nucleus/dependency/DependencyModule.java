// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.dependency;

public interface DependencyModule {

    void configure(final DependencyService.Binder binder);
}
