// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.database;

import java.sql.SQLException;

@FunctionalInterface
public interface SQLFunction<I, O> {
    O apply(final I input) throws SQLException;
}
