package fleet.multiplatform.shims

actual typealias SynchronizedObject = kotlinx.atomicfu.locks.SynchronizedObject

actual inline fun synchronizedImpl(lock: SynchronizedObject, block: () -> Any?): Any? {
    return kotlinx.atomicfu.locks.synchronized(lock, block)
}