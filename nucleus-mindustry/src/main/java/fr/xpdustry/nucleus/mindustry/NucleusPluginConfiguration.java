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

import java.util.List;
import org.aeonbits.owner.Config;

@Config.Sources("file:${plugin-directory}/config.properties")
public interface NucleusPluginConfiguration extends Config {

    @Config.DefaultValue("unknown")
    @Config.Key("fr.xpdustry.nucleus.mindustry.server-name")
    String getServerName();

    @Config.DefaultValue("Unknown")
    @Config.Key("fr.xpdustry.nucleus.mindustry.server-display-name")
    String getServerDisplayName();

    @Config.DefaultValue("Bonjour")
    @Config.Key("fr.xpdustry.nucleus.mindustry.quotes")
    @Config.Separator(";")
    List<String> getQuotes();

    @Config.DefaultValue("https://translate.xpdustry.fr")
    @Config.Key("fr.xpdustry.nucleus.mindustry.translation.endpoint")
    String getTranslationEndpoint();

    @Config.DefaultValue("no")
    @Config.Key("fr.xpdustry.nucleus.mindustry.translation.token")
    String getTranslationToken();
}
