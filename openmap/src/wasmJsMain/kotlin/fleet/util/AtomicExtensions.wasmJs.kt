package fleet.util

import kotlin.concurrent.atomics.AtomicReference

actual fun <T> AtomicReference<T>.updateAndGet(f: (T) -> T): T {
    val new = f(load())
    store(new)
    return new
}

actual fun <T> AtomicReference<T>.getAndUpdate(f: (T) -> T): T {
    val old = load()
    store(f(old))
    return old
}
