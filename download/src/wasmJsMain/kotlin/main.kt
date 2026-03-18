@file:OptIn(ExperimentalWasmJsInterop::class, UnsafeWasmMemoryApi::class)

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import web.cssom.ClassName
import web.dom.Element
import web.dom.ElementId
import web.dom.document
import web.events.addEventListener
import web.html.HTMLButtonElement
import web.html.HTMLTableRowElement
import web.html.HTMLTableSectionElement
import web.html.HtmlSource
import web.pointer.CLICK
import web.pointer.PointerEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi

data class StepTiming(val name: String, val duration: Duration)

private data class BenchmarkEntry(
    val name: String,
    val runner: suspend BenchmarkScope.() -> Unit,
    var resultCell: Element? = null,
    var button: HTMLButtonElement? = null,
)

fun main() {
    GlobalScope.launch {
        val benchmarks = listOf(
            BenchmarkEntry("Download JSON", runner = { downloadFileAsJson("/bundles.json") }),
            BenchmarkEntry("Download Bytes", runner = { downloadFileIntoBytes("/bundles.json") }),
            BenchmarkEntry("Download Blob", runner = { downloadFileIntoBlob("/bundles.json") }),
            BenchmarkEntry("Download ArrayBuffer", runner = { downloadFileIntoArrayBuffer("/bundles.json") }),
            BenchmarkEntry("Download Text", runner = { downloadFileAsText("/bundles.json") }),
            BenchmarkEntry("Fleet JSZip Load", runner = { fleetJsZipLoad() }),
            BenchmarkEntry("JSZip Load", runner = { jsZipLoad() }),
            BenchmarkEntry("fflate (Linear Memory)", runner = { fflateWasmMemory() }),
            BenchmarkEntry("JSZip (Linear Memory)", runner = { jsZipWasmMemory() }),
            BenchmarkEntry("Download One-by-One", runner = { downloadOneByOne() }),
            BenchmarkEntry("Download One-by-One (Linear Memory)", runner = { downloadOneByOneWasmMemory() }),
            BenchmarkEntry("Tar", runner = { tarLoad() }),
            BenchmarkEntry("Tar (Linear Memory)", runner = { tarLoadBytesWasmMemory() }),
        )

        val tbody = document.getElementById(ElementId("benchmark-body")) as HTMLTableSectionElement

        for (entry in benchmarks) {
            val row = document.createElement("tr") as HTMLTableRowElement

            val nameCell = document.createElement("td")
            nameCell.textContent = entry.name
            row.appendChild(nameCell)

            val resultsCell = document.createElement("td")
            resultsCell.className = ClassName("results")
            resultsCell.innerHTML = HtmlSource("<span class=\"pending\">pending...</span>")
            row.appendChild(resultsCell)
            entry.resultCell = resultsCell


            val buttonCell = document.createElement("td")
            val btn = document.createElement("button") as HTMLButtonElement
            btn.textContent = "Run"
            entry.button = btn
            btn.addEventListener(PointerEvent.CLICK, handler = { MainScope().runBenchmark(entry) })
            buttonCell.appendChild(btn)
            row.appendChild(buttonCell)

            tbody.appendChild(row)
        }

        val runAllBtn = document.getElementById(ElementId("run-all-btn")) as HTMLButtonElement
        runAllBtn.addEventListener(PointerEvent.CLICK, handler = {
            MainScope().launch {
                runAllBtn.disabled = true
                runAllBtn.textContent = "Running..."
                for (entry in benchmarks) {
                    runBenchmarkAsync(entry)
                }
                runAllBtn.disabled = false
                runAllBtn.textContent = "Run All"
            }
        })
    }
}

private fun CoroutineScope.runBenchmark(entry: BenchmarkEntry) {
    launch {
        runBenchmarkAsync(entry)
    }
}

private suspend fun runBenchmarkAsync(entry: BenchmarkEntry) {
    val btn = entry.button ?: return
    val cell = entry.resultCell ?: return
    btn.disabled = true
    btn.textContent = "Running..."
    cell.innerHTML = HtmlSource("<span class=\"running\">running...</span>")
    try {
        val benchmarkScope = BenchmarkScope()
        entry.run { benchmarkScope.runner() }
        val steps = benchmarkScope.spans
        val total = steps.fold(0.milliseconds) { acc, d -> acc + d.duration }
        val html = buildString {
            for (step in steps) {
                val cls = when {
                    step.duration < 50.milliseconds -> "step-fast"
                    step.duration < 300.milliseconds -> "step-mid"
                    else -> "step-slow"
                }
                append("<span class=\"step\"><span class=\"step-name\">${step.name}:</span> ")
                append("<span class=\"step-value $cls\">${step.duration}</span></span>")
            }
            append("<span class=\"total\">total: $total</span>")
        }
        cell.innerHTML = HtmlSource(html)
    } catch (e: Throwable) {
        e.printStackTrace()
        cell.innerHTML = HtmlSource("<span class=\"error\">Error: ${e.message}</span>")
    }
    btn.disabled = false
    btn.textContent = "Run"
}

