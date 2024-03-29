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

import arc.util.Strings;
import fr.xpdustry.distributor.api.event.EventHandler;
import fr.xpdustry.distributor.api.util.Players;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.common.application.NucleusPlatform;
import fr.xpdustry.nucleus.common.bridge.PlayerActionMessage;
import fr.xpdustry.nucleus.common.message.MessageService;
import fr.xpdustry.nucleus.common.translation.TranslationService;
import fr.xpdustry.nucleus.mindustry.NucleusPluginConfiguration;
import java.util.Locale;
import javax.inject.Inject;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Iconc;
import mindustry.gen.Player;

public final class DiscordChatBridgeListener implements NucleusListener {

    private final NucleusPluginConfiguration configuration;
    private final MessageService messageService;
    private final TranslationService translationService;

    @Inject
    public DiscordChatBridgeListener(
            final NucleusPluginConfiguration configuration,
            final MessageService messageService,
            final TranslationService translationService) {
        this.configuration = configuration;
        this.messageService = messageService;
        this.translationService = translationService;
    }

    @Override
    public void onNucleusInit() {
        this.messageService.subscribe(PlayerActionMessage.class, event -> {
            if (event.getOrigin() == NucleusPlatform.DISCORD
                    && this.configuration.getServerName().equals(event.getServerIdentifier())
                    && event.getType() == PlayerActionMessage.Type.CHAT) {
                Call.sendMessage("[coral][[[white]" + Iconc.discord + "[]][[[orange]" + event.getPlayerName()
                        + "[coral]]:[white] " + event.getMessage().orElseThrow());
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(final EventType.PlayerJoin event) {
        this.messageService.publish(PlayerActionMessage.builder()
                .setPlayerName(event.player.plainName())
                .setServerIdentifier(this.configuration.getServerName())
                .setOrigin(NucleusPlatform.MINDUSTRY)
                .setType(PlayerActionMessage.Type.JOIN)
                .build());
    }

    @EventHandler
    public void onPlayerChat(final EventType.PlayerChatEvent event) {
        if (event.message.startsWith("/")) {
            return;
        }
        final var rawMessage = Strings.stripColors(event.message);
        this.translationService
                .translate(rawMessage, Players.getLocale(event.player), Locale.ENGLISH)
                .exceptionally(throwable -> "")
                .thenAccept(translation -> this.sendChatMessage(
                        event.player, rawMessage + (translation.isEmpty() ? "" : " (" + translation + ")")));
    }

    @EventHandler
    public void onPlayerLeave(final EventType.PlayerLeave event) {
        this.messageService.publish(PlayerActionMessage.builder()
                .setPlayerName(event.player.plainName())
                .setServerIdentifier(this.configuration.getServerName())
                .setOrigin(NucleusPlatform.MINDUSTRY)
                .setType(PlayerActionMessage.Type.QUIT)
                .build());
    }

    private void sendChatMessage(final Player player, final String message) {
        this.messageService.publish(PlayerActionMessage.builder()
                .setPlayerName(player.plainName())
                .setServerIdentifier(this.configuration.getServerName())
                .setOrigin(NucleusPlatform.MINDUSTRY)
                .setType(PlayerActionMessage.Type.CHAT)
                .setMessage(message)
                .build());
    }
}
