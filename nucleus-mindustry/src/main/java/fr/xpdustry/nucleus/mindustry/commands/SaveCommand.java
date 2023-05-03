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

import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.common.inject.EnableScanning;
import fr.xpdustry.nucleus.mindustry.annotation.ClientSide;
import fr.xpdustry.nucleus.mindustry.command.NucleusPluginCommandManager;
import javax.inject.Inject;

@EnableScanning
public final class SaveCommand implements NucleusListener {

    private final SaveInterface saveInterface;
    private final NucleusPluginCommandManager clientCommandManager;

    @Inject
    public SaveCommand(
            final MindustryPlugin plugin, final @ClientSide NucleusPluginCommandManager clientCommandManager) {
        this.saveInterface = new SaveInterface(plugin);
        this.clientCommandManager = clientCommandManager;
    }

    @Override
    public void onNucleusInit() {
        clientCommandManager.command(clientCommandManager
                .commandBuilder("saves")
                .meta(CommandMeta.DESCRIPTION, "Opens the save menu.")
                .permission("nucleus.saves")
                .handler(ctx -> this.saveInterface.open(ctx.getSender().getPlayer())));
    }
}
