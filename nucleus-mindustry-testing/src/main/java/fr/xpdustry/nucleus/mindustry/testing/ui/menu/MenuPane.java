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

import fr.xpdustry.nucleus.mindustry.testing.ui.Pane;

public interface MenuPane extends Pane {

    String getTitle();

    void setTitle(final String title);

    String getContent();

    void setContent(final String content);

    MenuOption[][] getOptions();

    void setOptions(final MenuOption[][] options);

    MenuOption getOption(final int x, final int y);

    MenuOption getOption(final int id);

    void setOption(final int x, final int y, final MenuOption option);

    MenuOption[] getOptionRow(final int y);

    void setOptionRow(final int y, final MenuOption... options);

    void addOptionRow(final MenuOption... options);
}
