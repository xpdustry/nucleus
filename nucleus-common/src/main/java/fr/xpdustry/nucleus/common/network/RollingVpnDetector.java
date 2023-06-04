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
package fr.xpdustry.nucleus.common.network;

import fr.xpdustry.nucleus.common.web.ApiServiceException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class RollingVpnDetector implements VpnDetector {

    private final List<VpnDetector> detectors;
    private final AtomicInteger initial = new AtomicInteger(-1);

    public RollingVpnDetector(final List<VpnDetector> detectors) {
        this.detectors = List.copyOf(detectors);
    }

    public RollingVpnDetector(final VpnDetector... detectors) {
        this(List.of(detectors));
    }

    @Override
    public CompletableFuture<Boolean> isVpn(final String address) {
        if (this.detectors.size() == 0) {
            return CompletableFuture.failedFuture(new ApiServiceException("No VPN detectors available."));
        } else if (this.detectors.size() == 1) {
            return this.detectors.get(0).isVpn(address);
        }
        return tryNext(this.initial.get(), 0, address);
    }

    private CompletableFuture<Boolean> tryNext(final int index, final int tries, final String address) {
        // Ugly hack, I hate it
        if (tries == this.detectors.size()) {
            return CompletableFuture.failedFuture(new ApiServiceException("All VPN detectors have errored."));
        }
        final var next = (index + 1) % this.detectors.size();
        return this.detectors
                .get(next)
                .isVpn(address)
                .handle((result, throwable) -> {
                    if (throwable == null) {
                        this.initial.set(next);
                        return CompletableFuture.completedFuture(result);
                    }
                    return tryNext(next, tries + 1, address);
                })
                .thenCompose(future -> future);
    }
}
