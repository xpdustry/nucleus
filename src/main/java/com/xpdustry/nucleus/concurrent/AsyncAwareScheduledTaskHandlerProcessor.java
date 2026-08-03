// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.concurrent;

import com.xpdustry.foundation.FoundationAPI;
import com.xpdustry.foundation.annotation.ScheduledTaskHandler;
import com.xpdustry.foundation.annotation.ScheduledTaskHandlerProcessor;
import com.xpdustry.foundation.plugin.PluginFacade;
import com.xpdustry.foundation.scheduler.MindustryTask;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

public final class AsyncAwareScheduledTaskHandlerProcessor extends ScheduledTaskHandlerProcessor {

    private final Executor executor;

    public AsyncAwareScheduledTaskHandlerProcessor(final PluginFacade plugin, final Executor executor) {
        super(plugin, FoundationAPI.get().scheduler());
        this.executor = executor;
    }

    @Override
    protected MindustryTask createTask(
            final Object instance, final Method method, final ScheduledTaskHandler annotation) {
        if (method.isAnnotationPresent(Async.class)) {
            return this.scheduler
                    .newTaskBuilder(this.plugin)
                    .initialDelay(annotation.initialDelay(), annotation.unit())
                    .repeatWithDelay(annotation.delay(), annotation.unit())
                    .executor(this.executor)
                    .execute(new MethodTaskHandler(instance, method));
        } else {
            return super.createTask(instance, method, annotation);
        }
    }
}
