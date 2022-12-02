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
package fr.xpdustry.nucleus.testing.ui;

import mindustry.gen.Player;

public final class TransformContext<M extends Pane.Mutable> {

    private final State state;
    private final M pane;
    private final Player player;

    public static <M extends Pane.Mutable> TransformContext<M> of(
            final State state, final M pane, final Player player) {
        return new TransformContext<>(state, pane, player);
    }

    private TransformContext(final State state, final M pane, final Player player) {
        this.state = state;
        this.pane = pane;
        this.player = player;
    }

    public State getState() {
        return state;
    }

    public M getPane() {
        return pane;
    }

    public Player getPlayer() {
        return player;
    }
}
