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
package fr.xpdustry.nucleus.mindustry.testing.ui.popup;

import arc.util.Interval;
import arc.util.Time;
import fr.xpdustry.distributor.api.DistributorProvider;
import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.nucleus.mindustry.testing.ui.AbstractTransformingInterface;
import fr.xpdustry.nucleus.mindustry.testing.ui.View;
import fr.xpdustry.nucleus.mindustry.testing.ui.state.State;
import java.util.ArrayList;
import java.util.List;
import mindustry.game.EventType;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Call;
import mindustry.gen.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

final class PopupInterfaceImpl extends AbstractTransformingInterface<PopupPane> implements PopupInterface {

    private final List<PopupViewImpl> views = new ArrayList<>();
    private int updateInterval = 60;

    PopupInterfaceImpl(final MindustryPlugin plugin) {
        super(plugin);

        DistributorProvider.get().getEventBus().subscribe(EventType.PlayerLeave.class, plugin, event -> {
            views.removeIf(view -> view.getViewer().uuid().equals(event.player.uuid()));
        });

        DistributorProvider.get().getEventBus().subscribe(Trigger.update, plugin, () -> {
            for (final var view : views) {
                if (!view.interval.get(updateInterval)) {
                    continue;
                }
                view.update();
            }
        });
    }

    @Override
    protected PopupPane createPane() {
        return new PopupPaneImpl();
    }

    @Override
    public int getUpdateInterval() {
        return this.updateInterval;
    }

    @Override
    public void setUpdateInterval(final int interval) {
        this.updateInterval = interval;
    }

    @Override
    public View open(final Player viewer, final State state, final @Nullable View parent) {
        final var view = new PopupViewImpl(viewer, parent);
        view.setState(state);
        views.add(view);
        return view;
    }

    private final class PopupViewImpl extends AbstractView {

        private final Interval interval = new Interval();

        public PopupViewImpl(final Player viewer, final @Nullable View parent) {
            super(viewer, parent);
            this.interval.reset(0, Float.MAX_VALUE);
        }

        @Override
        public void update() {
            transform(this);
            Call.infoPopup(
                    this.getViewer().con(),
                    this.getPane().getContent(),
                    (Time.delta / 60F) * PopupInterfaceImpl.this.updateInterval,
                    this.getPane().getAlignement().getArcAlign(),
                    0,
                    0,
                    this.getPane().getShiftY(),
                    this.getPane().getShiftX());
        }

        @Override
        public boolean isOpen() {
            return PopupInterfaceImpl.this.views.contains(this);
        }

        @Override
        public void close() {
            PopupInterfaceImpl.this.views.remove(this);
        }
    }
}
