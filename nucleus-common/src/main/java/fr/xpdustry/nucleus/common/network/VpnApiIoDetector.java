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

import com.google.common.net.InetAddresses;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.xpdustry.nucleus.common.annotation.NucleusExecutor;
import fr.xpdustry.nucleus.common.configuration.NucleusConfiguration;
import fr.xpdustry.nucleus.common.web.ApiServiceException;
import fr.xpdustry.nucleus.common.web.ApiServiceRateLimitException;
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
        try {
            final var ip = InetAddresses.forString(address);
            if (ip.isLoopbackAddress()) {
                return CompletableFuture.completedFuture(false);
            }
        } catch (final Exception exception) {
            return CompletableFuture.failedFuture(new ApiServiceException("Invalid IP address: " + address));
        }

        final var builder = URIBuilder.of("https://vpnapi.io/api/" + address);
        if (!this.key.isBlank()) {
            builder.addParameter("key", this.key);
        }

        if (InetAddresses.forString(address).isLoopbackAddress()) {
            return CompletableFuture.completedFuture(false);
        }

        return this.http
                .sendAsync(
                        HttpRequest.newBuilder()
                                .uri(builder.build())
                                .timeout(Duration.ofSeconds(3L))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() == 429) {
                        return CompletableFuture.failedFuture(new ApiServiceRateLimitException());
                    }

                    if (response.statusCode() != 200) {
                        return CompletableFuture.failedFuture(
                                new ApiServiceException("Unexpected status code: " + response.statusCode()));
                    }

                    final var security = this.gson
                            .fromJson(response.body(), JsonObject.class)
                            .get("security")
                            .getAsJsonObject();
                    return CompletableFuture.completedFuture(security.get("vpn").getAsBoolean()
                            || security.get("proxy").getAsBoolean());
                });
    }
}
