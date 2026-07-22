// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.network

@JvmRecord data class InetAddressInfo(val safe: Boolean, val country: String, val asnName: String, val asnNumber: Long)
