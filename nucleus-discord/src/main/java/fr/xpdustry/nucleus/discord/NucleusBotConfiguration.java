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

import org.aeonbits.owner.Config;

@Config.Sources({"classpath:nucleus.properties", "file:nucleus.properties"})
@Config.LoadPolicy(Config.LoadType.MERGE)
public interface NucleusBotConfiguration extends Config {

    @Config.Key("fr.xpdustry.nucleus.discord.token")
    String getToken();

    @Config.Key("fr.xpdustry.nucleus.discord.channel.reports")
    long getReportChannel();

    @Config.Key("fr.xpdustry.nucleus.discord.category.servers")
    long getServerCategory();

    @Config.Key("fr.xpdustry.nucleus.discord.javelin.port")
    @Config.DefaultValue("12000")
    int getJavelinPort();

    @Config.Key("fr.xpdustry.nucleus.discord.javelin.workers")
    @Config.DefaultValue("4")
    int getJavelinWorkers();
}
