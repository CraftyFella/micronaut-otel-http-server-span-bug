package com.example

import com.example.client.DownstreamService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.opentelemetry.api.trace.Tracer
import reactor.core.publisher.Mono
import io.opentelemetry.instrumentation.annotations.WithSpan

@Controller("/api")
open class ApiController(private val downstreamService: DownstreamService, private val dbRepository: DbRepository, private val tracer: Tracer) {
    @Get(uri = "books/{bookId}")
    fun getBookById(@PathVariable(value = "bookId") bookId: String): Mono<HttpResponse<Book>>
    {
        val span = tracer.spanBuilder("dave").startSpan()
        val current = span.makeCurrent()
        return downstreamService.happy()
            .flatMap {
                findBookById(bookId)
            }.map<HttpResponse<Book>?> {
                HttpResponse.ok()
            }
            .doOnNext {
                current.close()
                span.end()
            }
    }

    @WithSpan
    open fun findBookById(id: String): Mono<Book> =
        dbRepository.findAllBooks().map { books ->
            books.single {
                it.id == id
            }
        }

//    @Get(uri = "authors/{authorId}")
//    fun getAuthorById(@PathVariable(value = "authorId") authorId: String): Mono<HttpResponse<Author>>
//    {
//        return db.findAllAuthors()
//            .map {
//                it.single { author -> author.id == authorId }
//            }
//            .map {
//                HttpResponse.ok(it)
//            }
//    }
}