// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.foundation.annotation;

import com.xpdustry.foundation.FoundationAPI;
import com.xpdustry.foundation.plugin.PluginFacade;
import com.xpdustry.foundation.scheduler.MindustryTask;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

final class TaskHandlerProcessor extends MethodAnnotationProcessor<TaskHandler, MindustryTask, MindustryTask> {

    private final PluginFacade plugin;

    TaskHandlerProcessor(final PluginFacade plugin) {
        super(TaskHandler.class);
        this.plugin = plugin;
    }

    @Override
    protected MindustryTask process(final Object instance, final Method method, final TaskHandler annotation) {
        if (method.getParameterCount() > 1) {
            throw new IllegalArgumentException("The task handler on " + method + " has the wrong parameter count.");
        } else if (method.getParameterCount() == 1 && !MindustryTask.class.equals(method.getParameterTypes()[0])) {
            throw new IllegalArgumentException("The task handler on " + method + " has the wrong parameter type.");
        }
        if (!method.canAccess(instance)) {
            method.setAccessible(true);
        }

        final var builder = FoundationAPI.get().scheduler().newTaskBuilder(this.plugin);
        if (annotation.delay() > -1) {
            builder.delay(annotation.delay(), annotation.unit());
        }
        if (annotation.repeat() > -1) {
            builder.repeat(annotation.repeat(), annotation.unit());
        }

        return builder.execute(new MethodTaskHandler(instance, method));
    }

    @Override
    protected Optional<MindustryTask> reduce(final List<MindustryTask> results) {
        return results.isEmpty() ? Optional.empty() : Optional.of(new CompositeTask(results));
    }

    private record MethodTaskHandler(Object object, Method method) implements Consumer<MindustryTask> {

        @Override
        public void accept(final MindustryTask task) {
            try {
                if (this.method.getParameterCount() == 1) {
                    this.method.invoke(this.object, task);
                } else {
                    this.method.invoke(this.object);
                }
            } catch (final IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Unable to invoke " + this.method, e);
            }
        }
    }

    private record CompositeTask(List<MindustryTask> tasks) implements MindustryTask {

        @Override
        public State state() {
            if (this.tasks.stream().anyMatch(task -> task.state() == State.SCHEDULED)) {
                return State.SCHEDULED;
            }
            return this.tasks.stream().allMatch(task -> task.state() == State.CANCELLED)
                    ? State.CANCELLED
                    : State.FINISHED;
        }

        @Override
        public void cancel() {
            this.tasks.forEach(MindustryTask::cancel);
        }
    }
}
