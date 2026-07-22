// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.text

class StringTrieMap<V> {
    private var children: MutableMap<Char, StringTrieMap<V>>? = null
    private var value: V? = null

    data class Token<V>(val word: String, val index: Int, val value: V)

    private data class Accumulator<V>(var node: StringTrieMap<V>, val index: Int)

    fun search(chars: CharSequence): List<Token<V>> {
        val tokens = ArrayList<Token<V>>()
        val accumulators = ArrayList<Accumulator<V>>()

        for (i in chars.indices) {
            val c = chars[i]

            if (this.children != null && this.children!!.containsKey(c)) {
                accumulators.add(Accumulator(this, i))
            }

            var j = 0
            while (j < accumulators.size) {
                val accumulator = accumulators[j]

                val child = accumulator.node.children?.get(c)
                if (child == null) {
                    accumulators.removeAt(j)
                    continue
                } else {
                    accumulator.node = child
                }

                val value = accumulator.node.value
                if (value != null) {
                    tokens.add(Token(chars.substring(accumulator.index, i + 1), accumulator.index, value))
                }
                j++
            }
        }

        return tokens
    }

    operator fun set(chars: CharSequence, value: V) {
        var node: StringTrieMap<V> = this
        for (c in chars) {
            if (node.children == null) node.children = HashMap()
            node = node.children!!.computeIfAbsent(c) { StringTrieMap() }
        }
        node.value = value
    }

    operator fun get(chars: CharSequence): V? {
        var node: StringTrieMap<V> = this
        for (c in chars) {
            val child = node.children?.get(c)
            if (child == null) {
                return null
            } else {
                node = child
            }
        }
        return node.value
    }

    operator fun contains(chars: CharSequence): Boolean {
        var node: StringTrieMap<V> = this
        for (c in chars) {
            val child = node.children?.get(c)
            if (child == null) {
                return false
            } else {
                node = child
            }
        }
        return node.value != null
    }
}
