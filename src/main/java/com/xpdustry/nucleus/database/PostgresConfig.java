// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.database;

import com.xpdustry.nucleus.util.Secret;

public record PostgresConfig(String host, int port, String database, String username, Secret password, boolean local) {}
