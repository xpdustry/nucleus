/*
 * Nucleus, the software collection powering Xpdustry.
 * Copyright (C) 2022  Xpdustry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.xpdustry.nucleus.mindustry;

import arc.ApplicationListener;
import arc.Core;
import arc.util.CommandHandler;
import fr.xpdustry.distributor.api.DistributorProvider;
import fr.xpdustry.distributor.api.plugin.AbstractMindustryPlugin;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.nucleus.common.NucleusCommonModule;
import fr.xpdustry.nucleus.common.application.AbstractNucleusApplication;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.common.application.NucleusPlatform;
import fr.xpdustry.nucleus.common.inject.ClasspathScanner;
import fr.xpdustry.nucleus.common.inject.NucleusInjector;
import fr.xpdustry.nucleus.common.inject.SimpleNucleusInjector;
import fr.xpdustry.nucleus.common.version.NucleusVersion;
import fr.xpdustry.nucleus.mindustry.command.NucleusPluginCommandManager;
import fr.xpdustry.nucleus.mindustry.listener.HubListener;
import java.nio.file.Path;
import mindustry.Vars;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class NucleusPlugin extends AbstractMindustryPlugin {

    private @MonotonicNonNull NucleusInjector injector = null;

    final NucleusPluginCommandManager serverCommands = new NucleusPluginCommandManager(this);
    final NucleusPluginCommandManager clientCommands = new NucleusPluginCommandManager(this);

    public NucleusInjector getInjector() {
        return injector;
    }

    @Override
    public void onServerCommandsRegistration(final CommandHandler handler) {
        serverCommands.initialize(handler);
    }

    @Override
    public void onClientCommandsRegistration(final CommandHandler handler) {
        clientCommands.initialize(handler);
    }

    @Override
    public void onLoad() {
        final var application = new NucleusMindustryApplication();
        this.addListener(application);

        this.injector =
                new SimpleNucleusInjector(application, new NucleusCommonModule(), new NucleusMindustryModule(this));

        final var scanner = this.injector.getInstance(ClasspathScanner.class);

        getLogger().info("Registering listeners...");
        scanner.findScanningEnabled(NucleusListener.class).forEach(clazz -> {
            this.getLogger().info("> Listener {}", clazz.getSimpleName());
            application.register(this.injector.getInstance(clazz));
        });

        if (this.injector.getInstance(NucleusPluginConfiguration.class).isHubEnabled()) {
            this.getLogger().info("Registering hub...");
            application.register(this.injector.getInstance(HubListener.class));
        }
    }

    private final class NucleusMindustryApplication extends AbstractNucleusApplication implements PluginListener {

        @Override
        public void onPluginLoad() {
            this.init();
        }

        @Override
        public void onPluginExit() {
            this.exit(Cause.SHUTDOWN);
        }

        @Override
        public void exit(final Cause cause) {
            super.exit(cause);
            Core.app.post(() -> {
                Core.app.exit();
                if (cause == Cause.RESTART) {
                    Core.app.addListener(new ApplicationListener() {
                        @Override
                        public void dispose() {
                            Core.settings.autosave();
                            System.exit(2);
                        }
                    });
                }
            });
        }

        @Override
        public NucleusVersion getVersion() {
            return NucleusVersion.parse(getDescriptor().getVersion());
        }

        @Override
        public NucleusPlatform getPlatform() {
            return NucleusPlatform.MINDUSTRY;
        }

        @Override
        public Path getDataDirectory() {
            return getDirectory();
        }

        @Override
        public Path getApplicationJar() {
            return Vars.mods.getMod(getDescriptor().getName()).file.file().toPath();
        }

        @Override
        protected void onRegister(final NucleusListener listener) {
            DistributorProvider.get().getPluginScheduler().parse(NucleusPlugin.this, listener);
            DistributorProvider.get().getEventBus().parse(NucleusPlugin.this, listener);
        }
    }
}
