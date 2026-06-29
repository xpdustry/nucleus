// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.config;

public interface ConfigManager {

    <T> T get(final ConfigPropertyKey<T> key);

    <T> ConfigManager set(final ConfigPropertyKey<T> key, final T value);
}
