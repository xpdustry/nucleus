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
package fr.xpdustry.nucleus.mindustry.translator;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import fr.xpdustry.nucleus.mindustry.util.URIBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class LibreTranslateTranslator implements Translator {

    private final Gson gson = new Gson();
    private final NucleusPlugin nucleus;
    private final HttpClient client;

    public LibreTranslateTranslator(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
        this.client = HttpClient.newBuilder()
                .executor(this.nucleus.getScheduler().getAsyncExecutor())
                .connectTimeout(Duration.ofMillis(500L))
                .build();
    }

    @Override
    public CompletableFuture<String> translate(final String text, final Locale source, final Locale target) {
        final var sourceLanguage = source.getLanguage();
        final var targetLanguage = target.getLanguage();
        if (sourceLanguage.equals("router")) {
            return CompletableFuture.completedFuture("router");
        }
        final var uri = URIBuilder.of(this.nucleus.getConfiguration().getTranslationEndpoint() + "/translate")
                .withParameter("q", text)
                .withParameter("source", sourceLanguage)
                .withParameter("target", targetLanguage)
                .withParameter("format", "text")
                .withParameter("api_key", this.nucleus.getConfiguration().getTranslationToken())
                .build();
        return client.sendAsync(
                        HttpRequest.newBuilder()
                                .uri(uri)
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build(),
                        HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> gson.fromJson(body, JsonObject.class))
                .thenCompose(json -> {
                    if (json.has("error")) {
                        return CompletableFuture.failedFuture(
                                new RuntimeException(json.get("error").getAsString()));
                    }
                    return CompletableFuture.completedFuture(
                            json.get("translatedText").getAsString());
                });
    }

    @Override
    public CompletableFuture<Boolean> supportsLanguage(final Locale locale) {
        if (locale.getLanguage().equals("router")) {
            return CompletableFuture.completedFuture(true);
        }
        return client.sendAsync(
                        HttpRequest.newBuilder()
                                .uri(URI.create(
                                        this.nucleus.getConfiguration().getTranslationEndpoint() + "/languages"))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> gson.fromJson(body, JsonElement.class))
                .thenCompose(json -> {
                    if (json.isJsonObject() && json.getAsJsonObject().has("error")) {
                        return CompletableFuture.failedFuture(new RuntimeException(
                                json.getAsJsonObject().get("error").getAsString()));
                    }
                    for (final var element : json.getAsJsonArray()) {
                        if (element.getAsJsonObject().get("code").getAsString().equals(locale.getLanguage())) {
                            return CompletableFuture.completedFuture(true);
                        }
                    }
                    return CompletableFuture.completedFuture(false);
                });
    }
}
