// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.metric

interface MetricSink {
    fun sample(name: String, type: MetricType, value: Number) {
        this.sample(name, type, value, emptyMap())
    }

    fun sample(name: String, type: MetricType, value: Number, labels: Map<String, String>)
}
