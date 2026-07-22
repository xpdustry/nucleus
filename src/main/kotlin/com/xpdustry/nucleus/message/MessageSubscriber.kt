// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.message

fun interface MessageSubscriber<E> {
    fun onMessage(sender: String, event: E)
}
