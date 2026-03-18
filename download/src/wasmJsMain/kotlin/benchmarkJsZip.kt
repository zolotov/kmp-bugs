@file:OptIn(ExperimentalWasmJsInterop::class, UnsafeWasmMemoryApi::class)

import jszip.JSZip
import jszip.bytes
import jszip.load
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.measureTimedValue
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi

/**
 * Download zip → parse with JSZip → extract each file as ArrayBuffer → convert to ByteArray.
 * Standard JSZip approach using kotlin-wrappers.
 */
suspend fun BenchmarkScope.jsZipLoad() {
    val zip = span("download") {
        downloadFileIntoBytes("fonts_linux.zip")
    }
    val jsZip = span("parse zip") {
        JSZip().load(data = zip)
    }
    span("extract files") {
        coroutineScope {
            measureTimedValue {
                buildList {
                    jsZip.forEach { name, rec ->
                        add(async { name to rec.bytes() })
                    }
                }.awaitAll().toMap()
            }
        }
    }
}

/**
 * Download zip as ArrayBuffer → parse with JSZip → extract files as Blobs →
 * copy each Blob into WASM linear memory → read back as ByteArrays.
 */
suspend fun BenchmarkScope.jsZipWasmMemory() {
    val zip = span("download") {
        downloadFileIntoBytes("fonts_linux.zip")
    }
    val jsZip = span("parse zip") {
        JSZip().load(data = zip)
    }

    val files = buildList {
        jsZip.forEach { _, file -> add(file) }
    }
    val unzippedArrays = span("extract") {
        coroutineScope {
            files.map { file ->
                async { file.name to file.bytes() }
            }.awaitAll()
        }
    }
    fastArraysToByteArrays(unzippedArrays.map { (_, buf) -> buf })
}