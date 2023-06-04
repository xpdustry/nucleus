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

public final class IpHubVpnDetector implements VpnDetector {

    private final String token;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    @Inject
    public IpHubVpnDetector(final NucleusConfiguration configuration, final @NucleusExecutor Executor executor) {
        this.token = configuration.getIpHubToken();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3L))
                .executor(executor)
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

        if (this.token.isBlank()) {
            return CompletableFuture.failedFuture(new ApiServiceRateLimitException("IpHub token is blank."));
        }

        return this.httpClient
                .sendAsync(
                        HttpRequest.newBuilder()
                                .uri(URIBuilder.of("https://v2.api.iphub.info/ip/" + address)
                                        .addParameter("key", this.token)
                                        .build())
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

                    // https://iphub.info/api
                    // block: 0 - Residential or business IP (i.e. safe IP)
                    // block: 1 - Non-residential IP (hosting provider, proxy, etc.)
                    // block: 2 - Non-residential & residential IP (warning, may flag innocent people)
                    final var type = this.gson
                            .fromJson(response.body(), JsonObject.class)
                            .get("block")
                            .getAsInt();
                    return CompletableFuture.completedFuture(type == 1);
                });
    }
}
