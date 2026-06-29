// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus;

import com.xpdustry.foundation.plugin.BaseMindustryPlugin;
import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.nucleus.config.ConfigModule;
import com.xpdustry.nucleus.database.DatabaseModule;
import com.xpdustry.nucleus.dependency.DependencyService;
import com.xpdustry.nucleus.dependency.Module;
import com.xpdustry.nucleus.network.NetworkModule;
import java.nio.file.Path;

public final class NucleusPlugin extends BaseMindustryPlugin {

    public final FoundationAPIFromNucleus foundation = new FoundationAPIFromNucleus(this);

    private final DependencyService dependencies =
            new DependencyService(new ConfigModule(), new PluginModule(), new DatabaseModule(), new NetworkModule());

    @Override
    public void onInit() {
        this.logger().info("Hello world");
        for (final var resolved : this.dependencies.resolveAll()) {
            if (resolved instanceof PluginListener listener) {
                this.addListener(listener);
            }
        }
    }

    private final class PluginModule implements Module {
        @Override
        public void configure(final DependencyService.Binder binder) {
            binder.bindInstance(Path.class, "home", NucleusPlugin.this.directory());
        }
    }
}
