// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a sealed configuration type or subtype for [SealedConfigDecoder].
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SealedConfig {

    /// The name used in the `type` discriminator for a sealed subtype.
    String name() default "";

    /// The default subtype name used when the `type` discriminator is absent.
    String def() default "";
}
