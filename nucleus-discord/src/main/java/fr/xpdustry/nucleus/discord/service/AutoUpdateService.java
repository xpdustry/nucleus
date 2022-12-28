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
package fr.xpdustry.nucleus.discord.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.xpdustry.nucleus.core.event.ImmutableAutoUpdateEvent;
import fr.xpdustry.nucleus.core.messages.VersionRequest;
import fr.xpdustry.nucleus.core.util.AutoUpdateHelper;
import fr.xpdustry.nucleus.core.util.NucleusVersion;
import fr.xpdustry.nucleus.discord.NucleusBot;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.Button;

public final class AutoUpdateService extends AutoUpdateHelper implements NucleusDiscordService {

    private final Gson gson = new Gson();

    public AutoUpdateService(final NucleusBot nucleus) {
        super(nucleus);
    }

    @Override
    public void onNucleusDiscordInit() {
        getNucleus().getMessenger().respond(VersionRequest.class, request -> getNucleus()
                .getVersion());
        onAutoUpdateCheckStart();
    }

    @Override
    protected String getArtifactName() {
        return "NucleusDiscord.jar";
    }

    @Override
    protected void onAutoUpdateStart(final NucleusVersion version) {
        new MessageBuilder()
                .setContent("Build **" + version + "** is available.")
                .addActionRow(Button.primary("temp:update", "Update"))
                .send(getNucleus().getSystemChannel())
                .thenAccept(message -> message.addButtonClickListener(click -> {
                    if (!click.getButtonInteraction().getUser().isBotOwner()) {
                        click.getButtonInteraction()
                                .createImmediateResponder()
                                .setContent("You are not the bot owner.")
                                .respond();
                        return;
                    }
                    click.getButtonInteraction()
                            .acknowledge()
                            .thenCompose(acknowledge -> message.createUpdater()
                                    .removeAllComponents()
                                    .setContent("Updating servers to **" + version + "**")
                                    .applyChanges())
                            .thenRunAsync(() -> {
                                getNucleus().getLogger().info("Updating to version " + version);
                                super.onAutoUpdateStart(version);
                            });
                }));
    }

    @Override
    protected void onAutoUpdateFinished() {
        getNucleus()
                .getMessenger()
                .send(ImmutableAutoUpdateEvent.builder()
                        .version(getNucleus().getVersion())
                        .build());
        getNucleus().restart();
    }

    @Override
    protected NucleusVersion getLatestVersion() {
        var latest = getNucleus().getVersion();
        try {
            final var response = getHttpClient()
                    .send(
                            HttpRequest.newBuilder()
                                    .uri(URI.create("https://api.github.com/repos/Xpdustry/Nucleus/releases/latest"))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            final var json = gson.fromJson(response.body(), JsonObject.class);
            latest = NucleusVersion.parse(json.get("tag_name").getAsString());
        } catch (final IOException | InterruptedException e) {
            getNucleus().getLogger().error("Failed to check for latest update", e);
        }
        return latest;
    }

    @Override
    protected Path getApplicationJarLocation() {
        final var codeSource = getClass().getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            throw new IllegalStateException("Could not find code source");
        }
        final var location = codeSource.getLocation();
        if (location == null) {
            throw new IllegalStateException("Could not find code source location");
        }
        return Path.of(location.getPath());
    }

    @Override
    protected NucleusBot getNucleus() {
        return (NucleusBot) super.getNucleus();
    }
}
