// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.gatekeeper;

import com.xpdustry.foundation.player.MUUID;
import java.net.InetAddress;

public record GatekeeperContext(String name, MUUID muuid, InetAddress address) {}
