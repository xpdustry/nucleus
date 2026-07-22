// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.gatekeeper

import com.xpdustry.foundation.player.MUUID
import java.net.InetAddress

data class GatekeeperContext(val name: String, val muuid: MUUID, val address: InetAddress)
