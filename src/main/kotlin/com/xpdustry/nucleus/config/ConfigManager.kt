// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.config

import com.xpdustry.foundation.plugin.PluginListener
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.Any
import kotlin.Boolean
import kotlin.Exception
import kotlin.IllegalArgumentException
import kotlin.IllegalStateException
import kotlin.Int
import kotlin.String
import kotlin.io.path.bufferedReader
import kotlin.reflect.cast
import org.jetbrains.annotations.VisibleForTesting

class ConfigManager : PluginListener {
    private var entries: MutableMap<String, Any> = HashMap<String, Any>()
    private val file: Path?

    constructor(file: Path) {
        this.file = file
    }

    constructor() {
        this.file = null
    }

    fun <T : Any> get(key: ConfigPropertyKey<T>): T {
        val value = this.entries[key.name]
        return if (value == null) key.def else key.type.cast(value)
    }

    fun <T : Any> set(key: ConfigPropertyKey<T>, value: T): ConfigManager {
        this.entries[key.name] = value
        return this
    }

    override fun onInit() {
        if (this.file == null || Files.notExists(this.file)) {
            return
        }

        val properties = Properties()
        try {
            this.file.bufferedReader().use { reader -> properties.load(reader) }
        } catch (e: IOException) {
            throw IllegalStateException("Failed to read the config file", e)
        }

        for (name in properties.stringPropertyNames()) {
            val key = ConfigPropertyKey.all()[name] ?: throw IllegalStateException("The key $name is unused")
            try {
                this.entries[key.name] = this.parse(key, properties.getProperty(name))
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to parse $name", e)
            }
        }
    }

    private fun <T : Any> parse(key: ConfigPropertyKey<T>, string: String): T {
        val value: Any
        if (key.type == String::class) {
            value = string
        } else if (key.type == Boolean::class) {
            value =
                when (string.lowercase()) {
                    "false" -> false
                    "true" -> true
                    else -> throw IllegalArgumentException("$string is not a valid boolean")
                }
        } else if (key.type == Int::class) {
            value = Integer.parseInt(string)
        } else if (key.type == URI::class) {
            value = URI.create(string)
        } else if (key.type.java.isEnum) {
            value =
                key.type.java.enumConstants.firstOrNull { (it as Enum<*>).name == string }
                    ?: throw IllegalArgumentException("$string is not a valid ${key.type.simpleName}")
        } else {
            throw IllegalArgumentException(key.type.simpleName + " is not a supported config type")
        }
        return key.type.cast(value)
    }

    @VisibleForTesting
    fun fork(): ConfigManager {
        val that = ConfigManager()
        that.entries = HashMap<String, Any>(this.entries)
        return that
    }
}
