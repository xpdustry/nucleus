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
package fr.xpdustry.nucleus.mindustry.service;

import arc.Core;
import arc.util.Strings;
import fr.xpdustry.distributor.api.event.EventHandler;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.distributor.api.util.Players;
import fr.xpdustry.nucleus.core.translation.Translator;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import mindustry.game.EventType;

public final class ChatTranslationService implements PluginListener {

    private final NucleusPlugin nucleus;
    private final Translator translator;

    public ChatTranslationService(final NucleusPlugin nucleus, Translator translator) {
        this.nucleus = nucleus;
        this.translator = translator;
    }

    @EventHandler
    public void onPlayerJoin(final EventType.PlayerJoin event) {
        translator
                .isSupportedLanguage(Locale.forLanguageTag(event.player.locale().replace('_', '-')))
                .exceptionally(throwable -> {
                    this.nucleus
                            .getLogger()
                            .atDebug()
                            .setMessage("Failed to check language support for {}.")
                            .setCause(throwable)
                            .log();
                    return false;
                })
                .thenAcceptAsync(
                        supported -> {
                            if (!supported) {
                                event.player.sendMessage(
                                        "[scarlet]Warning, your language is not supported by the chat auto-translator. Please talk in english.");
                            } else {
                                event.player.sendMessage(
                                        "[green]This server supports chat auto-translation for your language!");
                            }
                        },
                        Core.app::post);
    }

    @Override
    public void onPluginInit() {
        this.nucleus.getChatManager().addProcessor((source, message, target) -> {
            var sourceLocale = Players.getLocale(source);
            var targetLocale = Players.getLocale(target);

            try {
                final var sourceText = Strings.stripColors(message);
                final var targetText = this.translator
                        .translate(Strings.stripColors(message), sourceLocale, targetLocale)
                        .orTimeout(3L, TimeUnit.SECONDS)
                        .join();
                if (sourceText.equals(targetText)) {
                    return message;
                }
                return String.format("%s [lightgray](%s)", message, targetText);
            } catch (final Exception exception) {
                this.nucleus
                        .getLogger()
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
}
