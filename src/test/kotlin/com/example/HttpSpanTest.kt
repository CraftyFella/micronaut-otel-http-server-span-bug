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

    @RepeatedTest(2)
    fun `get book by id - which uses makeCurrent() will fail second time as it will get attached to the wrong parent`() {
        session.http<Book>(
            HttpRequest.GET("/api/books/book-1")
        ) { ctx ->
            val serverSpan = ctx.fetchSpanMatchingOrTimeout(ctx.traceId) { it.name == "GET /api/books/{bookId}" && it.kind == SpanKind.SERVER }
            val manuallyCreatedSpan = ctx.fetchSpanMatchingOrTimeout(ctx.traceId) { it.name == "manuallyCreatedSpan" && it.kind == SpanKind.INTERNAL && it.parentSpanId == serverSpan.spanId }
        }
    }

    @Test
    fun `manuallyCreatedSpan should become the current span so the next spans should be children`() {
        session.http<Book>(
            HttpRequest.GET("/api/books/book-1")
        ) { ctx ->
            val serverSpan = ctx.fetchSpanMatchingOrTimeout(ctx.traceId) { it.name == "GET /api/books/{bookId}" && it.kind == SpanKind.SERVER }
            val manuallyCreatedSpan = ctx.fetchSpanMatchingOrTimeout(ctx.traceId) { it.name == "manuallyCreatedSpan" && it.kind == SpanKind.INTERNAL && it.parentSpanId == serverSpan.spanId }
            val clientSpan = ctx.fetchSpanMatchingOrTimeout(ctx.traceId) { it.name == "GET" && it.kind == SpanKind.CLIENT && it.parentSpanId == manuallyCreatedSpan.spanId }
        }
    }

}
