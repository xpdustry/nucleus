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
package fr.xpdustry.nucleus.discord;

import fr.xpdustry.nucleus.core.NucleusConfiguration;
import java.util.List;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.LoadType;

@Config.Sources({"file:nucleus.properties", "classpath:nucleus.properties"})
@Config.LoadPolicy(LoadType.FIRST)
public interface NucleusBotConfiguration extends NucleusConfiguration {

    @Config.Key("fr.xpdustry.nucleus.discord.token")
    String getToken();

    @Config.Key("fr.xpdustry.nucleus.discord.channel.report")
    long getReportChannel();

    @Config.Key("fr.xpdustry.nucleus.discord.channel.system")
    long getSystemChannel();

    @Config.Key("fr.xpdustry.nucleus.discord.category.servers")
    long getServerCategory();

    @Config.Key("fr.xpdustry.nucleus.discord.javelin.port")
    @Config.DefaultValue("12000")
    int getJavelinPort();

    @Config.Key("fr.xpdustry.nucleus.discord.javelin.workers")
    @Config.DefaultValue("4")
    int getJavelinWorkers();

    @Config.Key("fr.xpdustry.nucleus.discord.owners")
    List<Long> getOwners();
}
