package com.example
import com.example.helpers.SpanSession
import io.micronaut.http.HttpRequest
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.opentelemetry.api.trace.SpanKind
import jakarta.inject.Inject
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.TestInstance

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpSpanTest {

    @Inject
    lateinit var session: SpanSession

    @RepeatedTest(2)
    fun `1 - get author by id - which does NOT use makeCurrent() will NOT fail second time`() {
        session.http<Book>(
            HttpRequest.GET("/api/authors/author-1")
        ) { ctx ->
            ctx.fetchSpanMatchingOrTimeout(ctx.traceId) { it.name == "GET /api/authors/{authorId}" && it.kind == SpanKind.SERVER }
        }
    }

    @RepeatedTest(2)
    fun `2 - get book by id - which uses makeCurrent() will fail second time`() {
        session.http<Book>(
            HttpRequest.GET("/api/books/book-1")
        ) { ctx ->
            ctx.fetchSpanMatchingOrTimeout(ctx.traceId) { it.name == "GET /api/books/{bookId}" && it.kind == SpanKind.SERVER }
        }
    }


}
