package com.example

import com.example.client.DownstreamService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.opentelemetry.api.trace.Tracer
import reactor.core.publisher.Mono

@Controller("/api")
class ApiController(private val downstreamService: DownstreamService) {
    @Get(uri = "books/{bookId}")
    fun getBookById(@PathVariable(value = "bookId") bookId: String): Mono<HttpResponse<Book>>
    {
        return downstreamService.happy().map {
            HttpResponse.ok()
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