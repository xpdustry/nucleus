// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.foundation;

import com.xpdustry.foundation.event.EventPublisher;
import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.foundation.scheduler.MindustryScheduler;
import com.xpdustry.foundation.translation.TranslationSourceList;
import com.xpdustry.nucleus.NucleusPlugin;
import java.util.Objects;
import mindustry.Vars;

/// The foundation-common API.
public interface FoundationAPI extends PluginListener {

    /// Returns the global [FoundationAPI] instance.
    static FoundationAPI get() {
        return ((NucleusPlugin)
                        Objects.requireNonNull(Vars.mods.getMod(NucleusPlugin.class), "nucleus is not loaded").main)
                .foundation;
    }

    /// Returns the global translation source list.
    TranslationSourceList translations();

    /// Returns the event publisher.
    EventPublisher events();

    /// Returns the plugin scheduler.
    MindustryScheduler scheduler();
}
