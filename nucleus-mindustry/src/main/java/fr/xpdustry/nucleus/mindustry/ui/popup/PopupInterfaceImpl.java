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
package fr.xpdustry.nucleus.mindustry.ui.popup;

import arc.util.Interval;
import arc.util.Time;
import fr.xpdustry.distributor.api.util.MoreEvents;
import fr.xpdustry.nucleus.mindustry.ui.*;
import java.util.*;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Player;

final class PopupInterfaceImpl implements PopupInterface {

    private final List<Transform<PopupPane.Mutable>> transforms = new ArrayList<>();
    private final Interval interval = new Interval();
    private final Set<PopupViewImpl> views = new HashSet<>();
    private int updateInterval = 60;

    {
        MoreEvents.subscribe(EventType.PlayerLeave.class, event -> {
            views.removeIf(view -> view.getViewer() == event.player);
        });
    }

    @Override
    public void onPluginUpdate() {
        if (!interval.get(updateInterval)) {
            return;
        }
        for (final var view : views) {
            final var context =
                    TransformContext.<PopupPane.Mutable>of(view.getState(), view.getPane(), view.getViewer());
            for (final var transform : transforms) {
                transform.accept(context);
            }
            if (view.getPane().isEmpty()) {
                return;
            }
            Call.infoPopup(
                    view.getViewer().con(),
                    view.getPane().getContent(),
                    (Time.delta / 60F) * PopupInterfaceImpl.this.updateInterval,
                    view.getPane().getAlignement().alignement,
                    view.getPane().getShiftY() > 0 ? view.getPane().getShiftY() : 0,
                    view.getPane().getShiftX() > 0 ? view.getPane().getShiftX() : 0,
                    view.getPane().getShiftY() < 0 ? view.getPane().getShiftY() : 0,
                    view.getPane().getShiftX() < 0 ? view.getPane().getShiftX() : 0);
        }
    }

    @Override
    public PopupView open(final Player viewer, final State metadata) {
        final var view = new PopupViewImpl(viewer, metadata);
        views.add(view);
        return view;
    }

    @Override
    public void addTransformer(final Transform<PopupPane.Mutable> transform) {
        this.transforms.add(transform);
    }

    @Override
    public List<Transform<PopupPane.Mutable>> getTransformers() {
        return Collections.unmodifiableList(this.transforms);
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

        private final PopupPaneImpl pane = new PopupPaneImpl();
        private final Player viewer;
        private final State metadata;

        private PopupViewImpl(final Player viewer, final State metadata) {
            this.viewer = viewer;
            this.metadata = metadata;
        }

        @Override
        public PopupPaneImpl getPane() {
            return pane;
        }

        @Override
        public State getState() {
            return metadata;
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
