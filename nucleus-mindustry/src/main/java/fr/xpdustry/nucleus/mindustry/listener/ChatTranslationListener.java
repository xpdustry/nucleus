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
import fr.xpdustry.nucleus.common.inject.EnableScanning;
import fr.xpdustry.nucleus.common.translation.TranslationService;
import fr.xpdustry.nucleus.mindustry.chat.ChatManager;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import mindustry.game.EventType;
import org.slf4j.Logger;

@EnableScanning
public final class ChatTranslationListener implements NucleusListener {

    private final ChatManager chatManager;
    private final TranslationService translationService;

    @Inject
    private Logger logger;

    @Inject
    public ChatTranslationListener(final ChatManager chatManager, TranslationService translationService) {
        this.chatManager = chatManager;
        this.translationService = translationService;
    }

    @Override
    public void onNucleusInit() {
        this.chatManager.addProcessor((source, message, target) -> {
            var sourceLocale = Players.getLocale(source);
            var targetLocale = Players.getLocale(target);

            try {
                final var sourceText = Strings.stripColors(message);
                final var targetText = this.translationService
                        .translate(Strings.stripColors(message), sourceLocale, targetLocale)
                        .orTimeout(3L, TimeUnit.SECONDS)
                        .join();
                if (sourceText.equals(targetText)) {
                    return message;
                }
                return String.format("%s [lightgray](%s)", message, targetText);
            } catch (final Exception exception) {
                this.logger
                        .atTrace()
                        .setMessage("Failed to translate the message '{}' from {} to {}")
                        .addArgument(message)
                        .addArgument(sourceLocale)
                        .addArgument(targetLocale)
                        .setCause(exception)
                        .log();
                return message;
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(final EventType.PlayerJoin event) {
        if (this.translationService.isSupportedLanguage(Players.getLocale(event.player))) {
            event.player.sendMessage("[green]This server supports chat auto-translation for your language!");
        } else {
            event.player.sendMessage(
                    "[scarlet]Warning, your language is not supported by the chat auto-translator. Please talk in english.");
        }
    }
}
