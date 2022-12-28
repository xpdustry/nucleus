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
package fr.xpdustry.nucleus.mindustry.service;

import arc.ApplicationListener;
import arc.Core;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.nucleus.core.event.AutoUpdateEvent;
import fr.xpdustry.nucleus.core.messages.ImmutableVersionRequest;
import fr.xpdustry.nucleus.core.util.AutoUpdateHelper;
import fr.xpdustry.nucleus.core.util.NucleusVersion;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import mindustry.Vars;

public final class AutoUpdateService extends AutoUpdateHelper implements PluginListener {

    public AutoUpdateService(final NucleusPlugin nucleus) {
        super(nucleus);
    }

    @Override
    public void onPluginLoad() {
        getNucleus().getMessenger().subscribe(AutoUpdateEvent.class, event -> onAutoUpdateStart(event.version()));
        // Delay because Javelin need some time to connect (Why I even made it async by default ;-;)
        getNucleus().getScheduler().schedule().delay(5, TimeUnit.SECONDS).execute(this::onAutoUpdateCheckStart);
    }

    @Override
    protected NucleusVersion getLatestVersion() {
        return getNucleus()
                .getMessenger()
                .request(ImmutableVersionRequest.of())
                .exceptionally(throwable -> {
                    getNucleus().getLogger().error("Failed to check for latest update", throwable);
                    return getNucleus().getVersion();
                })
                .join();
    }

    @Override
    protected String getArtifactName() {
        return "NucleusMindustry.jar";
    }

    @Override
    protected Path getApplicationJarLocation() {
        return Vars.mods.getMod(getNucleus().getClass()).file.file().toPath();
    }

    @Override
    protected void onAutoUpdateFinished() {
        Core.app.exit();
        Core.app.addListener(new ApplicationListener() {
            @Override
            public void dispose() {
                Core.settings.autosave();
                System.exit(2);
            }
        });
    }

    @Override
    protected NucleusPlugin getNucleus() {
        return (NucleusPlugin) super.getNucleus();
    }
}
