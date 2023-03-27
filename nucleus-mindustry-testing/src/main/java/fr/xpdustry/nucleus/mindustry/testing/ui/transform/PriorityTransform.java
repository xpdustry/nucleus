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

public final class PriorityTransform<P extends Pane> implements Transform<P>, Comparable<PriorityTransform<?>> {

    private final Transform<P> transform;
    private final Priority priority;

    public PriorityTransform(final Transform<P> transform, final Priority priority) {
        this.transform = transform;
        this.priority = priority;
    }

    @Override
    public void transform(final View view, final P pane) {
        transform.transform(view, pane);
    }

    @Override
    public int compareTo(final PriorityTransform<?> other) {
        return priority.compareTo(other.priority);
    }

    public Priority getPriority() {
        return priority;
    }
}
