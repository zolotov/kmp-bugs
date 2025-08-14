/**
 * [KT-80134 Wasm: Illegal cast to Unit](https://youtrack.jetbrains.com/issue/KT-80134/Wasm-Illegal-cast-to-Unit)
 */

fun main() {
    doRoll().invoke()
}

private fun doRoll(): () -> Unit {
    return {
        val hello = "Hello"
        uncheckedCast(hello)
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> uncheckedCast(any: Any?): T = any as T