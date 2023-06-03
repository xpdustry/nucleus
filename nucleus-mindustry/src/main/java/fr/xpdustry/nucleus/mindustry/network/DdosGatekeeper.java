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

import com.google.common.net.InetAddresses;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.xpdustry.distributor.api.event.EventHandler;
import fr.xpdustry.distributor.api.util.Priority;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import mindustry.game.EventType;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DdosGatekeeper implements NucleusListener {

    private static final Logger logger = LoggerFactory.getLogger(DdosGatekeeper.class);

    private final List<AddressesProvider> providers = new ArrayList<>();
    private final Set<String> blocked = new HashSet<>();

    @Inject
    public DdosGatekeeper() {
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
        for (final var provider : this.providers) {
            final Collection<InetAddress> addresses;
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
            for (final var address : addresses) {
                this.blocked.add(address.getHostAddress());
            }
        }

        logger.info("Blocked {} cloud addresses.", this.blocked.size());
    }

    @EventHandler(priority = Priority.HIGHEST)
    public void onPlayerConnect(final EventType.PlayerConnect event) {
        if (this.blocked.contains(event.player.ip())) {
            event.player.kick("Cloud addresses are not allowed on this server.");
        }
    }

    private static InetAddress toInetAddresses(final String string) {
        return InetAddresses.forString(string.split("/", 2)[0]);
    }

    private interface AddressesProvider {

        String getProviderName();

        Collection<InetAddress> getAddresses() throws IOException, InterruptedException;
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
        public final Collection<InetAddress> getAddresses() throws IOException, InterruptedException {
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

        protected abstract Collection<InetAddress> getAddresses(final JsonObject object);
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
        protected Collection<InetAddress> getAddresses(final JsonObject object) {
            return object.get("values").getAsJsonArray().asList().stream()
                    .map(element -> element.getAsJsonObject()
                            .get("properties")
                            .getAsJsonObject()
                            .get("addressPrefixes")
                            .getAsJsonArray())
                    .flatMap(array -> array.asList().stream().map(element -> toInetAddresses(element.getAsString())))
                    .collect(Collectors.toUnmodifiableSet());
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
        protected Collection<InetAddress> getAddresses(final JsonObject object) {
            return object.get("actions").getAsJsonArray().asList().stream()
                    .map(element -> toInetAddresses(element.getAsString()))
                    .collect(Collectors.toUnmodifiableSet());
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
        protected Collection<InetAddress> getAddresses(final JsonObject object) {
            final Set<InetAddress> addresses = new HashSet<>();
            addresses.addAll(parsePrefix(object, "prefixes", "ip_prefix"));
            addresses.addAll(parsePrefix(object, "ipv6_prefixes", "ipv6_prefix"));
            return Collections.unmodifiableSet(addresses);
        }

        private Set<InetAddress> parsePrefix(final JsonObject object, final String name, final String element) {
            return object.get(name).getAsJsonArray().asList().stream()
                    .map(entry ->
                            toInetAddresses(entry.getAsJsonObject().get(element).getAsString()))
                    .collect(Collectors.toUnmodifiableSet());
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
        protected Collection<InetAddress> getAddresses(JsonObject object) {
            return object.get("prefixes").getAsJsonArray().asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .map(element -> element.has("ipv4Prefix")
                            ? toInetAddresses(element.get("ipv4Prefix").getAsString())
                            : toInetAddresses(element.get("ipv6Prefix").getAsString()))
                    .collect(Collectors.toUnmodifiableSet());
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
        protected Collection<InetAddress> getAddresses(final JsonObject object) {
            return object.get("regions").getAsJsonArray().asList().stream()
                    .flatMap(element -> element.getAsJsonObject().get("cidrs").getAsJsonArray().asList().stream())
                    .map(element -> toInetAddresses(
                            element.getAsJsonObject().get("cidr").getAsString()))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }
}
