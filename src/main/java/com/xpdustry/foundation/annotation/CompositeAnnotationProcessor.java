// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.foundation.annotation;

import java.util.List;
import java.util.Optional;

record CompositeAnnotationProcessor(List<PluginAnnotationProcessor<?>> processors)
        implements PluginAnnotationProcessor<List<Object>> {

    @Override
    public Optional<List<Object>> process(final Object instance) {
        return Optional.of(this.processors.stream()
                .map(processor -> processor.process(instance))
                .filter(Optional::isPresent)
                .map(optional -> (Object) optional.orElseThrow())
                .toList());
    }
}
