// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.metric

fun interface MetricCollector {
    fun flush(sink: MetricSink)
}
