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
package fr.xpdustry.nucleus.discord.interaction.command;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.xpdustry.nucleus.common.annotation.NucleusExecutor;
import fr.xpdustry.nucleus.common.application.NucleusApplication;
import fr.xpdustry.nucleus.common.application.NucleusApplication.Cause;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.common.message.MessageService;
import fr.xpdustry.nucleus.common.version.NucleusVersion;
import fr.xpdustry.nucleus.common.version.UpdateMessage;
import fr.xpdustry.nucleus.common.version.UpdateService;
import fr.xpdustry.nucleus.discord.interaction.InteractionContext;
import fr.xpdustry.nucleus.discord.interaction.InteractionDescription;
import fr.xpdustry.nucleus.discord.interaction.InteractionPermission;
import fr.xpdustry.nucleus.discord.interaction.SlashInteraction;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import org.javacord.api.entity.permission.PermissionType;

@SlashInteraction("update")
@InteractionDescription("Updates the bot.")
@InteractionPermission(PermissionType.ADMINISTRATOR)
public final class UpdateCommand implements NucleusListener {

    private final NucleusApplication application;
    private final UpdateService updater;
    private final HttpClient httpClient;
    private final MessageService messenger;
    private final Gson gson = new Gson();

    @Inject
    public UpdateCommand(
            final NucleusApplication application,
            final UpdateService updater,
            final @NucleusExecutor Executor executor,
            final MessageService messenger) {
        this.application = application;
        this.updater = updater;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(5L))
                .executor(executor)
                .build();
        this.messenger = messenger;
    }

    @SlashInteraction.Handler
    public void onUpdate(final InteractionContext context) {
        context.sendEphemeralMessage("Retrieving latest version...")
                .thenCompose(updater -> httpClient
                        .sendAsync(
                                HttpRequest.newBuilder()
                                        .uri(URI.create(
                                                "https://api.github.com/repos/Xpdustry/Nucleus/releases/latest"))
                                        .GET()
                                        .build(),
                                HttpResponse.BodyHandlers.ofString())
                        .thenApply(response -> NucleusVersion.parse(gson.fromJson(response.body(), JsonObject.class)
                                .get("tag_name")
                                .getAsString()))
                        .thenCompose(version -> version.isNewerThan(this.application.getVersion())
                                ? updater.setContent("Found version " + version + ", performing update." + "..")
                                        .update()
                                        .thenCombine(this.updater.update(version), (message, unused) -> message)
                                        .thenCompose(message -> updater.setContent("Update complete! Closing server.")
                                                .update()
                                                .thenRun(() -> this.messenger.publish(UpdateMessage.of(version)))
                                                .thenRun(() -> this.application.exit(Cause.RESTART)))
                                : updater.setContent("No update available.")
                                        .update()
                                        .thenRun(() -> {}))) // This empty runnable makes me cringe so hard
                .exceptionally(throwable -> {
                    context.sendEphemeralMessage("An error occurred while updating: " + throwable.getMessage());
                    return null;
                });
    }
}
