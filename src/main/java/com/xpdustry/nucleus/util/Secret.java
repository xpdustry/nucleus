// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.util;

public record Secret(String value) {
    @Override
    public String toString() {
        return "Secret{*****}";
    }
}
