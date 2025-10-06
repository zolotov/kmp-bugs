package fleet.util

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.asJavaAtomic

actual fun <T> AtomicReference<T>.updateAndGet(f: (T) -> T): T {
    return asJavaAtomic().updateAndGet(f)
}

actual fun <T> AtomicReference<T>.getAndUpdate(f: (T) -> T): T {
    return asJavaAtomic().getAndUpdate(f)
}