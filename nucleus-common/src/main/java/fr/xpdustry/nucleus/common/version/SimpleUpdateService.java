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
package fr.xpdustry.nucleus.common.version;

import fr.xpdustry.nucleus.common.annotation.NucleusExecutor;
import fr.xpdustry.nucleus.common.application.NucleusApplication;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class SimpleUpdateService implements UpdateService {

    private final NucleusApplication application;
    private final HttpClient httpClient;
    private final AtomicBoolean updated = new AtomicBoolean(false);
    private @MonotonicNonNull CompletableFuture<Void> updating = null;

    @Inject
    public SimpleUpdateService(final NucleusApplication application, final @NucleusExecutor Executor executor) {
        this.application = application;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(5L))
                .executor(executor)
                .build();
    }

    @Override
    public CompletableFuture<Void> update(final NucleusVersion version) {
        if (!version.isNewerThan(this.application.getVersion())) {
            return CompletableFuture.failedFuture(
                    new UpdateException("Attempted to update to the older version " + version));
        } else if (this.updated.get()) {
            return CompletableFuture.failedFuture(new UpdateException("Already updated"));
        }

        synchronized (this) {
            if (updating == null || updating.isDone()) {
                updating = update0(version);
            }
            return updating;
        }
    }

    private CompletableFuture<Void> update0(final NucleusVersion version) {
        final var artifactName =
                "Nucleus" + capitalize(this.application.getPlatform().name()) + ".jar";

        return httpClient
                .sendAsync(
                        HttpRequest.newBuilder()
                                .uri(URI.create("https://github.com/Xpdustry/Nucleus/releases/download/" + version + "/"
                                        + artifactName))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofInputStream())
                .thenCompose(response -> {
                    if (response.statusCode() == 404) {
                        return CompletableFuture.failedFuture(new UpdateException("Failed to find build " + version));
                    } else if (response.statusCode() == 200) {
                        return CompletableFuture.completedFuture(response);
                    } else {
                        return CompletableFuture.failedFuture(
                                new UpdateException("Failed to download latest build %s (status-code=%s)."
                                        .formatted(version, response.statusCode())));
                    }
                })
                .thenCompose(response -> {
                    try (final var stream = response.body()) {
                        final var temp = Files.createTempFile("nucleus", ".jar.tmp");
                        Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
                        Files.move(temp, this.application.getApplicationJar(), StandardCopyOption.REPLACE_EXISTING);
                        this.updated.set(true);
                        return CompletableFuture.completedFuture(null);
                    } catch (final IOException e) {
                        return CompletableFuture.failedFuture(
                                new UpdateException("Failed to update the application.", e));
                    }
                });
    }

    private static String capitalize(final String string) {
        return string.substring(0, 1).toUpperCase(Locale.ENGLISH) + string.substring(1);
    }
}
