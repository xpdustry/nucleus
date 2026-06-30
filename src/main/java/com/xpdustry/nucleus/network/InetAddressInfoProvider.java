// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.network;

import com.google.gson.Gson;
import com.xpdustry.foundation.annotation.ScheduledTaskHandler;
import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.foundation.scheduler.MindustryTimeUnit;
import com.xpdustry.nucleus.config.ConfigManager;
import com.xpdustry.nucleus.config.ConfigPropertyKey;
import com.xpdustry.nucleus.database.PostgresDatabase;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InetAddressInfoProvider implements PluginListener {

    private static final Logger log = LoggerFactory.getLogger(InetAddressInfoProvider.class);

    private final ConfigManager configManager;
    private final Gson gson;
    private final HttpClient http;
    private final PostgresDatabase database;
    // TODO With custom name and unhandled exception handler
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public InetAddressInfoProvider(
            final ConfigManager configManager,
            final Gson gson,
            final HttpClient http,
            final PostgresDatabase database) {
        this.configManager = configManager;
        this.gson = gson;
        this.http = http;
        this.database = database;
    }

    public Optional<InetAddressInfo> get(final InetAddress address) {
        final var cached = this.database.withHandle(handle -> handle.prepareStatement("""
                        SELECT a."safe" as "safe", a."country_code", a."asn_name", a."asn_number" as "country"
                        FROM "address_info_request_cache" a
                        WHERE a."address" = ?::inet AND a."updated_at" + a."ttl" >= current_timestamp
                        """)
                .push(address.getHostAddress())
                .executeSingleSelect(result -> new InetAddressInfo(
                        result.getBoolean(1), result.getString(2), result.getString("3"), result.getLong("4"))));
        if (cached.isPresent()) {
            return cached;
        }

        final var token = this.configManager.get(ConfigPropertyKey.VPN_API_IO_TOKEN);
        if (token.isEmpty()) {
            return Optional.empty();
        }

        final HttpResponse<String> response;
        try {
            response = this.http.send(
                    HttpRequest.newBuilder(URI.create("https://vpnapi.io/api/"
                                    + URLEncoder.encode(address.getHostAddress(), StandardCharsets.UTF_8) + "?key="
                                    + URLEncoder.encode(token, StandardCharsets.UTF_8)))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (final Exception e) {
            log.error("An error occurred while retrieving info about {} from vpnapi.io", address.getHostAddress(), e);
            return Optional.empty();
        }

        final var code = response.statusCode();
        if (code != 200) {
            log.error(
                    "An unexpected response has been received from vpnapi.io while retrieving info about {} (status_code={}, body={})",
                    address.getHostAddress(),
                    code,
                    response.body());
            return Optional.empty();
        }

        try {
            final var body = this.gson.fromJson(response.body(), VpnApiIoResponse.class);
            final var safe = body.security.vpn || body.security.proxy || body.security.tor || body.security.relay;
            final var countryCode = body.location.country_code;
            final var asnName = body.network.autonomous_system_organization;
            final var asnNumber = Long.parseLong(body.network.autonomous_system_number.replaceFirst("AS", ""));

            this.database.withHandle(handle -> handle.prepareStatement("""
                        INSERT INTO "address_info_request_cache"("address", "safe", "country_code", "asn_name", "asn_number")
                        VALUES (?::inet, ?, ?, ?, ?)
                        ON CONFLICT ("address") DO UPDATE SET
                            "updated_at"    = current_timestamp,
                            "safe"          = excluded."safe",
                            "country_code"  = excluded."country_code",
                            "asn_name"      = excluded."asn_name",
                            "asn_number"    = excluded."asn_number"
                        """)
                    .push(address.getHostAddress())
                    .push(safe)
                    .push(countryCode)
                    .push(asnName)
                    .push(asnNumber)
                    .executeSingleUpdate());

            return Optional.of(new InetAddressInfo(safe, countryCode, asnName, asnNumber));
        } catch (final Exception e) {
            log.error(
                    "Failed to deserialize the response from vpnapi.io while retrieving info about {} (json={})",
                    address.getHostAddress(),
                    response.body(),
                    e);
            return Optional.empty();
        }
    }

    // TODO Add error handling + better logging
    @ScheduledTaskHandler(initialDelay = 0, delay = 12, unit = MindustryTimeUnit.HOURS)
    void housekeeping() {
        this.executor.execute(() ->
                this.database.withHandle(handle -> handle.prepareStatement("""
                    DELETE FROM "address_info_request_cache" a
                    WHERE a."updated_at" + a."ttl" < current_timestamp
                    """).executeUpdate()));
    }

    // https://vpnapi.io/api-documentation
    private record VpnApiIoResponse(Security security, Location location, Network network) {

        record Security(boolean vpn, boolean proxy, boolean tor, boolean relay) {}

        record Location(String country_code) {}

        record Network(String autonomous_system_number, String autonomous_system_organization) {}
    }
}
