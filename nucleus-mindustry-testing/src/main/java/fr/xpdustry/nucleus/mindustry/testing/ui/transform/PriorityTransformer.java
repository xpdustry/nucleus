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
package fr.xpdustry.nucleus.mindustry.testing.ui.transform;

import fr.xpdustry.distributor.api.util.Priority;
import fr.xpdustry.nucleus.mindustry.testing.ui.Pane;
import fr.xpdustry.nucleus.mindustry.testing.ui.View;

public final class PriorityTransformer<P extends Pane> implements Transformer<P>, Comparable<PriorityTransformer<?>> {

    private final Transformer<P> transformer;
    private final Priority priority;

    public PriorityTransformer(final Transformer<P> transformer, final Priority priority) {
        this.transformer = transformer;
        this.priority = priority;
    }

    @Override
    public void transform(final View view, final P pane) {
        transformer.transform(view, pane);
    }

    @Override
    public int compareTo(final PriorityTransformer<?> other) {
        return priority.compareTo(other.priority);
    }

    public Priority getPriority() {
        return priority;
    }
}
