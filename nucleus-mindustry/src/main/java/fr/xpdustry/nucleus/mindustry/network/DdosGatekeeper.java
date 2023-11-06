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
package fr.xpdustry.nucleus.mindustry.network;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.net.InetAddresses;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.xpdustry.distributor.api.event.EventHandler;
import fr.xpdustry.distributor.api.util.Priority;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.common.database.model.Punishment.Kind;
import fr.xpdustry.nucleus.common.network.VpnDetector;
import fr.xpdustry.nucleus.mindustry.moderation.ModerationService;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import mindustry.game.EventType;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public final class DdosGatekeeper implements NucleusListener {

    private static final Logger logger = LoggerFactory.getLogger(DdosGatekeeper.class);

    // TODO Cache in a local file
    private final List<AddressesProvider> providers = new ArrayList<>();
    private final RangeSet<BigInteger> blocked = TreeRangeSet.create();
    private final VpnDetector vpnDetector;
    private final ModerationService moderation;

    @Inject
    public DdosGatekeeper(final VpnDetector vpnDetector, final ModerationService moderation) {
        this.vpnDetector = vpnDetector;
        this.moderation = moderation;

        final var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5L))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        final var gson = new Gson();

        this.providers.add(new AzureAddressesProvider(httpClient, gson));
        this.providers.add(new GithubActionsAddressesProvider(httpClient, gson));
        this.providers.add(new AmazonWebServicesAddressesProvider(httpClient, gson));
        this.providers.add(new GoogleCloudAddressesProvider(httpClient, gson));
        this.providers.add(new OracleCloudAddressesProvider(httpClient, gson));
    }

    @Override
    public void onNucleusInit() {
        var count = 0;
        for (final var provider : this.providers) {
            final Collection<Range<BigInteger>> addresses;
            try {
                addresses = provider.getAddresses();
            } catch (final Exception e) {
                logger.error("Failed to get addresses for cloud provider '{}'", provider.getProviderName(), e);
                continue;
            }

            if (addresses.isEmpty()) {
                logger.warn("No addresses found for cloud provider '{}'", provider.getProviderName());
                continue;
            }

            logger.info("Found {} addresses for cloud provider '{}'", addresses.size(), provider.getProviderName());
            count += addresses.size();
            this.blocked.addAll(addresses);
        }

        logger.info("Blocked {} cloud addresses.", count);
    }

    @EventHandler(priority = Priority.HIGH)
    public void onPlayerConnect(final EventType.PlayerConnect event) {
        if (event.player.con().kicked) {
            return;
        }

        if (this.blocked.encloses(createInetAddressRange(event.player.ip()))) {
            this.moderation
                    .punish(
                            null,
                            event.player,
                            Kind.KICK,
                            "Cloud addresses are not allowed on this server.",
                            Duration.ofDays(7L))
                    .join();
            return;
        }

        if (this.isVpn(event.player.ip())) {
            this.moderation
                    .punish(
                            null,
                            event.player,
                            Kind.KICK,
                            "VPN addresses are not allowed on this server.",
                            Duration.ofDays(7L))
                    .join();
            return;
        }
    }

    private boolean isVpn(final String address) {
        logger.debug("Checking if '{}' is a VPN.", address);
        return this.vpnDetector
                .isVpn(address)
                .exceptionally(throwable -> {
                    logger.debug("Failed to check if '{}' is a VPN.", address, throwable);
                    return false;
                })
                .join();
    }

    private static Range<BigInteger> createInetAddressRange(final String address) {
        final var parts = address.split("/", 2);
        final var parsedAddress = InetAddresses.forString(parts[0]);
        if (parts.length != 2) {
            return Range.singleton(new BigInteger(1, parsedAddress.getAddress()));
        }
        final var bigIntAddress = new BigInteger(1, parsedAddress.getAddress());
        final var cidrPrefixLen = Integer.parseInt(parts[1]);
        final var bits = parsedAddress instanceof Inet4Address ? 32 : 128;
        final var addressCount = BigInteger.ONE.shiftLeft(bits - cidrPrefixLen);
        return Range.closed(bigIntAddress, bigIntAddress.add(addressCount));
    }

    private interface AddressesProvider {

        String getProviderName();

        Collection<Range<BigInteger>> getAddresses() throws IOException, InterruptedException;
    }

    private abstract static class JsonAddressesProvider implements AddressesProvider {

        private final String name;
        private final HttpClient httpClient;
        private final Gson gson;

        protected JsonAddressesProvider(final String name, final HttpClient httpClient, final Gson gson) {
            this.name = name;
            this.httpClient = httpClient;
            this.gson = gson;
        }

        @Override
        public final String getProviderName() {
            return this.name;
        }

        @Override
        public final Collection<Range<BigInteger>> getAddresses() throws IOException, InterruptedException {
            final var response = this.httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(this.getUri())
                            .timeout(Duration.ofSeconds(10L))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException(String.format(
                        "Failed to download '%s' public addresses file (status-code: %d, url: %s).",
                        this.getProviderName(), response.statusCode(), this.getUri()));
            }

            try (final Reader reader = new InputStreamReader(response.body(), StandardCharsets.UTF_8)) {
                return this.getAddresses(this.gson.fromJson(reader, JsonObject.class));
            }
        }

        protected abstract URI getUri() throws IOException;

        protected abstract Collection<Range<BigInteger>> getAddresses(final JsonObject object);
    }

    private static final class AzureAddressesProvider extends JsonAddressesProvider {

        private static final String AZURE_PUBLIC_ADDRESSES_DOWNLOAD_LINK =
                "https://www.microsoft.com/en-us/download/confirmation.aspx?id=56519";

        private AzureAddressesProvider(final HttpClient httpClient, final Gson gson) {
            super("Azure", httpClient, gson);
        }

        @Override
        protected URI getUri() throws IOException {
            return Jsoup.connect(AZURE_PUBLIC_ADDRESSES_DOWNLOAD_LINK)
                    .get()
                    .select("a[href*=download.microsoft.com]")
                    .stream()
                    .map(element -> element.attr("abs:href"))
                    .filter(l -> l.contains("ServiceTags_Public"))
                    .findFirst()
                    .map(URI::create)
                    .orElseThrow(() -> new IOException("Failed to find download link."));
        }

        @Override
        protected Collection<Range<BigInteger>> getAddresses(final JsonObject object) {
            return object.get("values").getAsJsonArray().asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .filter(element -> element.get("name").getAsString().equals("AzureCloud"))
                    .map(element -> element.get("properties")
                            .getAsJsonObject()
                            .get("addressPrefixes")
                            .getAsJsonArray())
                    .flatMap(array ->
                            array.asList().stream().map(element -> createInetAddressRange(element.getAsString())))
                    .toList();
        }
    }

    private static final class GithubActionsAddressesProvider extends JsonAddressesProvider {

        private static final URI GITHUB_ACTIONS_ADDRESSES_DOWNLOAD_LINK = URI.create("https://api.github.com/meta");

        private GithubActionsAddressesProvider(final HttpClient httpClient, final Gson gson) {
            super("Github Actions", httpClient, gson);
        }

        @Override
        protected URI getUri() {
            return GITHUB_ACTIONS_ADDRESSES_DOWNLOAD_LINK;
        }

        @Override
        protected Collection<Range<BigInteger>> getAddresses(final JsonObject object) {

            return object.get("actions").getAsJsonArray().asList().stream()
                    .map(element -> createInetAddressRange(element.getAsString()))
                    .toList();
        }
    }

    private static final class AmazonWebServicesAddressesProvider extends JsonAddressesProvider {

        private static final URI AMAZON_WEB_SERVICES_ADDRESSES_DOWNLOAD_LINK =
                URI.create("https://ip-ranges.amazonaws.com/ip-ranges.json");

        private AmazonWebServicesAddressesProvider(final HttpClient httpClient, final Gson gson) {
            super("Amazon Web Services", httpClient, gson);
        }

        @Override
        protected URI getUri() {
            return AMAZON_WEB_SERVICES_ADDRESSES_DOWNLOAD_LINK;
        }

        @Override
        protected Collection<Range<BigInteger>> getAddresses(final JsonObject object) {
            final Set<Range<BigInteger>> addresses = new HashSet<>();
            addresses.addAll(parsePrefix(object, "prefixes", "ip_prefix"));
            addresses.addAll(parsePrefix(object, "ipv6_prefixes", "ipv6_prefix"));
            return addresses;
        }

        private Collection<Range<BigInteger>> parsePrefix(
                final JsonObject object, final String name, final String element) {
            return object.get(name).getAsJsonArray().asList().stream()
                    .map(entry -> createInetAddressRange(
                            entry.getAsJsonObject().get(element).getAsString()))
                    .toList();
        }
    }

    private static final class GoogleCloudAddressesProvider extends JsonAddressesProvider {

        private static final URI GOOGLE_CLOUD_ADDRESSES_DOWNLOAD_LINK =
                URI.create("https://www.gstatic.com/ipranges/cloud.json");

        private GoogleCloudAddressesProvider(final HttpClient httpClient, final Gson gson) {
            super("Google Cloud", httpClient, gson);
        }

        @Override
        protected URI getUri() {
            return GOOGLE_CLOUD_ADDRESSES_DOWNLOAD_LINK;
        }

        @Override
        protected Collection<Range<BigInteger>> getAddresses(JsonObject object) {
            return object.get("prefixes").getAsJsonArray().asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .map(element -> element.has("ipv4Prefix")
                            ? createInetAddressRange(element.get("ipv4Prefix").getAsString())
                            : createInetAddressRange(element.get("ipv6Prefix").getAsString()))
                    .toList();
        }
    }

    private static final class OracleCloudAddressesProvider extends JsonAddressesProvider {

        private static final URI ORACLE_CLOUD_ADDRESSES_DOWNLOAD_LINK =
                URI.create("https://docs.cloud.oracle.com/en-us/iaas/tools/public_ip_ranges.json");

        private OracleCloudAddressesProvider(final HttpClient httpClient, final Gson gson) {
            super("Oracle Cloud", httpClient, gson);
        }

        @Override
        protected URI getUri() {
            return ORACLE_CLOUD_ADDRESSES_DOWNLOAD_LINK;
        }

        @Override
        protected Collection<Range<BigInteger>> getAddresses(final JsonObject object) {
            return object.get("regions").getAsJsonArray().asList().stream()
                    .flatMap(element -> element.getAsJsonObject().get("cidrs").getAsJsonArray().asList().stream())
                    .map(element -> createInetAddressRange(
                            element.getAsJsonObject().get("cidr").getAsString()))
                    .toList();
        }
    }
}
