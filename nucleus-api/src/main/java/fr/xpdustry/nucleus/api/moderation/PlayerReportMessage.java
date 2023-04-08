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
package fr.xpdustry.nucleus.api.moderation;

import fr.xpdustry.nucleus.api.annotation.NucleusStyle;
import fr.xpdustry.nucleus.api.message.Message;
import org.immutables.value.Value;

@Value.Immutable
@NucleusStyle
public sealed interface PlayerReportMessage extends Message permits ImmutablePlayerReportMessage {

    static PlayerReportMessage.Builder builder() {
        return ImmutablePlayerReportMessage.builder();
    }

    String getServerIdentifier();

    String getReporterName();

    String getReportedPlayerName();

    String getReportedPlayerIp();

    String getReportedPlayerUuid();

    String getReason();

    sealed interface Builder permits ImmutablePlayerReportMessage.Builder {

        Builder setServerIdentifier(final String serverIdentifier);

        Builder setReporterName(final String reporterName);

        Builder setReportedPlayerName(final String reportedPlayerName);

        Builder setReportedPlayerIp(final String reportedPlayerIp);

        Builder setReportedPlayerUuid(final String reportedPlayerUuid);

        Builder setReason(final String reason);

        PlayerReportMessage build();
    }
}
