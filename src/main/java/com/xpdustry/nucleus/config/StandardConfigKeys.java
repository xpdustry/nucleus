// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.config;

public final class StandardConfigKeys {

    public static final ConfigKey<String> NODE_NAME = new ConfigKey<>(
            String.class, "nucleus.node.name", "The name of this node", "unknown", ConfigKey.Flag.DYNAMIC);
}
