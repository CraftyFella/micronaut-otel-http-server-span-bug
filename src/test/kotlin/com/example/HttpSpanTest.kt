package com.example
import com.example.helpers.SpanSession
import io.micronaut.http.HttpRequest
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.opentelemetry.api.trace.SpanKind
import jakarta.inject.Inject
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpSpanTest {

    @Inject
    lateinit var session: SpanSession

    @Test
    fun `manuallyCreatedSpan should become the current span so the next spans should be children`() {
        session.http<Book>(
            HttpRequest.GET("/api/books/book-1")
        ) { ctx ->
            val serverSpan = ctx.fetchSpanMatchingOrTimeout(ctx.traceId) { it.name == "/api/books/{bookId}" && it.kind == SpanKind.SERVER }
            val manuallyCreatedSpan = ctx.fetchSpanMatchingOrTimeout(ctx.traceId) { it.name == "manuallyCreatedSpan" && it.kind == SpanKind.INTERNAL && it.parentSpanId == serverSpan.spanId }
            ctx.fetchSpanMatchingOrTimeout(ctx.traceId) { it.name == "HTTP GET" && it.kind == SpanKind.CLIENT && it.parentSpanId == manuallyCreatedSpan.spanId }
            val dynamicallyNamedSpan = ctx.fetchSpanMatchingOrTimeout(ctx.traceId) { it.name == "finding book book-1" && it.kind == SpanKind.INTERNAL && it.parentSpanId == manuallyCreatedSpan.spanId }
            ctx.fetchSpanMatchingOrTimeout(ctx.traceId) { it.name == "DbRepository.findAllBooks" && it.kind == SpanKind.INTERNAL && it.parentSpanId == dynamicallyNamedSpan.spanId }
        }
    }

}
