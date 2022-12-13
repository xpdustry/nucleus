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

import fr.xpdustry.nucleus.testing.ui.Pane;

public interface PopupPane extends Pane {

    String getContent();

    PopupPane setContent(final String content);

    int getShiftX();

    PopupPane setShiftX(final int shiftX);

    int getShiftY();

    PopupPane setShiftY(final int shiftY);

    PopupAlignement getAlignement();

    PopupPane setAlignement(final PopupAlignement alignement);
}
