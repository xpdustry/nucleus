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

import fr.xpdustry.nucleus.testing.ui.Pane;
import java.util.List;

public interface MenuPane extends Pane {

    String getTitle();

    String getContent();

    List<MenuOption> getOptionsAsList();

    MenuOption[][] getOptionAsGrid();

    MenuOption getOption(final int x, final int y);

    MenuOption getOption(final int id);

    List<MenuOption> getOptionRowAsList(final int y);

    int getOptions();

    int getRows();

    int getColumns(final int y);

    // TODO My god, this is ugly, a cleanup is needed
    interface Mutable extends MenuPane, Pane.Mutable {

        void setTitle(final String title);

        void setContent(final String content);

        void setOptions(final MenuOption[][] options);

        void setOption(final int x, final int y, final MenuOption option);

        void setOption(final int id, final MenuOption option);

        void setOptionRow(final int y, final List<MenuOption> options);

        void setRows(final int rows);

        void setColumns(final int y, final int columns);

        void addOptionRow(final List<MenuOption> options);

        void addOptionRow(final MenuOption... options);

        void addOptionRow(final int columns);

        void addOption(final int y, final MenuOption option);
    }

    /*


    public MenuPane setOption(final int x, final int y, final MenuOption option) {
      check(x, y);
      final var copy = getOptionsCopy();
      copy[y] = Arrays.copyOf(this.options[y], this.options[y].length);
      copy[y][x] = option;
      return createNewPane(this.options);
    }

    public MenuPane removeOption(final int x, final int y) {
      check(x, y);
      final var copy = getOptionsCopy();
      copy[y] = Magik.removeElementFromArray(copy[y], x);
      return createNewPane(copy);
    }

    public List<MenuOption> getOptionRowAsList(final int y) {
      check(y);
      return List.of(this.options[y]);
    }

    public MenuPane setOptionRow(final int y, MenuOption... row) {
      check(y);
      final var copy = getOptionsCopy();
      copy[y] = Arrays.copyOf(row, row.length);
      return createNewPane(this.options);
    }

    public MenuPane setOptionRow(final int y, Iterable<MenuOption> options) {
      check(y);
      final var copy = getOptionsCopy();
      copy[y] = options instanceof Collection<MenuOption> collection
        ? collection.toArray(MenuOption[]::new)
        : Seq.with(options).toArray();
      return createNewPane(copy);
    }

    public MenuPane addOptionRow(final MenuOption... row) {
      final var copy = Arrays.copyOf(this.options, this.options.length + 1);
      copy[copy.length - 1] = Arrays.copyOf(row, row.length);
      return new MenuPane(this.title, this.content, copy);
    }

    public MenuPane addOptionRow(final Iterable<MenuOption> options) {
      final MenuOption[] row = options instanceof Collection<MenuOption> collection
        ? collection.toArray(MenuOption[]::new)
        : Seq.with(options).toArray();
      final var copy = Arrays.copyOf(this.options, this.options.length + 1);
      copy[copy.length - 1] = row;
      return new MenuPane(this.title, this.content, copy);
    }

    public MenuPane removeOptionRow(final int y) {
      check(y);
      return createNewPane(Magik.removeElementFromArray(this.options, y));
    }

       */
}
