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
package fr.xpdustry.nucleus.common.application;

import fr.xpdustry.nucleus.api.application.NucleusApplication;
import fr.xpdustry.nucleus.api.application.NucleusListener;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNucleusApplication implements NucleusApplication {

    private final List<NucleusListener> listeners = new ArrayList<>();

    @Override
    public void init() {
        this.listeners.forEach(NucleusListener::onNucleusInit);
    }

    @Override
    public void exit(final Cause cause) {
        this.listeners.forEach(NucleusListener::onNucleusExit);
        this.listeners.clear();
    }

    @Override
    public void register(final NucleusListener listener) {
        synchronized (this.listeners) {
            if (this.listeners.contains(listener)) {
                return;
            }
            this.listeners.add(listener);
            this.onRegister(listener);
        }
    }

    protected void onRegister(final NucleusListener listener) {}
}
