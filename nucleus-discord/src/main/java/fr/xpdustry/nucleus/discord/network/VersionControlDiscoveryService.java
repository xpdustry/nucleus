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
package fr.xpdustry.nucleus.discord.network;

import fr.xpdustry.nucleus.common.annotation.NucleusExecutor;
import fr.xpdustry.nucleus.common.application.NucleusApplication;
import fr.xpdustry.nucleus.common.message.MessageService;
import fr.xpdustry.nucleus.common.network.DiscoveryMessage;
import fr.xpdustry.nucleus.common.network.ListeningDiscoveryService;
import fr.xpdustry.nucleus.common.version.UpdateMessage;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public final class VersionControlDiscoveryService extends ListeningDiscoveryService {

    private final NucleusApplication application;

    @Inject
    public VersionControlDiscoveryService(
            final MessageService messageService,
            final @NucleusExecutor Executor executor,
            final NucleusApplication application) {
        super(messageService, executor);
        this.application = application;
    }

    @Override
    protected void onServerDiscovered(final DiscoveryMessage message) {
        if (this.application.getVersion().isNewerThan(message.getNucleusVersion())) {
            logger.info("Server {} is outdated, sending update message.", message.getServerIdentifier());
            getMessageService().publish(UpdateMessage.of(this.application.getVersion()));
        }
    }
}
