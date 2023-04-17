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

import fr.xpdustry.distributor.api.util.Priority;
import fr.xpdustry.nucleus.mindustry.testing.ui.state.State;
import fr.xpdustry.nucleus.mindustry.testing.ui.transform.PriorityTransform;
import fr.xpdustry.nucleus.mindustry.testing.ui.transform.Transform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import mindustry.gen.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractTransformingInterface<P extends Pane> implements TransformingInterface<P> {

    private final List<PriorityTransform<P>> transformers = new ArrayList<>();

    @Override
    public List<Transform<P>> getTransformers() {
        return Collections.unmodifiableList(transformers);
    }

    @Override
    public void addTransformer(final Priority priority, final Transform<P> transform) {
        transformers.add(new PriorityTransform<>(transform, priority));
    }

    protected void transform(final AbstractView view) {
        view.pane.clear();
        for (final var transformer : transformers) {
            transformer.transform(view, view.getPane());
        }
    }

    protected abstract P createPane();

    public abstract class AbstractView implements View {

        private final Player viewer;
        private State state = State.create();
        private final @Nullable View parent;
        private final P pane = createPane();

        protected AbstractView(final Player viewer, final @Nullable View parent) {
            this.viewer = viewer;
            this.parent = parent;
        }

        // TODO Implement reactive properties, this State system is awful
        public void setState(final State state) {
            this.state = state;
        }

        @Override
        public Interface getInterface() {
            return AbstractTransformingInterface.this;
        }

        @Override
        public Player getViewer() {
            return viewer;
        }

        @Override
        public Optional<View> getParent() {
            return Optional.ofNullable(parent);
        }

        @Override
        public State getState() {
            return state;
        }

        public P getPane() {
            return pane;
        }
    }
}
