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
package fr.xpdustry.nucleus.mindustry.testing.ui.menu;

import fr.xpdustry.nucleus.mindustry.testing.ui.action.Action;

public interface MenuOption {

    static MenuOption empty() {
        return MenuOptionImpl.EMPTY;
    }

    static MenuOption of(final String content, final Action action) {
        return new MenuOptionImpl(content, action);
    }

    static MenuOption of(final char icon, final Action action) {
        return new MenuOptionImpl(String.valueOf(icon), action);
    }

    Action getAction();

    String getContent();
}
