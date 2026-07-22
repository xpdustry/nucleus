// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.concurrent

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory

object NucleusExecutors {
    fun newVirtualThreadFactory(name: String): ThreadFactory {
        return Thread.ofVirtual().name("nucleus-" + name + "-", 0).factory()
    }

    fun newVirtualThreadPerTaskExecutor(name: String): ExecutorService {
        return Executors.newThreadPerTaskExecutor(newVirtualThreadFactory(name))
    }

    fun newSingleThreadScheduledExecutor(name: String): ScheduledExecutorService {
        return Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("nucleus-" + name).factory())
    }
}
