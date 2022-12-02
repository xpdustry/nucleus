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
package fr.xpdustry.nucleus.testing.ui.menu;

import fr.xpdustry.nucleus.testing.ui.Action;

public final class MenuOption {

    private static final MenuOption EMPTY = new MenuOption("", Action.none());

    private final String content;
    private final Action<MenuView> action;

    private MenuOption(final String content, final Action<MenuView> action) {
        this.content = content;
        this.action = action;
    }

    public static MenuOption empty() {
        return EMPTY;
    }

    public static MenuOption of(final String content, final Action<MenuView> action) {
        return new MenuOption(content, action);
    }

    public static MenuOption of(final char icon, final Action<MenuView> action) {
        return new MenuOption(String.valueOf(icon), action);
    }

    public String getContent() {
        return content;
    }

    public Action<MenuView> getAction() {
        return action;
    }
}
