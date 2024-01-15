package com.example

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import reactor.core.publisher.Mono

@Controller("/api")
class ApiController(private val db: DbRepository) {
    @Get(uri = "books/{bookId}")
    fun getBookById(@PathVariable(value = "bookId") bookId: String): Mono<HttpResponse<Book>>
    {
        return db.findAllBooks()
            .map {
                it.single { book -> book.id == bookId }
            }
            .map {
                HttpResponse.ok(it)
            }
    }
    @Get(uri = "authors/{authorId}")
    fun getAuthorById(@PathVariable(value = "authorId") authorId: String): Mono<HttpResponse<Author>>
    {
        return db.findAllAuthors()
            .map {
                it.single { author -> author.id == authorId }
            }
            .map {
                HttpResponse.ok(it)
            }
    }
}