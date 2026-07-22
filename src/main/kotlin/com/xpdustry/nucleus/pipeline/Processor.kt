// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.pipeline

fun interface Processor<I, O> {
    suspend fun process(context: I): O
}
