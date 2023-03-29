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

import fr.xpdustry.nucleus.core.NucleusConfiguration;
import java.net.URL;
import java.util.List;
import org.aeonbits.owner.Config;

@Config.Sources("file:${plugin-directory}/config.properties")
public interface NucleusPluginConfiguration extends NucleusConfiguration {

    @Config.Key("fr.xpdustry.nucleus.mindustry.server-name")
    @Config.DefaultValue("unknown")
    String getServerName();

    @Config.Key("fr.xpdustry.nucleus.mindustry.server-display-name")
    @Config.DefaultValue("Unknown")
    String getServerDisplayName();

    @Config.DefaultValue("Bonjour")
    @Config.Key("fr.xpdustry.nucleus.mindustry.quotes")
    @Config.Separator(";")
    List<String> getQuotes();

    @Config.DefaultValue("10")
    @Config.Key("fr.xpdustry.nucleus.mindustry.inspector.limit")
    int getInspectorHistoryLimit();

    @Config.DefaultValue(
            "https://gist.githubusercontent.com/Phinner/720f0c15f2a828fa1aa11143bc3912c5/raw/e42c888cd7daa25cd48ff5b3790e2d1432240e9a/xpdustry-tips.yaml")
    @Config.Key("fr.xpdustry.nucleus.mindustry.tips.url")
    URL getTipsUrl();

    @Config.DefaultValue("false")
    @Config.Key("fr.xpdustry.nucleus.mindustry.hub.enabled")
    boolean isHubEnabled();
}
