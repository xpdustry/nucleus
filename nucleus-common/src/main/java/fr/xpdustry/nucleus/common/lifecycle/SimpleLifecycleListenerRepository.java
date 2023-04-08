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
package fr.xpdustry.nucleus.common.lifecycle;

import fr.xpdustry.nucleus.api.application.lifecycle.LifecycleListener;
import fr.xpdustry.nucleus.api.application.lifecycle.LifecycleListenerRepository;
import java.util.ArrayList;
import java.util.List;

public final class SimpleLifecycleListenerRepository implements LifecycleListenerRepository {

    private final List<LifecycleListener> listeners = new ArrayList<>();

    @Override
    public void register(final LifecycleListener listener) {
        synchronized (this.listeners) {
            if (this.listeners.contains(listener)) {
                return;
            }
            this.listeners.add(listener);
        }
    }

    public void initAll() {
        this.listeners.forEach(LifecycleListener::onLifecycleInit);
    }

    public void exitAll() {
        this.listeners.forEach(LifecycleListener::onLifecycleExit);
        this.listeners.clear();
    }
}
