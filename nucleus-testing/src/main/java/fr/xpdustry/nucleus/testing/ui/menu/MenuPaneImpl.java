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

import java.util.Arrays;
import java.util.List;

final class MenuPaneImpl implements MenuPane.Mutable {

    private static final MenuOption[][] EMPTY_OPTIONS = new MenuOption[0][0];

    private String title = "";
    private String content = "";
    MenuOption[][] options = EMPTY_OPTIONS;

    @Override
    public boolean isEmpty() {
        return title.isEmpty() && content.isEmpty() && options.length == 0;
    }

    @Override
    public void clear() {
        title = "";
        content = "";
        options = EMPTY_OPTIONS;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(final String title) {
        this.title = title;
    }

    @Override
    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public List<MenuOption> getOptionsAsList() {
        return Arrays.stream(options).flatMap(Arrays::stream).toList();
    }

    @Override
    public MenuOption[][] getOptionAsGrid() {
        return copy(options);
    }

    @Override
    public MenuOption getOption(int x, int y) {
        return options[y][x];
    }

    @Override
    public MenuOption getOption(int id) {
        int i = 0;
        for (final var row : options) {
            i += row.length;
            if (i > id) {
                return row[id - i + row.length];
            }
        }
        throw new IllegalArgumentException("The id is higher than " + getOptions());
    }

    @Override
    public List<MenuOption> getOptionRowAsList(int y) {
        return List.of(options[y]);
    }

    @Override
    public int getOptions() {
        int size = 0;
        for (final var row : options) {
            size += row.length;
        }
        return size;
    }

    @Override
    public int getRows() {
        return options.length;
    }

    @Override
    public int getColumns(int y) {
        return options[y].length;
    }

    @Override
    public void setOptions(MenuOption[][] options) {
        this.options = copy(options);
    }

    @Override
    public void setOption(int x, int y, MenuOption option) {
        options[y][x] = option;
    }

    @Override
    public void setOption(int id, MenuOption option) {
        int i = 0;
        for (final var row : options) {
            i += row.length;
            if (i > id) {
                row[id - i + row.length] = option;
                return;
            }
        }
        throw new IllegalArgumentException("The id is higher than " + getOptions());
    }

    @Override
    public void setOptionRow(int y, List<MenuOption> options) {
        this.options[y] = options.toArray(MenuOption[]::new);
    }

    @Override
    public void setRows(int rows) {
        options = Arrays.copyOf(options, rows);
    }

    @Override
    public void setColumns(int y, int columns) {
        options[y] = Arrays.copyOf(options[y], columns);
    }

    @Override
    public void addOptionRow(MenuOption... options) {
        addOptionRow(List.of(options));
    }

    @Override
    public void addOptionRow(final List<MenuOption> options) {
        this.options = Arrays.copyOf(this.options, this.options.length + 1);
        this.options[this.options.length - 1] = options.toArray(MenuOption[]::new);
    }

    @Override
    public void addOptionRow(int columns) {
        final var row = new MenuOption[columns];
        Arrays.fill(row, MenuOption.empty());
        addOptionRow(row);
    }

    @Override
    public void addOption(int y, MenuOption option) {
        options[y] = Arrays.copyOf(options[y], options[y].length + 1);
        options[y][options[y].length - 1] = option;
    }

    private MenuOption[][] copy(final MenuOption[][] options) {
        final var copy = new MenuOption[options.length][];
        for (int i = 0; i < options.length; i++) {
            copy[i] = Arrays.copyOf(options[i], options[i].length);
        }
        return copy;
    }
}
