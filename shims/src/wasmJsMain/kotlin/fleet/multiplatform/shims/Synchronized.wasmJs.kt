package fleet.multiplatform.shims

actual inline fun synchronizedImpl(lock: Any, block: () -> Any?): Any? = block()