package com.example

import com.example.client.DownstreamService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.tracing.annotation.NewSpan
import io.opentelemetry.api.trace.Span
import reactor.core.publisher.Mono

@Controller("/api")
open class ApiController(private val downstreamService: DownstreamService, private val dbRepository: DbRepository) {
    @Get(uri = "books/{bookId}")
    fun getBookById(@PathVariable(value = "bookId") bookId: String): Mono<HttpResponse<Book>>
    {
        return downstreamService.happy()
            .flatMap {
                findBookById(bookId)
            }.map<HttpResponse<Book>?> {
                HttpResponse.ok()
            }
    }

    @NewSpan("my-fake-db")
    open fun findBookById(id: String): Mono<Book> {
        Span.current().updateName("finding book $id")
        return dbRepository.findAllBooks().map { books ->
            books.single {
                it.id == id
            }
        }
    }

}