// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.dependency;

import java.util.List;

public interface DependencyService {

    static DependencyService create(final DependencyModule... modules) {
        return new DependencyServiceImpl(modules);
    }

    <T> T instantiate(final Class<T> type);

    List<Object> resolveAll();

    <T> T resolve(final Class<T> type, final String name);

    <T> T resolve(final Class<T> type);

    interface Binder {

        <T> void bindInstance(final Class<T> type, final String name, final T instance);

        <T> void bindInstance(final Class<T> type, final T instance);

        <T> void bindConstructor(final Class<T> type, final String name, final Class<? extends T> impl);

        <T> void bindConstructor(final Class<T> type, final Class<? extends T> impl);

        default <T> void bindConstructor(final Class<T> type) {
            this.bindConstructor(type, type);
        }
    }
}
