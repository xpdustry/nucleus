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
package fr.xpdustry.nucleus.common.bridge;

import fr.xpdustry.nucleus.common.annotation.ImmutableNucleusStyle;
import fr.xpdustry.nucleus.common.application.NucleusPlatform;
import fr.xpdustry.nucleus.common.message.Message;
import java.util.Optional;
import org.immutables.value.Value.Immutable;

// TODO Turn into a service class
@Immutable(copy = false)
@ImmutableNucleusStyle
public sealed interface PlayerActionMessage extends Message permits ImmutablePlayerActionMessage {

    static PlayerActionMessage.Builder builder() {
        return ImmutablePlayerActionMessage.builder();
    }

    String getServerIdentifier();

    String getPlayerName();

    Optional<String> getMessage();

    Type getType();

    NucleusPlatform getOrigin();

    enum Type {
        JOIN,
        CHAT,
        QUIT,
    }

    sealed interface Builder permits ImmutablePlayerActionMessage.Builder {

        Builder setServerIdentifier(final String serverIdentifier);

        Builder setPlayerName(final String playerName);

        Builder setMessage(final String message);

        Builder setType(final Type type);

        Builder setOrigin(final NucleusPlatform origin);

        PlayerActionMessage build();
    }
}
