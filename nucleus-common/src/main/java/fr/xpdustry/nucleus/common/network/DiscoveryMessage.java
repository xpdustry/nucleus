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
package fr.xpdustry.nucleus.common.network;

import fr.xpdustry.nucleus.common.annotation.ImmutableNucleusStyle;
import fr.xpdustry.nucleus.common.message.Message;
import fr.xpdustry.nucleus.common.version.NucleusVersion;
import java.util.Optional;
import org.immutables.value.Value.Immutable;

@Immutable(copy = false)
@ImmutableNucleusStyle
public sealed interface DiscoveryMessage extends Message permits ImmutableDiscoveryMessage {

    static DiscoveryMessage.Builder builder() {
        return ImmutableDiscoveryMessage.builder();
    }

    String getServerIdentifier();

    Optional<MindustryServerInfo> getServerInfo();

    NucleusVersion getNucleusVersion();

    Type getType();

    enum Type {
        DISCOVERY,
        HEARTBEAT,
        DISCONNECT
    }

    sealed interface Builder permits ImmutableDiscoveryMessage.Builder {

        Builder setServerIdentifier(final String serverIdentifier);

        Builder setServerInfo(final MindustryServerInfo serverInfo);

        Builder setNucleusVersion(final NucleusVersion nucleusVersion);

        Builder setType(final Type type);

        DiscoveryMessage build();
    }
}
