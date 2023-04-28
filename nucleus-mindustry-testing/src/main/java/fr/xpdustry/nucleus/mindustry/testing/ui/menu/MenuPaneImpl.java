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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

final class MenuPaneImpl implements MenuPane {

    private final List<List<MenuOption>> options = new ArrayList<>();
    private String title = "";
    private String content = "";

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public MenuPane setTitle(final String title) {
        this.title = title;
        return this;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public MenuPane setContent(final String content) {
        this.content = content;
        return this;
    }

    @Override
    public List<List<MenuOption>> getOptions() {
        return Collections.unmodifiableList(options);
    }

    @Override
    public MenuPane setOptions(final List<List<MenuOption>> options) {
        this.options.clear();
        this.options.addAll(options.stream().map(List::copyOf).toList());
        return this;
    }

    @Override
    public Optional<List<MenuOption>> getOptionRow(final int y) {
        if (y > 0 && y < options.size()) {
            return Optional.of(options.get(y));
        }
        return Optional.empty();
    }

    @Override
    public Optional<MenuOption> getOption(final int x, final int y) {
        if (y > 0 && y < options.size()) {
            final var row = options.get(y);
            if (x > 0 && x < row.size()) {
                return Optional.of(row.get(x));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<MenuOption> getOption(final int id) {
        int i = 0;
        for (final var row : options) {
            i += row.size();
            if (i > id) {
                return Optional.of(row.get(id - i + row.size()));
            }
        }
        return Optional.empty();
    }

    @Override
    public MenuPane addOptionRow(final Collection<MenuOption> options) {
        this.options.add(List.copyOf(options));
        return this;
    }
}
