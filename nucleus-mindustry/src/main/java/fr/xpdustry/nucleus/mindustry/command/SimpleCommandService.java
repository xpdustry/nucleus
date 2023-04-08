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
package fr.xpdustry.nucleus.mindustry.command;

import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.nucleus.api.application.lifecycle.LifecycleListener;
import javax.inject.Inject;
import mindustry.Vars;
import mindustry.server.ServerControl;

public final class SimpleCommandService implements CommandService, LifecycleListener {

    private final NucleusPluginCommandManager serverCommands;
    private final NucleusPluginCommandManager clientCommands;

    @Inject
    public SimpleCommandService(final MindustryPlugin plugin) {
        this.serverCommands = new NucleusPluginCommandManager(plugin);
        this.clientCommands = new NucleusPluginCommandManager(plugin);
    }

    @Override
    public void onLifecycleInit() {
        this.serverCommands.initialize(ServerControl.instance.handler);
        this.clientCommands.initialize(Vars.netServer.clientCommands);
    }

    @Override
    public NucleusPluginCommandManager getServerCommandManager() {
        return this.serverCommands;
    }

    @Override
    public NucleusPluginCommandManager getClientCommandManager() {
        return this.clientCommands;
    }
}
