// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.database;

import java.sql.SQLException;

@FunctionalInterface
public interface SQLBiConsumer<V1, V2> {
    void accept(final V1 value1, final V2 value2) throws SQLException;
}
