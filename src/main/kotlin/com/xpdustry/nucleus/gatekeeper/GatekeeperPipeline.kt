// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.gatekeeper

import com.xpdustry.nucleus.gatekeeper.GatekeeperDecision.Kick
import com.xpdustry.nucleus.pipeline.ProcessorPipeline
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GatekeeperPipeline : ProcessorPipeline<GatekeeperContext, GatekeeperDecision>("gatekeeper") {
    override suspend fun pump(context: GatekeeperContext): GatekeeperDecision {
        for (processor in this.processors()) {
            val decision: GatekeeperDecision
            try {
                decision = processor.process(context)
            } catch (error: Exception) {
                log.error("Error while verifying player {}", context.name, error)
                continue
            }
            if (decision is Kick) {
                return decision
            }
        }
        return GatekeeperDecision.Allow
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GatekeeperPipeline::class.java)
    }
}
