package fleet.multiplatform.shims

import java.util.concurrent.ConcurrentHashMap as JavaConcurrentHashMap

actual fun <K, V> ConcurrentHashMap(): ConcurrentHashMap<K, V> = MultiplatformConcurrentHashMap(JavaConcurrentHashMap())

private class MultiplatformConcurrentHashMap<K, V>(val hashMap: JavaConcurrentHashMap<K, V>) : MutableMap<K, V> by hashMap, ConcurrentHashMap<K, V> {
    override fun remove(key: K, value: V): Boolean {
        return hashMap.remove(key, value)
    }

    override fun putIfAbsent(key: K, value: V): V? {
        return hashMap.putIfAbsent(key, value)
    }

    override fun computeIfAbsent(key: K, f: (K) -> V): V {
        return hashMap.computeIfAbsent(key, f)
    }

    override fun computeIfPresent(key: K, f: (K, V) -> V): V? {
        return hashMap.computeIfPresent(key, f)
    }

    override fun compute(key: K, f: (K, V?) -> V?): V? {
        return hashMap.compute(key, f)
    }
}
