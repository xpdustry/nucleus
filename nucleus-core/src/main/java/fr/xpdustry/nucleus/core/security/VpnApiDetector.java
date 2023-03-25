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
package fr.xpdustry.nucleus.core.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.xpdustry.nucleus.core.util.URIBuilder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class VpnApiDetector implements VpnDetector {

    private final Gson gson = new Gson();
    private final String key;
    private final HttpClient http;

    public VpnApiDetector(final String key, final Executor executor) {
        this.key = key;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3L))
                .executor(executor)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public CompletableFuture<Boolean> isVpn(final String address) {
        return http.sendAsync(
                        HttpRequest.newBuilder(URIBuilder.of("https://vpnapi.io/api/" + address)
                                        .withParameter("key", this.key)
                                        .build())
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        // TODO Throw exception when rate limited ?
                        return false;
                    }
                    final var result =
                            gson.fromJson(response.body(), JsonObject.class).getAsJsonObject("security");
                    return result.get("vpn").getAsBoolean()
                            || result.get("proxy").getAsBoolean();
                });
    }
}
