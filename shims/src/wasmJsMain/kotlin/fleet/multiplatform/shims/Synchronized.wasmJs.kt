package fleet.multiplatform.shims

actual typealias SynchronizedObject = Any

actual inline fun synchronizedImpl(lock: SynchronizedObject, block: () -> Any?): Any? = block()
