package com.example

import com.example.client.DownstreamService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.runtime.http.scope.RequestScope
import io.micronaut.tracing.annotation.NewSpan
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import reactor.core.publisher.Mono


@RequestScope
class RequestScopedThing {
    val id = System.currentTimeMillis()
    fun printId() {
        println("RequestScopedThing id: $id")
    }

}

@Controller("/api")
open class ApiController(private val tracer: Tracer,  private val downstreamService: DownstreamService, private val dbRepository: DbRepository, private val requestScopedThing: RequestScopedThing) {
    @Get(uri = "books/{bookId}")
    fun getBookById(@PathVariable(value = "bookId") bookId: String): Mono<HttpResponse<Book>>
    {
        requestScopedThing.printId()

        val span = tracer.spanBuilder("manuallyCreatedSpan").startSpan()
        val scope = span.makeCurrent()

        return downstreamService.happy()
            .flatMap {
                findBookById(bookId)
            }.map<HttpResponse<Book>?> {
                HttpResponse.ok()
            }
            .doOnNext {
                scope.close()
                span.end()
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




