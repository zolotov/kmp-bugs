@file:OptIn(ExperimentalWasmJsInterop::class)

import js.collections.WeakMap
import js.numbers.JsInt
import js.numbers.JsNumbers.toJsInt
import js.numbers.JsNumbers.toKotlinInt

private var nextHash = 1

private val weakMap = WeakMap<JsAny, JsInt>()

private fun memoizeIdentityHashCode(instance: JsAny): Int {
    val value = nextHash++
    weakMap.set(instance.toJsReference(), value.toJsInt())
    return value
}

internal fun identityHashCodeWasmJs(instance: Any?): Int {
    if (instance == null) {
        return 0
    }

    val jsRef = instance.toJsReference()
    return weakMap.get(jsRef)?.toKotlinInt() ?: memoizeIdentityHashCode(jsRef)
}
