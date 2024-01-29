package com.example.doubles

import com.example.helpers.SpanPrinter.printSpanTreeWithHeaderAndFooter
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import jakarta.inject.Singleton
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeoutException

@Singleton
class InMemorySpanExporter: SpanExporter {
    private val finishedSpanItems: Queue<SpanData> = ConcurrentLinkedQueue()
    private var isStopped = false

    private fun getFinishedSpanItems(): List<SpanData> {
        return finishedSpanItems.toList()
    }

    fun reset() {
        finishedSpanItems.clear()
    }

    override fun export(spans: Collection<SpanData>): CompletableResultCode {
        if (this.isStopped) {
            return CompletableResultCode.ofFailure()
        } else {
            finishedSpanItems.addAll(spans)
            return CompletableResultCode.ofSuccess()
        }
    }

    override fun flush(): CompletableResultCode {
        return CompletableResultCode.ofSuccess()
    }

    override fun shutdown(): CompletableResultCode {
        finishedSpanItems.clear()
        this.isStopped = true
        return CompletableResultCode.ofSuccess()
    }

    fun fetchSpanMatchingOrTimeout(traceId: String, predicate: (SpanData) -> Boolean): SpanData {
        return tryWithTimeout(10000) {
            val spansMatchingTraceId = fetchSpansMatchingTraceId(traceId)
            spansMatchingTraceId.find { predicate(it) } ?: throw NoSuchElementException("No span matching predicate.")
        }
    }

    private fun fetchSpansMatchingTraceId(traceId: String): List<SpanData> {
        return fetchSpansMatching { it.traceId == traceId }
    }

    private fun fetchSpansMatching(predicate: (SpanData) -> Boolean): List<SpanData> {
        val allTraces = getFinishedSpanItems()
        printSpanTreeWithHeaderAndFooter(allTraces.toList())
        return allTraces.filter { predicate(it) }
    }

    private fun <T> tryWithTimeout(timeoutMillis: Long, function: () -> T): T {
        var result: T? = null
        var lastException: Exception? = null
        val startTime = System.currentTimeMillis()
        val sleepTime = timeoutMillis / 20
        var attempt = 0
        var elapsedTimeMs = System.currentTimeMillis() - startTime

        while (result == null && elapsedTimeMs < timeoutMillis) {
            try {
                println("attempt ${++attempt} at $elapsedTimeMs ms")
                result = function.invoke()
            } catch (e: Exception) {
                println("exception $attempt ${e.message}")
                lastException = e
            } finally {
                if (result == null) {
                    Thread.sleep(sleepTime)
                    elapsedTimeMs = System.currentTimeMillis() - startTime
                }
            }
        }

        if (result == null) {
            val timeoutException = TimeoutException("Timeout after $timeoutMillis ms")
            timeoutException.addSuppressed(lastException)
            throw timeoutException
        }

        return result
    }

}