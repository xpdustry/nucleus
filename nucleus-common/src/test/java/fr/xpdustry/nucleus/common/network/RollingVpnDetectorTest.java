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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class RollingVpnDetectorTest {

    private static final String TEST_ADDRESS = "78.5.47.96";

    @Test
    public void test_empty() {
        final var rolling = new RollingVpnDetector();
        Assertions.assertThrows(ApiServiceException.class, () -> {
            try {
                rolling.isVpn(TEST_ADDRESS).join();
            } catch (final CompletionException exception) {
                throw exception.getCause();
            }
        });
    }

    @Test
    public void test_single() {
        final var detector = new TestVpnDetector();
        final var rolling = new RollingVpnDetector(detector);

        Assertions.assertEquals(0, detector.getCalls());

        detector.setOutput(true);
        Assertions.assertTrue(rolling.isVpn(TEST_ADDRESS).join());
        Assertions.assertEquals(1, detector.getCalls());

        detector.setOutput(false);
        Assertions.assertFalse(rolling.isVpn(TEST_ADDRESS).join());
        Assertions.assertEquals(2, detector.getCalls());
    }

    @Test
    public void test_multiple_simple() {
        final var detector1 = new TestVpnDetector();
        detector1.setOutput(true);
        final var detector2 = new TestVpnDetector();
        detector2.setOutput(false);
        final var rolling = new RollingVpnDetector(detector1, detector2);

        Assertions.assertEquals(0, detector1.getCalls());
        Assertions.assertEquals(0, detector2.getCalls());

        Assertions.assertTrue(rolling.isVpn(TEST_ADDRESS).join());
        Assertions.assertEquals(1, detector1.getCalls());
        Assertions.assertEquals(0, detector2.getCalls());

        Assertions.assertFalse(rolling.isVpn(TEST_ADDRESS).join());
        Assertions.assertEquals(1, detector1.getCalls());
        Assertions.assertEquals(1, detector2.getCalls());
    }

    @Test
    public void test_multiple_failure_single() {
        final var detector1 = new TestVpnDetector();
        detector1.setError(true);
        final var detector2 = new TestVpnDetector();
        detector2.setOutput(true);
        final var detector3 = new TestVpnDetector();
        detector3.setError(true);
        final var detector4 = new TestVpnDetector();
        detector4.setOutput(false);

        final var rolling = new RollingVpnDetector(detector1, detector2, detector3, detector4);

        Assertions.assertEquals(0, detector1.getCalls());
        Assertions.assertEquals(0, detector2.getCalls());
        Assertions.assertEquals(0, detector3.getCalls());
        Assertions.assertEquals(0, detector4.getCalls());

        Assertions.assertTrue(rolling.isVpn(TEST_ADDRESS).join());

        Assertions.assertEquals(1, detector1.getCalls());
        Assertions.assertEquals(1, detector2.getCalls());
        Assertions.assertEquals(0, detector3.getCalls());
        Assertions.assertEquals(0, detector4.getCalls());

        Assertions.assertFalse(rolling.isVpn(TEST_ADDRESS).join());

        Assertions.assertEquals(1, detector1.getCalls());
        Assertions.assertEquals(1, detector2.getCalls());
        Assertions.assertEquals(1, detector3.getCalls());
        Assertions.assertEquals(1, detector4.getCalls());

        Assertions.assertTrue(rolling.isVpn(TEST_ADDRESS).join());

        Assertions.assertEquals(2, detector1.getCalls());
        Assertions.assertEquals(2, detector2.getCalls());
        Assertions.assertEquals(1, detector3.getCalls());
        Assertions.assertEquals(1, detector4.getCalls());
    }

    @Test
    void test_multiple_failure_all() {
        final var detector1 = new TestVpnDetector();
        detector1.setError(true);
        final var detector2 = new TestVpnDetector();
        detector2.setError(true);
        final var detector3 = new TestVpnDetector();
        detector3.setError(true);

        final var rolling = new RollingVpnDetector(detector1, detector2, detector3);

        Assertions.assertEquals(0, detector1.getCalls());
        Assertions.assertEquals(0, detector2.getCalls());
        Assertions.assertEquals(0, detector3.getCalls());

        Assertions.assertThrows(ApiServiceException.class, () -> {
            try {
                rolling.isVpn(TEST_ADDRESS).join();
            } catch (final CompletionException exception) {
                throw exception.getCause();
            }
        });

        Assertions.assertEquals(1, detector1.getCalls());
        Assertions.assertEquals(1, detector2.getCalls());
        Assertions.assertEquals(1, detector3.getCalls());

        Assertions.assertThrows(ApiServiceException.class, () -> {
            try {
                rolling.isVpn(TEST_ADDRESS).join();
            } catch (final CompletionException exception) {
                throw exception.getCause();
            }
        });

        Assertions.assertEquals(2, detector1.getCalls());
        Assertions.assertEquals(2, detector2.getCalls());
        Assertions.assertEquals(2, detector3.getCalls());
    }

    private static final class TestVpnDetector implements VpnDetector {

        private boolean output = false;
        private boolean error = false;
        private int calls = 0;

        @Override
        public CompletableFuture<Boolean> isVpn(final String address) {
            this.calls++;
            if (this.error) {
                return CompletableFuture.failedFuture(new ApiServiceException());
            }
            return CompletableFuture.completedFuture(this.output);
        }

        public void setOutput(final boolean output) {
            this.output = output;
        }

        public void setError(final boolean error) {
            this.error = error;
        }

        public int getCalls() {
            return this.calls;
        }
    }
}
