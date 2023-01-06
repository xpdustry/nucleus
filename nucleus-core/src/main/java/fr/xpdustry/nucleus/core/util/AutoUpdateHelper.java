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
package fr.xpdustry.nucleus.core.util;

import fr.xpdustry.nucleus.core.NucleusApplication;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AutoUpdateHelper {

    private final AtomicBoolean updating = new AtomicBoolean(false);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5L))
            .build();
    private final NucleusApplication nucleus;

    protected AutoUpdateHelper(final NucleusApplication nucleus) {
        this.nucleus = nucleus;
    }

    protected void onAutoUpdateCheckStart() {
        executor.execute(this::onAutoUpdateCheck);
    }

    protected void onAutoUpdateCheckStop() {
        executor.shutdownNow();
    }

    protected void onAutoUpdateStart(final NucleusVersion version) {
        if (!updating.compareAndSet(false, true)) {
            getNucleus().getLogger().debug("Already updating to version {}", version);
            return;
        }
        final HttpResponse<InputStream> response;
        try {
            response = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("https://github.com/Xpdustry/Nucleus/releases/download/" + version + "/"
                                    + getArtifactName()))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofInputStream());
        } catch (final IOException | InterruptedException e) {
            getNucleus().getLogger().error("Failed to download latest update", e);
            return;
        }
        if (response.statusCode() != 200) {
            if (response.statusCode() == 404) {
                getNucleus().getLogger().warn("Failed to find build {}.", version);
            } else {
                getNucleus()
                        .getLogger()
                        .warn("Failed to download latest build {} (status-code={}).", version, response.statusCode());
            }
            return;
        }
        try (final var stream = response.body()) {
            final var temp = Files.createTempFile("nucleus", ".jar.tmp");
            Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, getApplicationJarLocation(), StandardCopyOption.REPLACE_EXISTING);
            this.onAutoUpdateFinished(version);
        } catch (final IOException e) {
            getNucleus().getLogger().error("Failed to update the application", e);
        }
    }

    protected NucleusApplication getNucleus() {
        return nucleus;
    }

    protected HttpClient getHttpClient() {
        return httpClient;
    }

    protected abstract Path getApplicationJarLocation();

    protected abstract NucleusVersion getLatestVersion();

    protected abstract String getArtifactName();

    protected abstract void onAutoUpdateFinished(final NucleusVersion version);

    private void onAutoUpdateCheck() {
        if (!nucleus.getConfiguration().isAutoUpdateEnabled()) {
            executor.schedule(
                    this::onAutoUpdateCheck, nucleus.getConfiguration().getAutoUpdateInterval(), TimeUnit.SECONDS);
            return;
        }
        final var latest = getLatestVersion();
        getNucleus().getLogger().trace("Retrieved latest version {}", latest);
        if (latest.isNewerThan(nucleus.getVersion())) {
            getNucleus().getLogger().info("Build {} available, performing update.", latest);
            this.onAutoUpdateStart(latest);
        } else {
            executor.schedule(
                    this::onAutoUpdateCheck, nucleus.getConfiguration().getAutoUpdateInterval(), TimeUnit.SECONDS);
        }
    }
}
