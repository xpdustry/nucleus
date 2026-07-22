// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.text

import com.xpdustry.foundation.plugin.PluginListener

class BadWordFinder(words: Map<BadWordCategory, List<String>> = DEFAULT_BAD_WORDS) : PluginListener {
    private val trie = StringTrieMap<BadWordCategory>()

    init {
        for ((category, list) in words.entries) {
            for (word in list) {
                this.trie[word] = category
            }
        }
    }

    fun findBadWords(text: String, categories: Collection<BadWordCategory>): List<String> =
        this.trie
            .search(text)
            .filter { categories.contains(it.value) || (it.word.length >= 5 || isSurroundedBySpaceChars(text, it)) }
            .map(StringTrieMap.Token<*>::word)
            .toList()

    private fun isSurroundedBySpaceChars(text: String, token: StringTrieMap.Token<*>): Boolean {
        if (token.index == 0 || text[token.index - 1].isWhitespace()) {
            val end = token.index + token.word.length
            return end >= text.length || text[end].isWhitespace()
        }
        return false
    }

    companion object {
        private val DEFAULT_BAD_WORDS = buildMap {
            put(
                BadWordCategory.STRONG_LANGUAGE,
                listOf(
                    "bastard",
                    "fuck",
                    "shit",
                ),
            )

            put(
                BadWordCategory.SEXUAL,
                listOf(
                    "anal",
                    "ball sack",
                    "bbw",
                    "bdsm",
                    "blowjob",
                    "clit",
                    "creampie",
                    "cum",
                    "cunt",
                    "erotic",
                    "fellatio",
                    "handjob",
                    "hentai",
                    "horny",
                    "jizz",
                    "kink",
                    "milf",
                    "nipple",
                    "penis",
                    "porn",
                    "porno",
                    "pussy",
                    "rape",
                    "rectum",
                    "scat",
                    "semen",
                    "sex",
                    "slut",
                    "tranny",
                    "vagina",
                    "viagra",
                    "whore",
                ),
            )

            put(
                BadWordCategory.HATE_SPEECH,
                listOf(
                    "faggot",
                    "negro",
                    "nigga",
                    "nigger",
                    "retard",
                ),
            )
        }
    }
}
