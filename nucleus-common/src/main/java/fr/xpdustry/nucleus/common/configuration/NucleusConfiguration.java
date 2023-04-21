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
package fr.xpdustry.nucleus.common.configuration;

import org.aeonbits.owner.Config;

public interface NucleusConfiguration extends Config {

    @Config.Key("fr.xpdustry.nucleus.network.vpn-api-io.token")
    @Config.DefaultValue("")
    String getVpnApiIoToken();

    @Config.Key("fr.xpdustry.nucleus.auto-update.enabled")
    @Config.DefaultValue("true")
    boolean isAutoUpdateEnabled();

    @Config.Key("fr.xpdustry.nucleus.auto-update.interval")
    @Config.DefaultValue("900")
    int getAutoUpdateInterval();

    @Config.Key("fr.xpdustry.nucleus.translation.deepl.token")
    @Config.DefaultValue("")
    String getDeeplTranslationToken();

    @Config.Key("fr.xpdustry.nucleus.data.mongodb.host")
    @Config.DefaultValue("")
    String getMongoHost();

    @Config.Key("fr.xpdustry.nucleus.data.mongodb.port")
    @Config.DefaultValue("27017")
    int getMongoPort();

    @Config.Key("fr.xpdustry.nucleus.data.mongodb.username")
    @Config.DefaultValue("")
    String getMongoUsername();

    @Config.Key("fr.xpdustry.nucleus.data.mongodb.password")
    @Config.DefaultValue("")
    String getMongoPassword();

    @Config.Key("fr.xpdustry.nucleus.data.mongodb.auth-database")
    @Config.DefaultValue("admin")
    String getMongoAuthDatabase();

    @Config.Key("fr.xpdustry.nucleus.data.mongodb.ssl")
    @Config.DefaultValue("true")
    boolean isMongoConnectionSsl();

    @Config.Key("fr.xpdustry.nucleus.data.mongodb.database")
    @Config.DefaultValue("nucleus")
    String getMongoDatabase();
}
