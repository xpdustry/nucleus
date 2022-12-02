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

    int getShiftX();

    int getShiftY();

    PopupAlignement getAlignement();

    @Override
    default boolean isEmpty() {
        return getContent().isEmpty();
    }

    interface Mutable extends PopupPane, Pane.Mutable {

        void setContent(final String content);

        void setShiftX(final int shiftX);

        void setShiftY(final int shiftY);

        void setAlignement(final PopupAlignement alignement);

        @Override
        default void clear() {
            this.setContent("");
            this.setShiftX(0);
            this.setShiftY(0);
            this.setAlignement(PopupAlignement.CENTER);
        }
    }
}
