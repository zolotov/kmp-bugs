import kotlin.time.measureTimedValue

class BenchmarkScope {
    val spans: MutableList<StepTiming> = mutableListOf()

    inline fun <T> span(name: String, block: () -> T): T {
        return measureTimedValue { block() }.also {
            spans.add(StepTiming(name, it.duration))
        }.value
    }
}