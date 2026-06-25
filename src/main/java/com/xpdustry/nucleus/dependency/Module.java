// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.dependency;

public interface Module {

    void configure(final DependencyService.Binder binder);
}
