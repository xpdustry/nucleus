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
package fr.xpdustry.nucleus.common.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.xpdustry.nucleus.common.annotation.NucleusExecutor;
import fr.xpdustry.nucleus.common.configuration.NucleusConfiguration;
import fr.xpdustry.nucleus.common.exception.RatelimitException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public final class VpnApiIoDetector implements VpnDetector {

    private final Gson gson = new Gson();
    private final String key;
    private final HttpClient http;

    @Inject
    public VpnApiIoDetector(final NucleusConfiguration configuration, final @NucleusExecutor Executor executor) {
        this.key = configuration.getVpnApiIoToken();
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3L))
                .executor(executor)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public CompletableFuture<Boolean> isVpn(final String address) {
        final var builder = URIBuilder.of("https://vpnapi.io/api/" + address);
        if (!this.key.isBlank()) {
            builder.addParameter("key", this.key);
        }
        return this.http
                .sendAsync(HttpRequest.newBuilder(builder.build()).GET().build(), HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() == 429) {
                        return CompletableFuture.failedFuture(new RatelimitException());
                    }
                    if (response.statusCode() != 200) {
                        return CompletableFuture.failedFuture(
                                new IOException("Invalid status code: " + response.statusCode()));
                    }
                    final var result =
                            gson.fromJson(response.body(), JsonObject.class).getAsJsonObject("security");
                    return CompletableFuture.completedFuture(result.get("vpn").getAsBoolean()
                            || result.get("proxy").getAsBoolean()
                            || result.get("tor").getAsBoolean());
                });
    }
}
