@file:OptIn(ExperimentalWasmJsInterop::class, UnsafeWasmMemoryApi::class)

import js.array.component1
import js.array.component2
import js.buffer.ArrayBuffer
import js.objects.Object
import js.string.JsStrings.toKotlinString
import js.typedarrays.Uint8Array
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi

@JsModule("fflate")
external object Fflate {
    fun unzipSync(
        data: Uint8Array<ArrayBuffer>,
        opts: UnzipSyncOptions = definedExternally
    ): JsAny
}

external interface UnzipSyncOptions

/**
 * Download zip as ArrayBuffer → synchronous unzip with fflate → copy Uint8Arrays into
 * WASM linear memory → read back as ByteArrays.
 * Uses fflate's `unzipSync` for fast, synchronous decompression.
 */
suspend fun BenchmarkScope.fflateWasmMemory() {
    val zip = span("download") {
        downloadFileIntoBytes("fonts_linux.zip")
    }
    val files = span("parse zip") {
        Fflate.unzipSync(Uint8Array(zip))
    }

    val names = mutableListOf<String>()
    val offsets = mutableListOf<Int>()
    var totalSize = 0
    val arrays = mutableListOf<Uint8Array<ArrayBuffer>>()
    span("load sizes") {
        Object.entries(files).toArray().forEach { (fileName, file) ->
            offsets.add(totalSize)
            val buffer = file!!.unsafeCast<Uint8Array<ArrayBuffer>>()
            totalSize += buffer.byteLength
            names.add(fileName.toKotlinString())
            arrays.add(buffer)
        }
    }

    fastArraysToByteArrays(arrays)
}
