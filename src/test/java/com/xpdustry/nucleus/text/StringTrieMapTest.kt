// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.text

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class StringTrieMapTest {
    @Test
    fun put_and_get() {
        val trie = StringTrieMap<Int>()
        trie["test"] = 0
        trie["test1"] = 1
        trie["test2"] = 2

        Assertions.assertThat(trie["test"]).isEqualTo(0)
        Assertions.assertThat(trie["test1"]).isEqualTo(1)
        Assertions.assertThat(trie["test2"]).isEqualTo(2)
        Assertions.assertThat(trie["test3"]).isNull()
    }

    @Test
    fun contains() {
        val trie = StringTrieMap<Int>()
        trie["test"] = 0

        Assertions.assertThat("test" in trie).isTrue()
        Assertions.assertThat("te" in trie).isFalse()
        Assertions.assertThat("text" in trie).isFalse()
    }

    @Test
    fun search() {
        val trie = StringTrieMap<Int>()
        trie["dang"] = 0
        trie["cat"] = 1
        trie["catch"] = 2
        val result = trie.search("dang, this cat is hard to catch indeed")

        Assertions.assertThat(result)
            .containsExactly(
                StringTrieMap.Token("dang", 0, 0),
                StringTrieMap.Token("cat", 11, 1),
                StringTrieMap.Token("cat", 26, 1),
                StringTrieMap.Token("catch", 26, 2),
            )
    }

    @Test
    fun search_unicode() {
        val trie = StringTrieMap<Int>()
        trie["😏"] = 0
        trie["привет"] = 1
        val result = trie.search("test 😏 привет")

        Assertions.assertThat(result)
            .containsExactly(StringTrieMap.Token("😏", 5, 0), StringTrieMap.Token("привет", 8, 1))
    }
}
