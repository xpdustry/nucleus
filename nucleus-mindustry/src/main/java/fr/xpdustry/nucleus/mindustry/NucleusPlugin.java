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
package fr.xpdustry.nucleus.mindustry;

import arc.util.CommandHandler;
import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.distributor.api.command.ArcCommandManager;
import fr.xpdustry.distributor.api.command.sender.CommandSender;
import fr.xpdustry.distributor.api.event.EventBus;
import fr.xpdustry.distributor.api.event.EventBusListener;
import fr.xpdustry.distributor.api.event.EventHandler;
import fr.xpdustry.distributor.api.plugin.ExtendedPlugin;
import fr.xpdustry.javelin.JavelinPlugin;
import fr.xpdustry.javelin.JavelinSocket;
import fr.xpdustry.nucleus.common.NucleusApplication;
import fr.xpdustry.nucleus.common.NucleusApplicationProvider;
import fr.xpdustry.nucleus.common.event.ImmutablePlayerJoinEvent;
import fr.xpdustry.nucleus.common.event.ImmutablePlayerMessageEvent;
import fr.xpdustry.nucleus.common.event.ImmutablePlayerQuitEvent;
import fr.xpdustry.nucleus.common.event.PlayerMessageEvent;
import fr.xpdustry.nucleus.common.util.NucleusPlatform;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.net.Administration;

public final class NucleusPlugin extends ExtendedPlugin implements NucleusApplication, EventBusListener {

    private final ArcCommandManager<CommandSender> clientCommands = ArcCommandManager.standard(this);
    private final ArcCommandManager<CommandSender> serverCommands = ArcCommandManager.standard(this);

    private final Administration.Config discordChannelName = new Administration.Config(
            "nucleus:discord-channel-name", "The name of the linked discord channel.", "unknown");

    @Override
    public void onInit() {
        NucleusApplicationProvider.set(this);
        EventBus.mindustry().register(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onLoad() {
        this.getSocket().subscribe(PlayerMessageEvent.class, event -> {
            if (discordChannelName.string().equals(event.getServerName())) {
                /*
                TODO
                 1. Add discord tag
                 2. Update Flex plugin to have a proper chat formatting library
                */
                Call.sendMessage(
                        "[coral][[[orange]" + event.getPlayerName() + "[coral]]:[white] " + event.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(final EventType.PlayerJoin event) {
        this.getSocket()
                .sendEvent(ImmutablePlayerJoinEvent.builder()
                        .playerName(event.player.plainName())
                        .serverName(this.discordChannelName.string())
                        .build());
    }

    @EventHandler
    public void onPlayerMessage(final EventType.PlayerChatEvent event) {
        this.getSocket()
                .sendEvent(ImmutablePlayerMessageEvent.builder()
                        .playerName(event.player.plainName())
                        .serverName(this.discordChannelName.string())
                        .message(event.message)
                        .build());
    }

    @EventHandler
    public void onPlayerQuit(final EventType.PlayerLeave event) {
        this.getSocket()
                .sendEvent(ImmutablePlayerQuitEvent.builder()
                        .playerName(event.player.plainName())
                        .serverName(this.discordChannelName.string())
                        .build());
    }

    @Override
    public void onClientCommandsRegistration(final CommandHandler handler) {
        this.clientCommands.initialize(handler);
        this.clientCommands.command(this.clientCommands
                .commandBuilder("discord")
                .meta(CommandMeta.DESCRIPTION, "Send you our discord invitation link.")
                .handler(ctx -> {
                    Call.openURI(ctx.getSender().getPlayer().con(), "https://discord.xpdustry.fr");
                }));
    }

    @Override
    public JavelinSocket getSocket() {
        return JavelinPlugin.getJavelinSocket();
    }

    @Override
    public NucleusPlatform getPlatform() {
        return NucleusPlatform.MINDUSTRY;
    }
}
