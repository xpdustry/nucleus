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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import fr.xpdustry.nucleus.api.application.NucleusInjector;
import fr.xpdustry.nucleus.api.application.lifecycle.LifecycleListenerRepository;
import java.util.Arrays;

public final class SimpleNucleusInjector implements NucleusInjector {

    private final Injector injector;

    public SimpleNucleusInjector(final LifecycleListenerRepository repository, final Module... modules) {
        this.injector = Guice.createInjector(Arrays.stream(modules)
                .map(m -> new LifecycleAwareModule(repository, m))
                .toList());
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        return injector.getInstance(type);
    }
}
