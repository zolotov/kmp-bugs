@file:OptIn(ExperimentalWasmJsInterop::class, UnsafeWasmMemoryApi::class)

import js.buffer.ArrayBuffer
import js.typedarrays.Uint8Array
import web.assembly.Memory
import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

external interface WasmExports {
    val memory: Memory<ArrayBuffer>
}

external val wasmExports: WasmExports

@OptIn(UnsafeWasmMemoryApi::class, ExperimentalWasmJsInterop::class)
internal fun BenchmarkScope.fastArrayToByteArray(array: Uint8Array<ArrayBuffer>): ByteArray {
    return span("fastArrayToByteArray") {
        val size = array.byteLength
        withScopedMemoryAllocator { allocator ->
            val bufferPtr = allocator.allocate(size)
            Uint8Array(wasmExports.memory.buffer).set(array, bufferPtr.address.toInt())
            readFromLinearMemory(bufferPtr, 0, size)
        }
    }
}

@OptIn(UnsafeWasmMemoryApi::class, ExperimentalWasmJsInterop::class)
internal fun BenchmarkScope.fastArraysToByteArrays(arrays: List<Uint8Array<ArrayBuffer>>): List<ByteArray> {
    return span("fastArraysToByteArrays") {
        val sizes = arrays.map { array -> array.byteLength }
        val totalSize = sizes.sum()
        val offsets = sizes.runningFold(0) { acc, s -> acc + s }

        withScopedMemoryAllocator { allocator ->
            val bufferPtr = allocator.allocate(totalSize)
            val pointerOffset = bufferPtr.address.toInt()
            arrays.mapIndexed { index, array ->
                Uint8Array(wasmExports.memory.buffer).set(array, pointerOffset + offsets[index])
            }
            List(sizes.size) { index ->
                readFromLinearMemory(bufferPtr, offsets[index], sizes[index])
            }
        }
    }
}

private fun readFromLinearMemory(base: Pointer, offset: Int, length: Int): ByteArray {
    val bytes = ByteArray(length)
    val src = base + offset
    val intCount = length / 4
    var idx = 0
    for (i in 0 until intCount) {
        val value = (src + idx).loadInt()
        bytes[idx] = (value and 0xFF).toByte()
        bytes[idx + 1] = ((value shr 8) and 0xFF).toByte()
        bytes[idx + 2] = ((value shr 16) and 0xFF).toByte()
        bytes[idx + 3] = ((value shr 24) and 0xFF).toByte()
        idx += 4
    }
    for (i in idx until length) {
        bytes[i] = (src + i).loadByte()
    }
    return bytes
}