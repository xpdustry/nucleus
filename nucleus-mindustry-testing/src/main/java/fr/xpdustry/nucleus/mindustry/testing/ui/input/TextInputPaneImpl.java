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

import fr.xpdustry.nucleus.mindustry.testing.ui.action.Action;
import fr.xpdustry.nucleus.mindustry.testing.ui.action.BiAction;

final class TextInputPaneImpl implements TextInputPane {

    private String title = "";
    private String message = "";
    private int textMaxLength = 64;
    private String defaultValue = "";
    private boolean numeric = false;
    private BiAction<String> action = Action.close().asBiAction();

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(final String title) {
        this.title = title;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public int getTextMaxLength() {
        return textMaxLength;
    }

    @Override
    public void setTextMaxLength(int textMaxLength) {
        this.textMaxLength = textMaxLength;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean isNumeric() {
        return numeric;
    }

    @Override
    public void setNumeric(boolean numeric) {
        this.numeric = numeric;
    }

    @Override
    public BiAction<String> getAction() {
        return action;
    }

    @Override
    public void setAction(BiAction<String> action) {
        this.action = action;
    }

    @Override
    public void clear() {
        this.title = "";
        this.message = "";
        this.textMaxLength = 64;
        this.defaultValue = "";
        this.numeric = false;
        this.action = Action.close().asBiAction();
    }
}
