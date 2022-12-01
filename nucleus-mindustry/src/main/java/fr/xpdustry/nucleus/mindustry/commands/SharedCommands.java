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
package fr.xpdustry.nucleus.mindustry.commands;

import arc.util.CommandHandler;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import fr.xpdustry.nucleus.mindustry.internal.NucleusPluginCommandManager;

public final class SharedCommands implements PluginListener {

    private final NucleusPlugin nucleus;

    public SharedCommands(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public void onPluginServerCommandsRegistration(final CommandHandler handler) {
        this.onPluginSharedCommandsRegistration(this.nucleus.getServerCommands());
    }

    @Override
    public void onPluginClientCommandsRegistration(final CommandHandler handler) {
        this.onPluginSharedCommandsRegistration(this.nucleus.getClientCommands());
    }

    private void onPluginSharedCommandsRegistration(final NucleusPluginCommandManager manager) {
        // Empty, what a shame
    }
}
