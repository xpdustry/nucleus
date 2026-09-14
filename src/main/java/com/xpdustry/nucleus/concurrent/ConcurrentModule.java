// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.concurrent;

import com.xpdustry.nucleus.dependency.DependencyModule;
import com.xpdustry.nucleus.dependency.DependencyService;
import java.util.concurrent.Executor;

public class ConcurrentModule implements DependencyModule {
    @Override
    public void configure(final DependencyService.Binder binder) {
        binder.bindConstructor(AsyncAwareScheduledTaskHandlerProcessor.class);
        binder.bindConstructor(Executor.class, NucleusExecutor.class);
    }
}
