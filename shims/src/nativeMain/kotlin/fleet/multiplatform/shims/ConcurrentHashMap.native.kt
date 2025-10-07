package fleet.multiplatform.shims

actual fun <K, V> ConcurrentHashMap(): ConcurrentHashMap<K, V> {
    return MultiplatformConcurrentHashMap(mutableMapOf())
}

private class MultiplatformConcurrentHashMap<K, V>(val hashMap: MutableMap<K, V>) : MutableMap<K, V> by hashMap, ConcurrentHashMap<K, V> {
    private val lock = SynchronizedObject()

    override fun remove(key: K, value: V): Boolean {
        return synchronized(lock) {
            hashMap[key] == value && hashMap.remove(key) != null
        }
    }

    override fun putIfAbsent(key: K, value: V): V? {
        return synchronized(lock) {
            hashMap[key] ?: hashMap.put(key, value)
        }
    }

    override fun computeIfAbsent(key: K, f: (K) -> V): V {
        return synchronized(lock) {
            hashMap[key] ?: f(key).also { hashMap[key] = it }
        }
    }

    override fun computeIfPresent(key: K, f: (K, V) -> V): V? {
        return synchronized(lock) {
            hashMap[key]?.let { f(key, it) }
        }
    }

    override fun compute(key: K, f: (K, V?) -> V?): V? {
        return synchronized(lock) {
            val newValue = f(key, hashMap[key])
            if (newValue == null) {
                hashMap.remove(key)
                null
            } else {
                hashMap[key] = newValue
                newValue
            }
        }
    }
}
