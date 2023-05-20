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
package fr.xpdustry.nucleus.mindustry.listener;

import fr.xpdustry.distributor.api.DistributorProvider;
import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.distributor.api.scheduler.MindustryTimeUnit;
import fr.xpdustry.nucleus.common.application.NucleusApplication;
import fr.xpdustry.nucleus.common.application.NucleusApplication.Cause;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.common.message.MessageService;
import fr.xpdustry.nucleus.common.version.UpdateMessage;
import fr.xpdustry.nucleus.common.version.UpdateService;
import javax.inject.Inject;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UpdateListener implements NucleusListener {

    private static final Logger logger = LoggerFactory.getLogger(UpdateListener.class);

    private final MindustryPlugin plugin;
    private final NucleusApplication application;
    private final UpdateService updater;
    private final MessageService messenger;

    @Inject
    public UpdateListener(
            final MindustryPlugin plugin,
            final NucleusApplication application,
            final UpdateService updater,
            final MessageService messenger) {
        this.plugin = plugin;
        this.application = application;
        this.updater = updater;
        this.messenger = messenger;
    }

    @Override
    public void onNucleusInit() {
        this.messenger.subscribe(UpdateMessage.class, message -> {
            if (message.getVersion().isNewerThan(application.getVersion())) {
                logger.info("New version available {}, performing update.", message.getVersion());
                // TODO Create a failure system that sends error messages to Discord
                this.updater
                        .update(message.getVersion())
                        .thenRun(this::postUpdate)
                        .exceptionally(throwable -> {
                            logger.error("Failed to update to version {}.", message.getVersion(), throwable);
                            return null;
                        });
            }
        });
    }

    // TODO Find a way to update the server immediately, having servers with different versions creates too many issues
    private void postUpdate() {
        if (!Vars.state.isPlaying()) {
            this.application.exit(Cause.RESTART);
            return;
        }

        DistributorProvider.get()
                .getEventBus()
                .subscribe(
                        EventType.PlayerJoin.class,
                        plugin,
                        event -> event.player.sendMessage("[scarlet]The server will restart soon to update itself."));

        if (Vars.state.rules.tags.getBool("xpdustry-router:active")
                || Vars.state.rules.tags.getBool("xpdustry-hub:active")) {
            Call.sendMessage("[scarlet]The server will auto update itself in 10 minutes.");
            DistributorProvider.get()
                    .getPluginScheduler()
                    .scheduleAsync(plugin)
                    .delay(10L, MindustryTimeUnit.MINUTES)
                    .execute(() -> this.application.exit(Cause.RESTART));
        } else {
            Call.sendMessage("[scarlet]The server will auto update itself when the game is over.");
            DistributorProvider.get()
                    .getEventBus()
                    .subscribe(EventType.GameOverEvent.class, plugin, event -> DistributorProvider.get()
                            .getPluginScheduler()
                            .scheduleAsync(plugin)
                            .execute(() -> this.application.exit(Cause.RESTART)));
        }
    }
}
