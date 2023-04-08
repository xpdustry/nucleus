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

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.matcher.Matchers;
import fr.xpdustry.nucleus.api.application.lifecycle.LifecycleListenerRepository;

final class LifecycleAwareModule extends AbstractModule {

    private final LifecycleListenerRepository repository;
    private final Module module;

    LifecycleAwareModule(final LifecycleListenerRepository repository, Module module) {
        this.repository = repository;
        this.module = module;
    }

    @Override
    protected void configure() {
        bindListener(Matchers.any(), new LifecycleProvisionListener(this.repository));
        install(module);
    }
}
