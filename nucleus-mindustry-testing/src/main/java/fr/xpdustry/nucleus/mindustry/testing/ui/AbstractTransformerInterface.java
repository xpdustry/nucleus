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
package fr.xpdustry.nucleus.mindustry.testing.ui;

import fr.xpdustry.distributor.api.DistributorProvider;
import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.distributor.api.util.MUUID;
import fr.xpdustry.distributor.api.util.Priority;
import fr.xpdustry.nucleus.mindustry.testing.ui.state.State;
import fr.xpdustry.nucleus.mindustry.testing.ui.transform.PriorityTransformer;
import fr.xpdustry.nucleus.mindustry.testing.ui.transform.Transformer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mindustry.game.EventType;
import mindustry.gen.Player;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractTransformerInterface<I extends TransformerInterface<I, P>, P extends Pane>
        implements TransformerInterface<I, P> {

    private final Map<MUUID, SimpleView> views = new HashMap<>();
    private final MindustryPlugin plugin;
    private final List<PriorityTransformer<P>> transformers = new ArrayList<>();

    protected AbstractTransformerInterface(final MindustryPlugin plugin) {
        this.plugin = plugin;

        DistributorProvider.get().getEventBus().subscribe(EventType.PlayerLeave.class, plugin, event -> {
            final var view = this.getView(event.player);
            if (view != null) {
                view.close();
            }
        });
    }

    @Override
    public View create(final View parent) {
        return new SimpleView(parent.getViewer(), parent);
    }

    @Override
    public View create(final Player viewer) {
        return new SimpleView(viewer, null);
    }

    @Override
    public List<Transformer<P>> getTransformers() {
        return Collections.unmodifiableList(this.transformers);
    }

    @SuppressWarnings("unchecked")
    @Override
    public I addTransformer(final Priority priority, final Transformer<P> transformer) {
        this.transformers.add(new PriorityTransformer<>(transformer, priority));
        return (I) this;
    }

    protected MindustryPlugin getPlugin() {
        return this.plugin;
    }

    protected Collection<SimpleView> getViews() {
        return Collections.unmodifiableCollection(this.views.values());
    }

    protected @Nullable SimpleView getView(final Player viewer) {
        return this.views.get(MUUID.of(viewer));
    }

    protected abstract P createPane();

    protected abstract void onViewOpen(final SimpleView view);

    protected void onViewClose(final SimpleView view) {}

    protected final class SimpleView implements View {

        private final Player viewer;
        private final @Nullable View parent;
        private final State state = State.create();
        private @MonotonicNonNull P pane = null;

        private SimpleView(final Player viewer, final @Nullable View parent) {
            this.viewer = viewer;
            this.parent = parent;
        }

        @Override
        public void open() {
            final var previous = AbstractTransformerInterface.this.views.get(MUUID.of(this.viewer));
            if (previous != this) {
                AbstractTransformerInterface.this.views.put(MUUID.of(viewer), this);
                if (previous != null) {
                    previous.close();
                }
            }
            this.pane = AbstractTransformerInterface.this.createPane();
            for (final var transform : AbstractTransformerInterface.this.transformers) {
                transform.transform(this, this.pane);
            }
            AbstractTransformerInterface.this.onViewOpen(this);
        }

        @Override
        public void close() {
            if (AbstractTransformerInterface.this.views.remove(MUUID.of(this.viewer), this)) {
                AbstractTransformerInterface.this.onViewClose(this);
            }
        }

        @Override
        public boolean isOpen() {
            return AbstractTransformerInterface.this.views.containsKey(MUUID.of(this.viewer));
        }

        @SuppressWarnings("unchecked")
        @Override
        public I getInterface() {
            return (I) AbstractTransformerInterface.this;
        }

        @Override
        public Player getViewer() {
            return this.viewer;
        }

        @Override
        public Optional<View> getParent() {
            return Optional.ofNullable(this.parent);
        }

        @Override
        public State getState() {
            return this.state;
        }

        public P getPane() {
            return this.pane;
        }
    }
}
