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

    Optional<MenuOption> getOption(final int x, final int y);

    Optional<MenuOption> getOption(final int id);

    MenuPane setOption(final int x, final int y, final MenuOption option);

    MenuPane addOption(final int x, final int y, final MenuOption option);

    Optional<List<MenuOption>> getOptionRow(final int y);

    MenuPane setOptionRow(final int y, final Collection<MenuOption> options);

    default MenuPane setOptionRow(final int y, final MenuOption... options) {
        return setOptionRow(y, List.of(options));
    }

    MenuPane addOptionRow(final Collection<MenuOption> options);

    default MenuPane addOptionRow(final MenuOption... options) {
        return addOptionRow(List.of(options));
    }
}
