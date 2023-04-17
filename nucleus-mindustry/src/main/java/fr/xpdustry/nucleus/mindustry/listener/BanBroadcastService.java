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

import arc.Core;
import fr.xpdustry.nucleus.api.annotation.NucleusAutoListener;
import fr.xpdustry.nucleus.api.application.lifecycle.LifecycleListener;
import fr.xpdustry.nucleus.api.message.MessageService;
import fr.xpdustry.nucleus.api.moderation.ModerationActionMessage;
import javax.inject.Inject;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Groups;

@NucleusAutoListener
public final class BanBroadcastService implements LifecycleListener {

    private static final String MESSAGE_TEMPLATE =
            """
            You were [scarlet]%s[] by [orange]%s[] because of [red]'%s'[].
            If you think this is a mistake, please contact a moderator on our discord server [cyan]https://discord.xpdstry.fr[].""";

    private final MessageService messageService;

    @Inject
    public BanBroadcastService(final MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void onLifecycleInit() {
        // TODO Add random messages
        this.messageService.subscribe(ModerationActionMessage.class, event -> {
            if (event.getType() == ModerationActionMessage.Type.BAN) {
                Vars.netServer.admins.banPlayerID(event.getTargetUuid());
                Vars.netServer.admins.banPlayerIP(event.getTargetIp());
                Groups.player.each(
                        player -> player.uuid().equals(event.getTargetUuid())
                                || player.ip().equals(event.getTargetIp()),
                        player -> {
                            player.kick(MESSAGE_TEMPLATE.formatted("banned", event.getAuthor(), event.getReason()));
                            Core.app.post(() -> Call.sendMessage("[scarlet]" + player.plainName()
                                    + " has been thanos snapped by " + event.getAuthor()));
                        });
            } else {
                Vars.netServer.admins.handleKicked(event.getTargetUuid(), event.getTargetIp(), 30 * 60 * 1000);
                Groups.player.each(
                        player -> player.uuid().equals(event.getTargetUuid())
                                || player.ip().equals(event.getTargetIp()),
                        player -> {
                            player.kick(MESSAGE_TEMPLATE.formatted("kicked", event.getAuthor(), event.getReason()));
                            Core.app.post(() -> Call.sendMessage(
                                    "[scarlet]" + player.plainName() + " has been kicked by " + event.getAuthor()));
                        });
            }
        });
    }
}
