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

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.ProvisionListener;
import fr.xpdustry.nucleus.api.application.NucleusApplication;
import fr.xpdustry.nucleus.api.application.NucleusListener;

final class NucleusAwareModule extends AbstractModule {

    private final NucleusApplication application;
    private final Module module;

    NucleusAwareModule(final NucleusApplication application, final Module module) {
        this.application = application;
        this.module = module;
    }

    @Override
    protected void configure() {
        install(module);
        bind(NucleusApplication.class).toInstance(this.application);
        bindListener(Matchers.any(), new ProvisionListener() {
            @Override
            public <T> void onProvision(final ProvisionInvocation<T> invocation) {
                if (invocation.provision() instanceof NucleusListener listener) {
                    NucleusAwareModule.this.application.register(listener);
                }
            }
        });
    }
}
