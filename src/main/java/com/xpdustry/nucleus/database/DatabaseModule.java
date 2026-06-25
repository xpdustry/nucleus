// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.database;

import com.xpdustry.nucleus.dependency.DependencyService;
import com.xpdustry.nucleus.dependency.Module;

public final class DatabaseModule implements Module {

    @Override
    public void configure(final DependencyService.Binder binder) {
        binder.bindConstructor(SQLDatabase.class, SQLDatabaseImpl.class);
    }
}
