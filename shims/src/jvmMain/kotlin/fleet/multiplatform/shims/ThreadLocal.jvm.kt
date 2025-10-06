package fleet.multiplatform.shims

internal actual fun threadLocalImpl(supplier: () -> Any?) = threadLocal(supplier)

private fun <T> threadLocal(supplier: (() -> T)) = object : ThreadLocal<T> {
    val threadLocal = java.lang.ThreadLocal.withInitial(supplier)

    override fun get(): T = threadLocal.get()

    override fun remove() = threadLocal.remove()

    override fun set(value: T) = threadLocal.set(value)
}
