// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.database;

// TODO Wrap password in Secret
public record SQLDatabaseConfig(
        String host, String port, String database, String username, String password, boolean local) {}
