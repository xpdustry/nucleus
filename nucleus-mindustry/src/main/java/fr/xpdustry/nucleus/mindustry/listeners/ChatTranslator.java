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
package fr.xpdustry.nucleus.mindustry.listeners;

import arc.util.serialization.Jval;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import fr.xpdustry.nucleus.mindustry.util.URIBuilder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ChatTranslator implements PluginListener {

    private final HttpClient client = HttpClient.newHttpClient();
    private final NucleusPlugin nucleus;

    public ChatTranslator(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public void onPluginLoad() {
        this.nucleus.addProcessor((sender, message, receiver) -> {
            if (sender.locale().equals(receiver.locale())) {
                return message;
            }
            final var uri = URIBuilder.of(this.nucleus.getConfiguration().getTranslationEndpoint())
                    .withParameter("q", message)
                    .withParameter("source", sender.locale())
                    .withParameter("target", receiver.locale())
                    .withParameter("format", "text")
                    .withParameter("api_key", this.nucleus.getConfiguration().getTranslationToken())
                    .build();
            final var result = client.sendAsync(
                            HttpRequest.newBuilder()
                                    .uri(uri)
                                    .POST(HttpRequest.BodyPublishers.noBody())
                                    .build(),
                            HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .orTimeout(1L, TimeUnit.SECONDS)
                    .exceptionally(throwable -> {
                        final var reason = throwable instanceof TimeoutException ? "timed out" : throwable.getMessage();
                        return "{\"error\": \"%s\"}".formatted(reason);
                    })
                    .join();
            final var json = Jval.read(result);
            if (json.has("error")) {
                this.nucleus
                        .getLogger()
                        .atDebug()
                        .setMessage("Failed to translate the message '{}' from {} to {}: {}")
                        .addArgument(message)
                        .addArgument(sender.locale())
                        .addArgument(receiver.locale())
                        .addArgument(json.getString("error"))
                        .log();
                return message;
            } else {
                return message + " [gray](" + json.getString("translatedText").trim() + ")";
            }
        });
    }
}
