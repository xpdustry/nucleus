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

import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.nucleus.mindustry.testing.ui.TransformerInterface;
import fr.xpdustry.nucleus.mindustry.testing.ui.action.Action;
import fr.xpdustry.nucleus.mindustry.testing.ui.action.BiAction;

public interface TextInputInterface extends TransformerInterface<TextInputInterface, TextInputPane> {

    static TextInputInterface create(final MindustryPlugin plugin) {
        return new TextInputInterfaceImpl(plugin);
    }

    int getMaxInputLength();

    TextInputInterface setMaxInputLength(final int maxInputLength);

    BiAction<String> getInputAction();

    TextInputInterface setInputAction(final BiAction<String> inputAction);

    Action getExitAction();

    TextInputInterface setExitAction(final Action exitAction);
}
