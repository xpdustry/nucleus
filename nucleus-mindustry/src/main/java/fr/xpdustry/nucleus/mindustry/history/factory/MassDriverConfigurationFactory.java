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
package fr.xpdustry.nucleus.mindustry.history.factory;

import arc.math.geom.Point2;
import mindustry.Vars;
import mindustry.world.blocks.distribution.MassDriver;
import mindustry.world.blocks.distribution.MassDriver.MassDriverBuild;

public final class MassDriverConfigurationFactory extends LinkableBlockConfigurationFactory<MassDriverBuild> {

    @Override
    protected boolean isLinkValid(final MassDriverBuild building, final int x, final int y) {
        if (Point2.pack(x, y) == -1) {
            return false;
        }
        return Vars.world.build(Point2.pack(x, y)) instanceof MassDriverBuild other
                && building.block == other.block
                && building.team == other.team
                && building.within(other, ((MassDriver) building.block).range);
    }
}
