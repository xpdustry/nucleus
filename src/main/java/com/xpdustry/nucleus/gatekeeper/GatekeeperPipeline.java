// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.gatekeeper;

import com.xpdustry.nucleus.pipeline.AbstractProcessorPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GatekeeperPipeline extends AbstractProcessorPipeline<GatekeeperContext, GatekeeperDecision> {

    private static final Logger log = LoggerFactory.getLogger(GatekeeperPipeline.class);

    public GatekeeperPipeline() {
        super("gatekeeper");
    }

    @Override
    public GatekeeperDecision pump(final GatekeeperContext context) {
        for (final var processor : this.processors()) {
            final GatekeeperDecision decision;
            try {
                decision = processor.process(context);
            } catch (final RuntimeException error) {
                log.error("Error while verifying player {}", context.name(), error);
                continue;
            }
            if (decision instanceof GatekeeperDecision.Kick) {
                return decision;
            }
        }
        return GatekeeperDecision.ALLOW;
    }
}
