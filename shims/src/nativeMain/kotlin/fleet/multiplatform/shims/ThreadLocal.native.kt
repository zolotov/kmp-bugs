package fleet.multiplatform.shims

internal actual fun threadLocalImpl(supplier: () -> Any?): ThreadLocal<Any?> = NativeThreadLocal(supplier)

@kotlin.native.concurrent.ThreadLocal
private val threadLocals = mutableMapOf<NativeThreadLocal<*>, Any?>()

class NativeThreadLocal<T>(private val supplier: () -> T) : ThreadLocal<T> {
    init {
        threadLocals[this] = supplier() as Any?
    }
    override fun get(): T {
        return threadLocals[this] as T
    }

    override fun remove() {
        threadLocals[this] = supplier() as Any?
    }

    override fun set(value: T) {
        threadLocals[this] = value
    }
}
