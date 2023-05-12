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
import mindustry.world.blocks.distribution.MassDriver.MassDriverBuild;
import mindustry.world.blocks.payloads.PayloadMassDriver;
import mindustry.world.blocks.payloads.PayloadMassDriver.PayloadDriverBuild;

public final class PayloadMassDriverConfigurationFactory extends LinkableBlockConfigurationFactory<PayloadDriverBuild> {

    @Override
    protected boolean isLinkValid(final PayloadDriverBuild building, final int x, final int y) {
        return Vars.world.build(Point2.pack(x, y)) instanceof MassDriverBuild other
                && building.block == other.block
                && building.team == other.team
                && building.within(other, ((PayloadMassDriver) building.block).range);
    }
}
