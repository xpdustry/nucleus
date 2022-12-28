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
package fr.xpdustry.nucleus.core;

import fr.xpdustry.nucleus.core.util.NucleusPlatform;
import fr.xpdustry.nucleus.core.util.NucleusVersion;
import org.slf4j.Logger;

public interface NucleusApplication {

    NucleusVersion getVersion();

    NucleusPlatform getPlatform();

    NucleusConfiguration getConfiguration();

    Logger getLogger();
}
