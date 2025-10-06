package fleet.multiplatform.shims

internal actual fun threadLocalImpl(supplier: () -> Any?) = threadLocal(supplier)

private fun <T> threadLocal(supplier: () -> T) = object : ThreadLocal<T> {
    var value: T? = null

    override fun get(): T = value ?: supplier().also { value = it }

    override fun remove() {
        value = null
    }

    override fun set(value: T) {
        this.value = value
    }
}