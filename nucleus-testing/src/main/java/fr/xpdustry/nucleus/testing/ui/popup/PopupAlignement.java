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
package fr.xpdustry.nucleus.testing.ui.popup;

import arc.util.Align;

public enum PopupAlignement {
    TOP_LEFT(Align.topLeft),
    TOP(Align.top),
    TOP_RIGHT(Align.topRight),
    LEFT(Align.left),
    CENTER(Align.center),
    RIGHT(Align.right),
    BOTTOM_LEFT(Align.bottomLeft),
    BOTTOM(Align.bottom),
    BOTTOM_RIGHT(Align.bottomRight);

    private final int alignement;

    PopupAlignement(final int alignement) {
        this.alignement = alignement;
    }

    public int getArcAlign() {
        return alignement;
    }
}
