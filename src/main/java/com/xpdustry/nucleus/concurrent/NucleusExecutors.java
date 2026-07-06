// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public final class NucleusExecutors {

    public static ThreadFactory newVirtualThreadFactory(final String name) {
        return Thread.ofVirtual().name("nucleus-" + name + "-", 0).factory();
    }

    public static ExecutorService newVirtualThreadPerTaskExecutor(final String name) {
        return Executors.newThreadPerTaskExecutor(newVirtualThreadFactory(name));
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(final String name) {
        return Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().name("nucleus-" + name).factory());
    }

    private NucleusExecutors() {}
}
