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
package fr.xpdustry.nucleus.common.application;

import fr.xpdustry.nucleus.common.version.NucleusVersion;
import java.nio.file.Path;

// TODO Should this class be the injector ?
public interface NucleusApplication {

    void init();

    void exit(final Cause cause);

    void register(final NucleusListener listener);

    NucleusVersion getVersion();

    NucleusPlatform getPlatform();

    Path getDataDirectory();

    Path getApplicationJar();

    enum Cause {
        SHUTDOWN,
        RESTART
    }
}
