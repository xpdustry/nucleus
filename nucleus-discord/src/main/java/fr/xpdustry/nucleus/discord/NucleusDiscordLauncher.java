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

import fr.xpdustry.nucleus.api.application.ClasspathScanner;
import fr.xpdustry.nucleus.api.application.NucleusInjector;
import fr.xpdustry.nucleus.api.application.ShutdownEvent;
import fr.xpdustry.nucleus.api.application.lifecycle.LifecycleListener;
import fr.xpdustry.nucleus.api.event.EventService;
import fr.xpdustry.nucleus.common.NucleusCommonModule;
import fr.xpdustry.nucleus.common.lifecycle.SimpleLifecycleListenerRepository;
import fr.xpdustry.nucleus.common.lifecycle.SimpleNucleusInjector;
import fr.xpdustry.nucleus.discord.interaction.InteractionListener;
import fr.xpdustry.nucleus.discord.interaction.InteractionManager;
import org.slf4j.Logger;

public final class NucleusDiscordLauncher {

    private NucleusDiscordLauncher() {}

    public static void main(final String[] args) {
        final SimpleLifecycleListenerRepository repository = new SimpleLifecycleListenerRepository();
        final NucleusInjector injector =
                new SimpleNucleusInjector(repository, new NucleusCommonModule(), new NucleusDiscordModule());

        final var interactions = injector.getInstance(InteractionManager.class);
        final var events = injector.getInstance(EventService.class);
        final var logger = injector.getInstance(Logger.class);
        final var scanner = injector.getInstance(ClasspathScanner.class);

        logger.info("Hello world! Nucleus is initializing...");

        logger.info("Registering listeners...");
        scanner.getAnnotatedListeners(LifecycleListener.class).forEach(clazz -> {
            logger.info("> Listener {}", clazz.getSimpleName());
            repository.register(injector.getInstance(clazz));
        });

        logger.info("Registering interactions...");
        scanner.getAnnotatedListeners(InteractionListener.class).forEach(clazz -> {
            logger.info("> Interaction {}", clazz.getSimpleName());
            interactions.register(injector.getInstance(clazz));
        });

        repository.initAll();
        logger.info("Nucleus is ready!");

        events.subscribe(ShutdownEvent.class, event -> {
            logger.info("Shutting down the bot...");
            repository.exitAll();
            logger.info("Nucleus Bot has been shut down.");
            switch (event.getCause()) {
                case EXIT -> System.exit(0);
                case RESTART -> System.exit(2);
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(repository::exitAll));
    }
}
