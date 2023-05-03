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

import fr.xpdustry.nucleus.common.application.NucleusApplication;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import javax.inject.Inject;
import org.aeonbits.owner.ConfigFactory;

public final class SimpleConfigurationFactory implements ConfigurationFactory {

    private final NucleusConfigurationUpgrader upgrader;
    private final Path configPath;

    @Inject
    public SimpleConfigurationFactory(
            final NucleusConfigurationUpgrader upgrader, final NucleusApplication application) {
        this.upgrader = upgrader;
        this.configPath = application.getDataDirectory().resolve("config.properties");
    }

    @Override
    public <C extends NucleusConfiguration> C create(final Class<C> type) {
        if (!Files.exists(configPath)) {
            return ConfigFactory.create(type);
        }
        final var properties = new Properties();
        try (final var reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to load configuration file", e);
        }
        this.upgrader.upgrade(properties);
        try (final var writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            properties.store(writer, null);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to save configuration file", e);
        }
        return ConfigFactory.create(type, properties);
    }
}
