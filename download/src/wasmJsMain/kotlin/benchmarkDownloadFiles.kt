@file:OptIn(ExperimentalWasmJsInterop::class, UnsafeWasmMemoryApi::class)

import js.typedarrays.toByteArray
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi

val fileNames = listOf(
    "fonts/Inter/InterVariable-Italic.ttf",
    "fonts/Inter/InterVariable.ttf",
    "fonts/Inter/LICENSE.txt",
    "fonts/JetBrainsMono/AUTHORS.txt",
    "fonts/JetBrainsMono/JetBrainsMono-Bold.ttf",
    "fonts/JetBrainsMono/JetBrainsMono-BoldItalic.ttf",
    "fonts/JetBrainsMono/JetBrainsMono-ExtraBold.ttf",
    "fonts/JetBrainsMono/JetBrainsMono-ExtraBoldItalic.ttf",
    "fonts/JetBrainsMono/JetBrainsMono-ExtraLight.ttf",
    "fonts/JetBrainsMono/JetBrainsMono-ExtraLightItalic.ttf",
    "fonts/JetBrainsMono/JetBrainsMono-Italic.ttf",
    "fonts/JetBrainsMono/JetBrainsMono-Light.ttf",
    "fonts/JetBrainsMono/JetBrainsMono-LightItalic.ttf",
    "fonts/JetBrainsMono/JetBrainsMono-Medium.ttf",
    "fonts/JetBrainsMono/JetBrainsMono-MediumItalic.ttf",
    "fonts/JetBrainsMono/JetBrainsMono-Regular.ttf",
    "fonts/JetBrainsMono/JetBrainsMono-SemiBold.ttf",
    "fonts/JetBrainsMono/JetBrainsMono-SemiBoldItalic.ttf",
    "fonts/JetBrainsMono/JetBrainsMono-Thin.ttf",
    "fonts/JetBrainsMono/JetBrainsMono-ThinItalic.ttf",
    "fonts/JetBrainsMono/OFL.txt",
    "fonts/KomunaVar/KomunaVar.ttf",
    "fonts/KomunaVar/Tekio_EULA_2024.pdf",
    "fonts/NotoColorEmoji/LICENSE",
    "fonts/NotoColorEmoji/NotoColorEmoji.ttf",
)

/**
 * Download each file individually as Blob → convert to ByteArray via Blob.byteArray().
 * No zip involved — plain parallel download of individual files.
 */
suspend fun BenchmarkScope.downloadOneByOne() {
    val downloadedFiles = span("download all") {
        coroutineScope {
            fileNames.map { async { it to downloadFileIntoBytes("/$it") } }.awaitAll().toMap()
        }
    }
    span("convert blobs") {
        coroutineScope {
            downloadedFiles.map { (k, v) ->
                async { k to v.toByteArray().size }
            }.awaitAll().toMap()
        }
    }
}

/**
 * Download each file individually as ArrayBuffer → copy ArrayBuffers into WASM linear memory →
 * read back as ByteArrays.
 */
suspend fun BenchmarkScope.downloadOneByOneWasmMemory() {
    val downloadedBuffers = span("download all") {
        coroutineScope {
            fileNames.map { async { it to downloadFileIntoBytes("/$it") } }.awaitAll()
        }
    }

    fastArraysToByteArrays(downloadedBuffers.map { (_, buf) -> buf })
}
