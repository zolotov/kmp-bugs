@file:OptIn(ExperimentalWasmJsInterop::class)

import js.buffer.ArrayBuffer
import js.typedarrays.Uint8Array
import js.typedarrays.toByteArray
import jszip.JSZip
import kotlinx.coroutines.await
import web.blob.Blob
import kotlin.js.Promise

@JsModule("jszip")
external class FleetJSZip : JsAny {
    @Suppress("unused")
    fun loadAsync(data: Uint8Array<ArrayBuffer>): Promise<JSZip>

    @Suppress("unused")
    fun loadAsync(data: Blob): Promise<JSZip>
}

/**
 * Download zip as Blob → parse with Fleet's JSZip wrapper (loadAsync) →
 * extract files to Uint8Arrays via JS interop → convert to ByteArrays.
 * Demonstrates the approach used in Fleet for loading zipped resources.
 */
suspend fun BenchmarkScope.fleetJsZipLoad() {
    val zip = downloadFileIntoBytes("fonts_linux.zip")
    val jsZip = span("parse zip") {
        FleetJSZip().loadAsync(data = zip).await<FleetJSZip>()
    }
    span("extract files") {
        jsZip.getFiles()
    }
}

private suspend fun FleetJSZip.getFiles(): Map<String, ByteArray> =
    getFilesJs(this).await<JsArray<JsArray<JsAny>>>().toArray().associate { array ->
        val name = array[0].toString()
        val content = array[1] as Uint8Array<*>
        name to content.toByteArray()
    }

@Suppress("unused")
private fun getFilesJs(zip: FleetJSZip): Promise<JsAny?> =
    js(
        """{
    const files = Object.values(zip.files);
    const filePromises = files
        .filter(file => !file.dir)
        .map(file =>
            file.async("uint8array").then(content => [file.name, content])
        );
    return Promise.all(filePromises);
  }"""
    )
