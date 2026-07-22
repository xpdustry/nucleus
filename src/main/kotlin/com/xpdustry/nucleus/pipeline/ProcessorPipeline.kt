// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.pipeline

import com.xpdustry.foundation.util.Priority
import org.slf4j.LoggerFactory

abstract class ProcessorPipeline<I, O> protected constructor(name: String) {
    private val logger = LoggerFactory.getLogger("processor-pipeline-$name")
    private val processors = ArrayList<ProcessorWithData<I, O>>()

    abstract suspend fun pump(context: I): O

    protected fun processors(): List<Processor<I, O>> {
        return this.processors.map(ProcessorWithData<I, O>::processor)
    }

    protected fun processor(name: String): Processor<I, O>? {
        return this.processors.find { it.name == name }?.processor
    }

    fun register(name: String, priority: Priority = Priority.NORMAL, processor: Processor<I, O>) {
        require(this.processor(name) == null) { "Processor $name is already registered" }
        this.processors.add(ProcessorWithData(processor, name, priority))
        processors.sortWith(compareBy(ProcessorWithData<I, O>::priority))
        this.logger.debug("Registered processor {} with priority {}", name, priority)
    }

    private data class ProcessorWithData<I, O>(
        val processor: Processor<I, O>,
        val name: String,
        val priority: Priority,
    )
}
