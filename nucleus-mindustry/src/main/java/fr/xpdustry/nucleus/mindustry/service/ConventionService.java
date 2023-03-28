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
package fr.xpdustry.nucleus.mindustry.service;

import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.distributor.api.scheduler.MindustryTimeUnit;
import fr.xpdustry.distributor.api.scheduler.TaskHandler;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import java.util.Random;
import mindustry.net.Administration;

public final class ConventionService implements PluginListener {

    private final Random random = new Random();
    private final NucleusPlugin nucleus;

    public ConventionService(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public void onPluginInit() {
        Administration.Config.serverName.set("[cyan]<[white] Xpdustry [cyan]\uF821[white] "
                + nucleus.getConfiguration().getServerDisplayName() + " [cyan]>[white]");

        Administration.Config.motd.set(
                "[cyan]>>>[] Bienvenue sur [cyan]Xpdustry[], le seul serveur mindustry français. N'hésitez pas à nous rejoindre sur Discord avec la commande [cyan]/discord[].");
    }

    @TaskHandler(interval = 1L, unit = MindustryTimeUnit.MINUTES)
    public void onQuoteUpdate() {
        final var quote = this.nucleus
                .getConfiguration()
                .getQuotes()
                .get(random.nextInt(this.nucleus.getConfiguration().getQuotes().size()));
        Administration.Config.desc.set("\"" + quote + "\" [white]https://discord.xpdustry.fr");
    }
}
