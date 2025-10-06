package com.jetbrains.rhizomedb.benchmark

import com.jetbrains.rhizomedb.impl.Editor
import fleet.util.radixTrie.RadixTrie
import fleet.util.radixTrie.RadixTrieNode
import fleet.util.radixTrie.get
import fleet.util.radixTrie.put
import kotlinx.benchmark.*

@State(Scope.Benchmark)
class TrieBenchmark {
    var e: Editor? = null
    var t: RadixTrieNode<String>? = null

    @Setup
    fun prepare() {
        e = Editor()
        t = RadixTrie.empty()
        repeat(10000000) { i ->
            t = t!!.put(e, i, "hey $i")
        }
    }

    @Benchmark
    fun lookup(bh: Blackhole) {
        bh.consume(t!![5432101])
    }
}