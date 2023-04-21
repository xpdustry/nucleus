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

import fr.xpdustry.nucleus.api.application.NucleusClasspath;
import fr.xpdustry.nucleus.api.application.NucleusInjector;
import fr.xpdustry.nucleus.api.application.NucleusListener;
import fr.xpdustry.nucleus.common.NucleusCommonModule;
import fr.xpdustry.nucleus.common.application.AbstractNucleusApplication;
import fr.xpdustry.nucleus.common.application.SimpleNucleusInjector;
import fr.xpdustry.nucleus.discord.interaction.InteractionListener;
import fr.xpdustry.nucleus.discord.interaction.InteractionManager;
import org.slf4j.Logger;

public final class NucleusDiscordApplication extends AbstractNucleusApplication {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(NucleusDiscordApplication.class);

    private NucleusDiscordApplication() {}

    public static void main(final String[] args) {
        logger.info("Hello world! Nucleus is initializing...");

        final NucleusDiscordApplication application = new NucleusDiscordApplication();
        final NucleusInjector injector =
                new SimpleNucleusInjector(application, new NucleusCommonModule(), new NucleusDiscordModule());
        final var scanner = injector.getInstance(NucleusClasspath.class);

        logger.info("Registering listeners...");
        scanner.getAnnotatedListeners(NucleusListener.class).forEach(clazz -> {
            logger.info("> Listener {}", clazz.getSimpleName());
            application.register(injector.getInstance(clazz));
        });

        final var interactions = injector.getInstance(InteractionManager.class);
        logger.info("Registering interactions...");
        scanner.getAnnotatedListeners(InteractionListener.class).forEach(clazz -> {
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
}
