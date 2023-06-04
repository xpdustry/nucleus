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
import fr.xpdustry.nucleus.common.annotation.NucleusExecutor;
import fr.xpdustry.nucleus.common.configuration.ConfigurationFactory;
import fr.xpdustry.nucleus.common.configuration.NoopNucleusConfigurationUpgrader;
import fr.xpdustry.nucleus.common.configuration.NucleusConfiguration;
import fr.xpdustry.nucleus.common.configuration.NucleusConfigurationUpgrader;
import fr.xpdustry.nucleus.common.configuration.SimpleConfigurationFactory;
import fr.xpdustry.nucleus.common.database.DatabaseService;
import fr.xpdustry.nucleus.common.database.mongo.MongoDatabaseService;
import fr.xpdustry.nucleus.common.hash.BcryptHashFunction;
import fr.xpdustry.nucleus.common.hash.HashFunction;
import fr.xpdustry.nucleus.common.network.DiscoveryService;
import fr.xpdustry.nucleus.common.network.IpHubVpnDetector;
import fr.xpdustry.nucleus.common.network.IpQualityScoreVpnDetector;
import fr.xpdustry.nucleus.common.network.ListeningDiscoveryService;
import fr.xpdustry.nucleus.common.network.RollingVpnDetector;
import fr.xpdustry.nucleus.common.network.VpnApiIoDetector;
import fr.xpdustry.nucleus.common.network.VpnDetector;
import fr.xpdustry.nucleus.common.translation.TranslationService;
import fr.xpdustry.nucleus.common.translation.TranslationServiceProvider;
import fr.xpdustry.nucleus.common.version.SimpleUpdateService;
import fr.xpdustry.nucleus.common.version.UpdateService;
import java.util.concurrent.Executor;
import javax.inject.Singleton;

public final class NucleusCommonModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(DatabaseService.class).to(MongoDatabaseService.class).in(Singleton.class);
        bind(HashFunction.class).to(BcryptHashFunction.class).in(Singleton.class);
        bind(DiscoveryService.class).to(ListeningDiscoveryService.class).in(Singleton.class);
        bind(TranslationService.class)
                .toProvider(TranslationServiceProvider.class)
                .in(Singleton.class);
        bind(NucleusConfigurationUpgrader.class)
                .to(NoopNucleusConfigurationUpgrader.class)
                .in(Singleton.class);
        bind(ConfigurationFactory.class).to(SimpleConfigurationFactory.class).in(Singleton.class);
        bind(UpdateService.class).to(SimpleUpdateService.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    public NucleusConfiguration provideConfiguration(final ConfigurationFactory factory) {
        return factory.create(NucleusConfiguration.class);
    }

    @Provides
    @Singleton
    public VpnDetector provideVpnDetector(
            final NucleusConfiguration configuration, final @NucleusExecutor Executor executor) {
        return new RollingVpnDetector(
                new IpHubVpnDetector(configuration, executor),
                new VpnApiIoDetector(configuration, executor),
                new IpQualityScoreVpnDetector(configuration, executor));
    }
}
