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
import fr.xpdustry.distributor.api.util.MUUID;
import fr.xpdustry.nucleus.mindustry.testing.ui.AbstractTransformingInterface;
import fr.xpdustry.nucleus.mindustry.testing.ui.View;
import fr.xpdustry.nucleus.mindustry.testing.ui.state.State;
import java.util.HashMap;
import java.util.Map;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.ui.Menus;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TextInputInterfaceImpl extends AbstractTransformingInterface<TextInputPane> implements TextInputInterface {

    private final Map<MUUID, TextInputView> views = new HashMap<>();
    private final int id;

    public TextInputInterfaceImpl(final MindustryPlugin plugin) {
        super(plugin);
        this.id = Menus.registerTextInput((player, text) -> {
            final var view = this.views.get(MUUID.of(player));
            if (view != null) {
                view.getPane().getAction().accept(view, text);
                view.close();
            }
        });
    }

    @Override
    protected TextInputPane createPane() {
        return new TextInputPaneImpl();
    }

    @Override
    public View open(final Player viewer, final State state, final @Nullable View parent) {
        final var view = views.computeIfAbsent(MUUID.of(viewer), p -> new TextInputView(viewer, parent));
        view.setState(state);
        view.update();
        return view;
    }

    private final class TextInputView extends AbstractView {

        private TextInputView(final Player viewer, final @Nullable View parent) {
            super(viewer, parent);
        }

        @Override
        public boolean isOpen() {
            return TextInputInterfaceImpl.this.views.containsKey(MUUID.of(getViewer()));
        }

        @Override
        public void update() {
            transform(this);
            Call.textInput(
                    getViewer().con(),
                    TextInputInterfaceImpl.this.id,
                    getPane().getTitle(),
                    getPane().getMessage(),
                    getPane().getTextMaxLength(),
                    getPane().getDefaultValue(),
                    getPane().isNumeric());
        }

        @Override
        public void close() {
            TextInputInterfaceImpl.this.views.remove(MUUID.of(getViewer()));
        }
    }
}
