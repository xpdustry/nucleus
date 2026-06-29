// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus;

import com.xpdustry.foundation.FoundationAPI;
import com.xpdustry.foundation.event.EventPublisher;
import com.xpdustry.foundation.event.EventPublisherImpl;
import com.xpdustry.foundation.plugin.ListenablePluginFacade;
import com.xpdustry.foundation.scheduler.MindustryScheduler;
import com.xpdustry.foundation.scheduler.MindustrySchedulerImpl;
import com.xpdustry.foundation.scheduler.MindustryTimeSource;
import com.xpdustry.foundation.translation.TranslationSourceList;

public final class FoundationAPIFromNucleus implements FoundationAPI {

    private final TranslationSourceList translations = new TranslationSourceList();
    private final EventPublisher events = new EventPublisherImpl();
    private final MindustrySchedulerImpl scheduler = new MindustrySchedulerImpl(MindustryTimeSource.mindustry());

    public FoundationAPIFromNucleus(final ListenablePluginFacade plugin) {
        plugin.addListener(this.scheduler);
    }

    @Override
    public TranslationSourceList translations() {
        return this.translations;
    }

    @Override
    public EventPublisher events() {
        return this.events;
    }

    @Override
    public MindustryScheduler scheduler() {
        return this.scheduler;
    }
}
