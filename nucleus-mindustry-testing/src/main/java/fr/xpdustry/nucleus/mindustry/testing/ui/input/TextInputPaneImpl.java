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
package fr.xpdustry.nucleus.mindustry.testing.ui.input;

final class TextInputPaneImpl implements TextInputPane {

    private String title = "";
    private String message = "";
    private String defaultValue = "";

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public TextInputPane setTitle(final String title) {
        this.title = title;
        return this;
    }

    @Override
    public String getContent() {
        return message;
    }

    @Override
    public TextInputPane setContent(final String content) {
        this.message = content;
        return this;
    }

    @Override
    public String getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public TextInputPane setDefaultValue(final String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }
}
