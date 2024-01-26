package com.example

import com.example.client.DownstreamService
import io.micronaut.core.propagation.PropagatedContext
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.runtime.http.scope.RequestScope
import io.micronaut.tracing.annotation.NewSpan
import io.micronaut.tracing.opentelemetry.OpenTelemetryPropagationContext
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import reactor.core.publisher.Mono


@RequestScope
class RequestScopedThing {
    val id = System.currentTimeMillis()
    fun printId() {
        println("RequestScopedThing id: $id")
    }

}

@Controller("/api")
open class ApiController(
    private val tracer: Tracer,
    private val downstreamService: DownstreamService,
    private val dbRepository: DbRepository,
    private val requestScopedThing: RequestScopedThing
) {
    @Get(uri = "books/{bookId}")
    fun getBookById(@PathVariable(value = "bookId") bookId: String): Mono<HttpResponse<Book>> {
        requestScopedThing.printId()
        // This should always be avoided as it is implementation dependent, use @NewSpan instead
        val span = tracer.spanBuilder("manuallyCreatedSpan").startSpan()
        try {
            span.makeCurrent().use {
                // Based on: https://github.com/micronaut-projects/micronaut-tracing/blob/87a7f7e83ebd4f29bb364bd41a3f1f640f274966/tracing-opentelemetry/src/main/java/io/micronaut/tracing/opentelemetry/interceptor/NewSpanOpenTelemetryTraceInterceptor.java#L96
                // without this the Span would be created, but still its parent span would be used as parents to all
                // further child spans created within this block
                PropagatedContext.getOrEmpty()
                    .plus(OpenTelemetryPropagationContext(Context.current()))
                    .propagate().use {
                        return downstreamService.happy()
                            .flatMap {
                                findBookById(bookId)
                            }.map<HttpResponse<Book>?> {
                                span.end()
                                HttpResponse.ok()
                            }
                            .doOnError {
                                span.recordException(it)
                                span.end()
                            }
                    }
            }
        } catch (t: Throwable) {
            // this is highly unlikely, but still part of fully handling all error scenarios, i.e. NPE from
            // assembling the reactor publishers
            span.recordException(t)
            throw t
        }
    }

    @NewSpan("annotationSpan")
    open fun findBookById(id: String): Mono<Book> {
        Span.current().updateName("finding book $id")
        return dbRepository.findAllBooks().map { books ->
            books.single {
                it.id == id
            }
        }
    }

}