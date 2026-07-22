// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.gatekeeper

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface GatekeeperDecision {
    object Allow : GatekeeperDecision

    data class Kick(val reason: String, val duration: Duration = 5.seconds) : GatekeeperDecision
}
