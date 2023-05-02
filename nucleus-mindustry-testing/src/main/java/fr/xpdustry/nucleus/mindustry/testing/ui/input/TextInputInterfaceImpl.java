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
import fr.xpdustry.nucleus.mindustry.testing.ui.AbstractTransformerInterface;
import fr.xpdustry.nucleus.mindustry.testing.ui.action.Action;
import fr.xpdustry.nucleus.mindustry.testing.ui.action.BiAction;
import java.util.HashSet;
import java.util.Set;
import mindustry.gen.Call;
import mindustry.ui.Menus;

final class TextInputInterfaceImpl extends AbstractTransformerInterface<TextInputInterface, TextInputPane>
        implements TextInputInterface {

    private final Set<MUUID> visible = new HashSet<>();
    private final int id;
    private int maxInputLength = 64;
    private BiAction<String> inputAction = Action.none().asBiAction();
    private Action exitAction = Action.back();

    TextInputInterfaceImpl(final MindustryPlugin plugin) {
        super(plugin);

        this.id = Menus.registerTextInput((player, text) -> {
            final var view = this.getView(player);
            if (view == null) {
                this.getPlugin()
                        .getLogger()
                        .warn(
                                "Received text input from player {} (uuid: {}) but no view was found",
                                player.plainName(),
                                player.uuid());
                return;
            }

            // Simple trick to not reopen an interface when an action already does it.
            this.visible.remove(MUUID.of(player));
            if (text == null) {
                this.exitAction.accept(view);
            } else if (text.length() > this.maxInputLength) {
                this.getPlugin()
                        .getLogger()
                        .warn(
                                "Received text input from player {} (uuid: {}) with length {} but the maximum length is {}",
                                player.plainName(),
                                player.uuid(),
                                text.length(),
                                this.maxInputLength);
                view.close();
            } else {
                this.inputAction.accept(view, text);
            }
            // The text input closes automatically when the player presses enter,
            // so reopen if it was not explicitly closed by the server.
            if (view.isOpen() && !this.visible.contains(MUUID.of(player))) {
                view.open();
            }
        });
    }

    @Override
    protected TextInputPane createPane() {
        return new TextInputPaneImpl();
    }

    @Override
    protected void onViewOpen(final SimpleView view) {
        if (this.visible.add(MUUID.of(view.getViewer()))) {
            Call.textInput(
                    view.getViewer().con(),
                    TextInputInterfaceImpl.this.id,
                    view.getPane().getTitle(),
                    view.getPane().getContent(),
                    this.maxInputLength,
                    view.getPane().getDefaultValue(),
                    false);
        }
    }

    @Override
    protected void onViewClose(final SimpleView view) {
        this.visible.remove(MUUID.of(view.getViewer()));
    }

    @Override
    public int getMaxInputLength() {
        return this.maxInputLength;
    }

    @Override
    public TextInputInterface setMaxInputLength(final int maxInputLength) {
        this.maxInputLength = maxInputLength;
        return this;
    }

    @Override
    public BiAction<String> getInputAction() {
        return this.inputAction;
    }

    @Override
    public TextInputInterface setInputAction(final BiAction<String> inputAction) {
        this.inputAction = inputAction;
        return this;
    }

    @Override
    public Action getExitAction() {
        return exitAction;
    }

    @Override
    public TextInputInterface setExitAction(final Action exitAction) {
        this.exitAction = exitAction;
        return this;
    }
}
