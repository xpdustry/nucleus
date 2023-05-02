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
import fr.xpdustry.nucleus.mindustry.testing.ui.AbstractTransformerInterface;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Call;

final class PopupInterfaceImpl extends AbstractTransformerInterface<PopupInterface, PopupPane>
        implements PopupInterface {

    private final Interval interval = new Interval();
    private int updateInterval = 60;

    PopupInterfaceImpl(final MindustryPlugin plugin) {
        super(plugin);

        this.interval.reset(0, Float.MAX_VALUE);

        DistributorProvider.get().getEventBus().subscribe(Trigger.update, plugin, () -> {
            if (PopupInterfaceImpl.this.interval.get(PopupInterfaceImpl.this.updateInterval)) {
                for (final var view : PopupInterfaceImpl.this.getViews()) {
                    view.open();
                }
            }
        });
    }

    @Override
    public int getUpdateInterval() {
        return updateInterval;
    }

    @Override
    public PopupInterface setUpdateInterval(final int updateInterval) {
        this.updateInterval = updateInterval;
        return this;
    }

    @Override
    protected PopupPane createPane() {
        return new PopupPaneImpl();
    }

    @Override
    protected void onViewOpen(final SimpleView view) {
        Call.infoPopup(
                view.getViewer().con(),
                view.getPane().getContent(),
                // Don't even ask me why this works, I don't know either
                (Time.delta / 60F) * PopupInterfaceImpl.this.updateInterval,
                view.getPane().getAlignement().getArcAlign(),
                0,
                0,
                view.getPane().getShiftY(),
                view.getPane().getShiftX());
    }
}
