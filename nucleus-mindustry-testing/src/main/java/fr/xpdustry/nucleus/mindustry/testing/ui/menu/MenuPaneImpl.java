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

import java.util.Arrays;

final class MenuPaneImpl implements MenuPane {

    private static final MenuOption[][] EMPTY_OPTIONS = new MenuOption[0][0];

    private String title = "";
    private String content = "";
    private MenuOption[][] options = EMPTY_OPTIONS;

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(final String title) {
        this.title = title;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public void setContent(final String content) {
        this.content = content;
    }

    @Override
    public MenuOption[][] getOptions() {
        return copy(options);
    }

    @Override
    public void setOptions(final MenuOption[][] options) {
        this.options = copy(options);
    }

    @Override
    public MenuOption getOption(final int x, final int y) {
        return options[y][x];
    }

    @Override
    public MenuOption getOption(final int id) {
        int i = 0;
        for (final var row : options) {
            i += row.length;
            if (i > id) {
                return row[id - i + row.length];
            }
        }
        throw new IllegalArgumentException("The id is invalid.");
    }

    @Override
    public void setOption(final int x, final int y, final MenuOption option) {
        this.options[y][x] = option;
    }

    @Override
    public MenuOption[] getOptionRow(final int y) {
        return copy(options[y]);
    }

    @Override
    public void setOptionRow(final int y, final MenuOption... options) {
        this.options[y] = copy(options);
    }

    @Override
    public void addOptionRow(final MenuOption... options) {
        this.options = Arrays.copyOf(this.options, this.options.length + 1);
        this.options[this.options.length - 1] = copy(options);
    }

    private MenuOption[][] copy(final MenuOption[][] options) {
        final var copy = new MenuOption[options.length][];
        for (int i = 0; i < options.length; i++) {
            copy[i] = Arrays.copyOf(options[i], options[i].length);
        }
        return copy;
    }

    private MenuOption[] copy(final MenuOption[] options) {
        return Arrays.copyOf(options, options.length);
    }

    @Override
    public void clear() {
        title = "";
        content = "";
        options = EMPTY_OPTIONS;
    }
}
