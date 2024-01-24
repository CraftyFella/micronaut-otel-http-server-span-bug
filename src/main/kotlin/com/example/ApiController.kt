package com.example

import com.example.client.DownstreamService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.tracing.annotation.NewSpan
import reactor.core.publisher.Mono

@Controller("/api")
open class ApiController(private val downstreamService: DownstreamService, private val dbRepository: DbRepository) {
    @Get(uri = "books/{bookId}")
    fun getBookById(@PathVariable(value = "bookId") bookId: String): Mono<HttpResponse<Book>>
    {
        return downstreamService.happy()
            .flatMap {
                findBookById(bookId)
            }.map {
                HttpResponse.ok()
            }
    }

    @NewSpan("my-fake-db")
    open fun findBookById(id: String) =
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