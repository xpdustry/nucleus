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
package fr.xpdustry.nucleus.common.util;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class URIBuilder {
    private final Map<String, String> parameters = new HashMap<>();
    private final String base;

    private URIBuilder(final String base) {
        this.base = base;
    }

    public static URIBuilder of(final String base) {
        return new URIBuilder(base);
    }

    public URIBuilder withParameter(final String parameter, final String value) {
        this.parameters.put(parameter, value);
        return this;
    }

    public URI build() {
        final var builder = new StringBuilder(base.length() * 2).append(base);
        var first = true;
        for (final var parameter : this.parameters.entrySet()) {
            builder.append(first ? '?' : '&');
            first = false;
            builder.append(URLEncoder.encode(parameter.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(parameter.getValue(), StandardCharsets.UTF_8));
        }
        return URI.create(builder.toString());
    }
}
