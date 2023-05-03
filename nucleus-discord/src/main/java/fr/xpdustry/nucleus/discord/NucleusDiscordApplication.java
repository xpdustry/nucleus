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

import fr.xpdustry.nucleus.common.NucleusCommonModule;
import fr.xpdustry.nucleus.common.application.AbstractNucleusApplication;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.common.application.NucleusPlatform;
import fr.xpdustry.nucleus.common.inject.ClasspathScanner;
import fr.xpdustry.nucleus.common.inject.NucleusInjector;
import fr.xpdustry.nucleus.common.inject.SimpleNucleusInjector;
import fr.xpdustry.nucleus.common.version.NucleusVersion;
import fr.xpdustry.nucleus.discord.interaction.InteractionListener;
import fr.xpdustry.nucleus.discord.interaction.InteractionManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.slf4j.Logger;

public final class NucleusDiscordApplication extends AbstractNucleusApplication {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(NucleusDiscordApplication.class);

    private @MonotonicNonNull NucleusVersion version = null;
    private @MonotonicNonNull Path jarLocation = null;

    private NucleusDiscordApplication() {}

    public static void main(final String[] args) {
        logger.info("Hello world! Nucleus is initializing...");

        final NucleusDiscordApplication application = new NucleusDiscordApplication();
        final NucleusInjector injector =
                new SimpleNucleusInjector(application, new NucleusCommonModule(), new NucleusDiscordModule());
        final var scanner = injector.getInstance(ClasspathScanner.class);

        logger.info("Registering listeners...");
        scanner.findScanningEnabled(NucleusListener.class).forEach(clazz -> {
            logger.info("> Listener {}", clazz.getSimpleName());
            application.register(injector.getInstance(clazz));
        });

        final var interactions = injector.getInstance(InteractionManager.class);
        logger.info("Registering interactions...");
        scanner.findScanningEnabled(InteractionListener.class).forEach(clazz -> {
            logger.info("> Interaction {}", clazz.getSimpleName());
            interactions.register(injector.getInstance(clazz));
        });

        application.init();
    }

    @Override
    public void init() {
        super.init();
        logger.info("Nucleus is ready!");
    }

    @Override
    public void exit(final Cause cause) {
        logger.info("Shutting down the bot...");
        super.exit(cause);
        logger.info("Nucleus Bot has been shut down.");
        switch (cause) {
            case SHUTDOWN -> System.exit(0);
            case RESTART -> System.exit(2);
        }
    }

    @Override
    public NucleusVersion getVersion() {
        if (version != null) {
            return version;
        }
        final var stream = getClass().getClassLoader().getResourceAsStream("VERSION.txt");
        if (stream == null) {
            throw new IllegalStateException("Could not find VERSION.txt, using default version");
        }
        try (final var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return version = NucleusVersion.parse(reader.readLine());
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load version", e);
        }
    }

    @Override
    public NucleusPlatform getPlatform() {
        return NucleusPlatform.DISCORD;
    }

    @Override
    public Path getDataDirectory() {
        return Path.of(".");
    }

    @Override
    public Path getApplicationJar() {
        if (jarLocation != null) {
            return jarLocation;
        }
        final var codeSource = getClass().getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            throw new IllegalStateException("Could not find code source");
        }
        final var location = codeSource.getLocation();
        if (location == null) {
            throw new IllegalStateException("Could not find code source location");
        }
        return jarLocation = Path.of(location.getPath());
    }
}
