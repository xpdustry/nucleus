// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.database;

import com.xpdustry.nucleus.dependency.Module;
import com.xpdustry.nucleus.dependency.Named;
import com.xpdustry.nucleus.dependency.Provider;
import java.nio.file.Path;

public final class DatabaseModule implements Module {

    @Provider
    SQLDatabase createDatabase(final SQLDatabaseConfig config, final @Named("directory") Path directory) {
        return new SQLDatabaseImpl(config, directory);
    }
}
