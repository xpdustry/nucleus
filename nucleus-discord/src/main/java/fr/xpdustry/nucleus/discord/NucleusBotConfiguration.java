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
package fr.xpdustry.nucleus.discord;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "fr.xpdustry.nucleus.discord")
@ConstructorBinding
public class NucleusBotConfiguration {
    private final String token;
    private final String prefix;
    private final @NestedConfigurationProperty ChannelsConfig channels;
    private final @NestedConfigurationProperty JavelinConfig javelin;

    public NucleusBotConfiguration(
            final String token, final String prefix, final ChannelsConfig channels, final JavelinConfig javelin) {
        this.token = token;
        this.prefix = prefix;
        this.channels = channels;
        this.javelin = javelin;
    }

    public String getToken() {
        return token;
    }

    public String getPrefix() {
        return prefix;
    }

    public ChannelsConfig getChannels() {
        return channels;
    }

    public JavelinConfig getJavelin() {
        return javelin;
    }

    public static class ChannelsConfig {

        private final long reports;
        private final long servers;

        public ChannelsConfig(long reports, long servers) {
            this.reports = reports;
            this.servers = servers;
        }

        public long getReports() {
            return reports;
        }

        public long getServers() {
            return servers;
        }
    }

    public static class JavelinConfig {

        private final int port;

        public JavelinConfig(int port) {
            this.port = port;
        }

        public int getPort() {
            return port;
        }
    }
}
