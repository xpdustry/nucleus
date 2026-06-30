// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.gatekeeper;

import java.time.Duration;

public sealed interface GatekeeperResult permits GatekeeperResult.Success, GatekeeperResult.Failure {

    Success SUCCESS = new Success();

    final class Success implements GatekeeperResult {
        private Success() {}
    }

    record Failure(String reason, Duration duration) implements GatekeeperResult {

        private static final Duration FIVE_SECONDS = Duration.ofSeconds(5);

        public Failure(final String reason) {
            this(reason, FIVE_SECONDS);
        }
    }
}
