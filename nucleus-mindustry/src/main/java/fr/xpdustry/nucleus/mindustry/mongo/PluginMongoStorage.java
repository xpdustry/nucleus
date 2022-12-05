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
package fr.xpdustry.nucleus.mindustry.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.nucleus.common.model.PunishmentManager;
import fr.xpdustry.nucleus.common.model.UserManager;
import fr.xpdustry.nucleus.common.mongo.MongoStorage;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import fr.xpdustry.nucleus.mindustry.model.DocumentPunishmentManager;
import fr.xpdustry.nucleus.mindustry.model.DocumentUserManager;
import org.bson.codecs.configuration.CodecRegistries;

@SuppressWarnings("NullAway.Init")
public final class PluginMongoStorage implements MongoStorage, PluginListener {

    private final NucleusPlugin nucleus;
    private MongoClient client;
    private UserManager users;
    private PunishmentManager punishments;

    public PluginMongoStorage(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public UserManager getUserManager() {
        return users;
    }

    @Override
    public PunishmentManager getPunishmentManager() {
        return punishments;
    }

    @Override
    public void onPluginInit() {
        this.client = MongoClients.create(MongoClientSettings.builder()
                .codecRegistry(CodecRegistries.fromProviders(
                        MongoClientSettings.getDefaultCodecRegistry(), new MongoWrapperCodecProvider()))
                .applyConnectionString(
                        new ConnectionString(nucleus.getConfiguration().getMongoUri()))
                .serverApi(ServerApi.builder().version(ServerApiVersion.V1).build())
                .build());
        final var database = this.client.getDatabase(nucleus.getConfiguration().getMongoDatabase());
        this.users = new DocumentUserManager(database.getCollection("users"));
        this.punishments = new DocumentPunishmentManager(database.getCollection("punishments"), this.users);
    }

    @Override
    public void onPluginExit() {
        this.client.close();
    }
}
