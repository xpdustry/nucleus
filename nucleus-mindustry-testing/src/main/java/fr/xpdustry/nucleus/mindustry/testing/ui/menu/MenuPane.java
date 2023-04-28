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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MenuPane extends Pane {

    String getTitle();

    MenuPane setTitle(final String title);

    String getContent();

    MenuPane setContent(final String content);

    List<List<MenuOption>> getOptions();

    MenuPane setOptions(final List<List<MenuOption>> options);

    Optional<List<MenuOption>> getOptionRow(final int y);

    Optional<MenuOption> getOption(final int x, final int y);

    Optional<MenuOption> getOption(final int id);

    MenuPane addOptionRow(final Collection<MenuOption> options);

    default MenuPane addOptionRow(final MenuOption... options) {
        return addOptionRow(List.of(options));
    }
}
