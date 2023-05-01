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
import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;

public final class SaveCommand implements PluginListener {

    private final NucleusPlugin nucleus;
    private final SaveInterface saveInterface;

    // TODO It would be nice to create a MapManager for map handling
    public SaveCommand(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
        this.saveInterface = new SaveInterface(nucleus);
    }

    @Override
    public void onPluginClientCommandsRegistration(final CommandHandler handler) {
        final var manager = this.nucleus.getClientCommands();
        manager.command(manager.commandBuilder("saves")
                .meta(CommandMeta.DESCRIPTION, "Opens the save menu.")
                .permission("nucleus.saves")
                .handler(ctx -> this.saveInterface.open(ctx.getSender().getPlayer())));
    }
}
