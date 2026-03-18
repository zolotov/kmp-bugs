@file:OptIn(ExperimentalWasmJsInterop::class)

import js.buffer.ArrayBuffer
import js.objects.unsafeJso
import js.typedarrays.Uint8Array
import web.blob.Blob
import web.http.*

suspend fun BenchmarkScope.downloadFileIntoBytes(url: String): Uint8Array<ArrayBuffer> {
    return span("fetch") {
        fetch(url = url, unsafeJso {
            method = RequestMethod.GET
            redirect = RequestRedirect.follow
            cache
        })
    }.let {
        span("bytes") {
            it.bytes()
        }
    }

}

suspend fun BenchmarkScope.downloadFileIntoArrayBuffer(url: String): ArrayBuffer {
    return span("fetch") {
        fetch(url = url, unsafeJso {
            method = RequestMethod.GET
            redirect = RequestRedirect.follow
        })
    }.let {
        span("arrayBuffer") {
            it.arrayBuffer()
        }
    }
}

suspend fun BenchmarkScope.downloadFileIntoBlob(url: String): Blob {
    return span("fetch") {
        fetch(url = url, unsafeJso {
            method = RequestMethod.GET
            redirect = RequestRedirect.follow
        })
    }.let {
        span("blob") {
            it.blob()
        }
    }
}

suspend fun BenchmarkScope.downloadFileAsText(url: String): String {
    return span("fetch") {
        fetch(url = url, unsafeJso {
            method = RequestMethod.GET
            redirect = RequestRedirect.follow
        })
    }.let {
        span("text") {
            it.text()
        }
    }
}

suspend fun BenchmarkScope.downloadFileAsJson(url: String): JsAny? {
    return span("fetch") {
        fetch(url = url, unsafeJso {
            method = RequestMethod.GET
            redirect = RequestRedirect.follow
        })
    }.let {
        span("json") {
            it.json()
        }
    }
}