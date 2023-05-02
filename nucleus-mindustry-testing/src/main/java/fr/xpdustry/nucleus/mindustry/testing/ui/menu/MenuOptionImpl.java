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

final class MenuOptionImpl implements MenuOption {

    static final MenuOption EMPTY = new MenuOptionImpl("", Action.none());

    private final String content;
    private final Action action;

    MenuOptionImpl(final String content, final Action action) {
        this.content = content;
        this.action = action;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public Action getAction() {
        return action;
    }
}
