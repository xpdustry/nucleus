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
package fr.xpdustry.nucleus.common;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import fr.xpdustry.nucleus.api.application.ClasspathScanner;
import fr.xpdustry.nucleus.api.database.DatabaseService;
import fr.xpdustry.nucleus.api.database.ObjectIdentifierGenerator;
import fr.xpdustry.nucleus.api.event.EventService;
import fr.xpdustry.nucleus.api.hash.HashFunction;
import fr.xpdustry.nucleus.api.network.DiscoveryService;
import fr.xpdustry.nucleus.api.network.VpnDetector;
import fr.xpdustry.nucleus.api.translation.TranslationService;
import fr.xpdustry.nucleus.common.application.SimpleClasspathScanner;
import fr.xpdustry.nucleus.common.configuration.ConfigurationFactory;
import fr.xpdustry.nucleus.common.configuration.NoopNucleusConfigurationUpgrader;
import fr.xpdustry.nucleus.common.configuration.NucleusConfiguration;
import fr.xpdustry.nucleus.common.configuration.NucleusConfigurationUpgrader;
import fr.xpdustry.nucleus.common.configuration.SimpleConfigurationFactory;
import fr.xpdustry.nucleus.common.database.mongo.MongoDatabaseService;
import fr.xpdustry.nucleus.common.database.mongo.MongoObjectIdentifierGenerator;
import fr.xpdustry.nucleus.common.event.KyoriEventService;
import fr.xpdustry.nucleus.common.network.ListeningDiscoveryService;
import fr.xpdustry.nucleus.common.network.VpnApiIoDetector;
import fr.xpdustry.nucleus.common.security.BcryptHashFunction;
import fr.xpdustry.nucleus.common.translation.TranslationServiceProvider;
import javax.inject.Singleton;

public final class NucleusCommonModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(DatabaseService.class).to(MongoDatabaseService.class).in(Singleton.class);
        bind(ObjectIdentifierGenerator.class)
                .to(MongoObjectIdentifierGenerator.class)
                .in(Singleton.class);
        bind(HashFunction.class).to(BcryptHashFunction.class).in(Singleton.class);
        bind(DiscoveryService.class).to(ListeningDiscoveryService.class).in(Singleton.class);
        bind(TranslationService.class)
                .toProvider(TranslationServiceProvider.class)
                .in(Singleton.class);
        bind(VpnDetector.class).to(VpnApiIoDetector.class).in(Singleton.class);
        bind(NucleusConfigurationUpgrader.class)
                .to(NoopNucleusConfigurationUpgrader.class)
                .in(Singleton.class);
        bind(ConfigurationFactory.class).to(SimpleConfigurationFactory.class).in(Singleton.class);
        bind(EventService.class).to(KyoriEventService.class).in(Singleton.class);
        bind(ClasspathScanner.class).to(SimpleClasspathScanner.class);
    }

    @Provides
    @Singleton
    public NucleusConfiguration provideConfiguration(final ConfigurationFactory factory) {
        return factory.create(NucleusConfiguration.class);
    }
}
