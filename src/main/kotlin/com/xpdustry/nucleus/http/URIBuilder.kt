// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.http

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class URIBuilder {
    private val path: MutableList<String?> = ArrayList<String?>()
    private val parameters: MutableMap<String?, String?> = HashMap<String?, String?>()
    private val base: URI

    constructor(base: String) {
        this.base = URI.create(base)
    }

    constructor(base: URI) {
        this.base = base
    }

    fun addPathSegment(segment: String): URIBuilder {
        this.path.add(escape(segment))
        return this
    }

    fun addParameter(parameter: String, value: String): URIBuilder {
        this.parameters[escape(parameter)] = escape(value)
        return this
    }

    fun build(): URI {
        val base = this.base.toString()
        val builder = StringBuilder(base.length * 2).append(base)
        for (i in this.path.indices) {
            if (i > 0 || !base.endsWith("/")) {
                builder.append('/')
            }
            builder.append(this.path[i])
        }
        var first = true
        for (parameter in this.parameters.entries) {
            builder.append(if (first) '?' else '&')
            first = false
            builder.append(parameter.key).append('=').append(parameter.value)
        }
        return URI.create(builder.toString())
    }

    companion object {
        private fun escape(value: String): String? {
            return URLEncoder.encode(value, StandardCharsets.UTF_8)
        }
    }
}
