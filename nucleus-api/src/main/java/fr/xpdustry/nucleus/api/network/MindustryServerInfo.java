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
package fr.xpdustry.nucleus.api.network;

import fr.xpdustry.nucleus.api.annotation.NucleusStyle;
import fr.xpdustry.nucleus.api.application.MindustryVersion;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@NucleusStyle
public sealed interface MindustryServerInfo permits ImmutableMindustryServerInfo {

    static MindustryServerInfo.Builder builder() {
        return ImmutableMindustryServerInfo.builder();
    }

    String getName();

    String getHost();

    int getPort();

    String getMapName();

    String getDescription();

    int getWave();

    int getPlayerCount();

    int getPlayerLimit();

    MindustryVersion getGameVersion();

    GameMode getGameMode();

    Optional<String> getGameModeName();

    enum GameMode {
        SURVIVAL,
        SANDBOX,
        ATTACK,
        PVP,
        EDITOR
    }

    sealed interface Builder permits ImmutableMindustryServerInfo.Builder {

        Builder setName(final String name);

        Builder setHost(final String host);

        Builder setPort(final int port);

        Builder setMapName(final String mapName);

        Builder setDescription(final String description);

        Builder setWave(final int wave);

        Builder setPlayerCount(final int playerCount);

        Builder setPlayerLimit(final int playerLimit);

        Builder setGameVersion(final MindustryVersion gameVersion);

        Builder setGameMode(final GameMode gameMode);

        Builder setGameModeName(final String gameModeName);

        MindustryServerInfo build();
    }
}
