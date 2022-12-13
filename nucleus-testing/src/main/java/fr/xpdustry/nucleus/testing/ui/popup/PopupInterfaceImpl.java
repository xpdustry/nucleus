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
package fr.xpdustry.nucleus.testing.ui.popup;

import arc.util.Interval;
import arc.util.Time;
import fr.xpdustry.distributor.api.util.MoreEvents;
import fr.xpdustry.nucleus.testing.ui.State;
import fr.xpdustry.nucleus.testing.ui.Transform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Player;

final class PopupInterfaceImpl implements PopupInterface {

    private final List<Transform<PopupView, PopupPane>> transformers = new ArrayList<>();
    private final Interval interval = new Interval();
    private final List<PopupViewImpl> views = new ArrayList<>();
    private int updateInterval = 60;

    {
        MoreEvents.subscribe(EventType.PlayerLeave.class, event -> {
            views.removeIf(view -> view.getViewer().uuid().equals(event.player.uuid()));
        });
    }

    @Override
    public void onPluginUpdate() {
        if (!interval.get(updateInterval)) {
            return;
        }
        for (final var view : views) {
            for (final var transformer : transformers) {
                view.pane = transformer.apply(view);
            }
            Call.infoPopup(
                    view.getViewer().con(),
                    view.getPane().getContent(),
                    (Time.delta / 60F) * PopupInterfaceImpl.this.updateInterval,
                    view.getPane().getAlignement().getArcAlign(),
                    Math.max(view.getPane().getShiftY(), 0),
                    Math.max(view.getPane().getShiftX(), 0),
                    Math.min(view.getPane().getShiftY(), 0),
                    Math.min(view.getPane().getShiftX(), 0));
        }
    }

    @Override
    public PopupView open(final Player viewer, final State state) {
        final var view = new PopupViewImpl(viewer, state.copy());
        views.add(view);
        return view;
    }

    @Override
    public void addTransformer(final Transform<PopupView, PopupPane> transformer) {
        this.transformers.add(transformer);
    }

    @Override
    public List<Transform<PopupView, PopupPane>> getTransformers() {
        return Collections.unmodifiableList(this.transformers);
    }

    @Override
    public int getUpdateInterval() {
        return this.updateInterval;
    }

    @Override
    public void setUpdateInterval(int interval) {
        this.updateInterval = interval;
    }

    private final class PopupViewImpl implements PopupView {

        private final Player viewer;
        private final State state;
        private PopupPane pane = new PopupPaneImpl();

        private PopupViewImpl(final Player viewer, final State state) {
            this.viewer = viewer;
            this.state = state;
        }

        @Override
        public PopupPane getPane() {
            return pane;
        }

        @Override
        public State getState() {
            return state;
        }

        @Override
        public PopupInterface getInterface() {
            return PopupInterfaceImpl.this;
        }

        @Override
        public Player getViewer() {
            return viewer;
        }

        @Override
        public boolean isViewing() {
            return PopupInterfaceImpl.this.views.contains(this);
        }

        @Override
        public void close() {
            PopupInterfaceImpl.this.views.remove(this);
        }
    }
}
