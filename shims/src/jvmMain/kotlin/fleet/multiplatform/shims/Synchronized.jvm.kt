package fleet.multiplatform.shims

actual inline fun synchronizedImpl(lock: Any, block: () -> Any?): Any? = kotlin.synchronized(lock, block)