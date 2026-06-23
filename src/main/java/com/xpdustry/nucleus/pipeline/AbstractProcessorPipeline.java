// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.pipeline;

import com.xpdustry.foundation.util.Priority;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractProcessorPipeline<I, O> implements ProcessorPipeline<I, O> {

    private final Logger logger;
    private final List<ProcessorWithData<I, O>> processors = new ArrayList<>();

    protected AbstractProcessorPipeline(final String name) {
        this.logger = LoggerFactory.getLogger("processor-pipeline-" + name);
    }

    protected final List<Processor<I, O>> processors() {
        return this.processors.stream().map(ProcessorWithData::processor).toList();
    }

    protected final @Nullable Processor<I, O> processor(final String name) {
        return this.processors.stream()
                .filter(processor -> processor.name().equals(name))
                .findFirst()
                .map(ProcessorWithData::processor)
                .orElse(null);
    }

    @Override
    public final void register(final String name, final Priority priority, final Processor<I, O> processor) {
        if (this.processors.stream().anyMatch(candidate -> candidate.name().equals(name))) {
            throw new IllegalArgumentException("Processor with name " + name + " already registered");
        }
        this.processors.add(new ProcessorWithData<>(processor, name, priority));
        this.processors.sort(Comparator.comparing(ProcessorWithData::priority));
        this.logger.debug("Registered processor {} with priority {}", name, priority);
    }

    private record ProcessorWithData<I, O>(Processor<I, O> processor, String name, Priority priority) {}
}
