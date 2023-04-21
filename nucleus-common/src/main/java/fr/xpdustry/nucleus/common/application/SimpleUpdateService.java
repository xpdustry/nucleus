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
package fr.xpdustry.nucleus.common.application;

import fr.xpdustry.nucleus.api.application.NucleusRuntime;
import fr.xpdustry.nucleus.api.application.NucleusVersion;
import fr.xpdustry.nucleus.api.application.ShutdownEvent;
import fr.xpdustry.nucleus.api.application.ShutdownEvent.Cause;
import fr.xpdustry.nucleus.api.application.update.UpdateService;
import fr.xpdustry.nucleus.api.event.EventService;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import org.slf4j.Logger;

public class SimpleUpdateService implements UpdateService {

    private final AtomicBoolean updating = new AtomicBoolean(false);
    private final NucleusRuntime runtime;
    private final EventService event;
    private final HttpClient httpClient;

    @Inject
    private Logger logger;

    @Inject
    public SimpleUpdateService(final NucleusRuntime runtime, final EventService event) {
        this.runtime = runtime;
        this.event = event;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(5L))
                .executor(runtime.getAsyncExecutor())
                .build();
    }

    @Override
    public void update(final NucleusVersion version) {
        if (!version.isNewerThan(this.runtime.getVersion())) {
            this.logger.warn("Attempted to update to the older version {}", version);
            return;
        }
        if (!this.updating.compareAndSet(false, true)) {
            return;
        }
        final HttpResponse<InputStream> response;
        try {
            final var artifactName =
                    "Nucleus" + capitalize(this.runtime.getPlatform().name()) + ".jar";
            response = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("https://github.com/Xpdustry/Nucleus/releases/download/" + version + "/"
                                    + artifactName))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofInputStream());
        } catch (final IOException | InterruptedException e) {
            this.logger.error("Failed to download latest update", e);
            return;
        }
        if (response.statusCode() != 200) {
            if (response.statusCode() == 404) {
                this.logger.error("Failed to find build {}", version);
            } else {
                this.logger.error(
                        "Failed to download latest build {} (status-code={}).", version, response.statusCode());
            }
            return;
        }
        try (final var stream = response.body()) {
            final var temp = Files.createTempFile("nucleus", ".jar.tmp");
            Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, this.runtime.getApplicationJar(), StandardCopyOption.REPLACE_EXISTING);
            this.event.publish(ShutdownEvent.of(Cause.RESTART));
        } catch (final IOException e) {
            this.logger.error("Failed to update the application", e);
        }
    }

    private static String capitalize(final String string) {
        return string.substring(0, 1).toUpperCase(Locale.ENGLISH) + string.substring(1);
    }
}