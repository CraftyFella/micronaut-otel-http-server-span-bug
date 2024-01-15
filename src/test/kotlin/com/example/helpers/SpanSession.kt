package com.example.helpers

import com.example.doubles.InMemorySpanExporter
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.trace.data.SpanData
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.function.Consumer

@Singleton
class SpanSession(private val spanExporter: InMemorySpanExporter, private val tracer: Tracer) {

    @field:Client("/")
    @Inject
    lateinit var httpClient: HttpClient

    fun <T> http(request: MutableHttpRequest<T>, consumer: Consumer<HttpSpanSessionContext>) {
        val testSpan = tracer.spanBuilder("test").startSpan()
        val currentSpan = testSpan.makeCurrent()
        spanExporter.reset()
        httpClient.toBlocking().exchange(request, String::class.java)
        currentSpan.close()
        testSpan.end()
        consumer.accept(HttpSpanSessionContext(spanExporter, testSpan.spanContext.traceId))
        spanExporter.reset()
    }

}

class HttpSpanSessionContext(private val spanExporter: InMemorySpanExporter, val traceId: String) {
    fun fetchSpanMatchingOrTimeout(traceId: String, predicate: (SpanData) -> Boolean): SpanData =
        spanExporter.fetchSpanMatchingOrTimeout(traceId, predicate)

}
