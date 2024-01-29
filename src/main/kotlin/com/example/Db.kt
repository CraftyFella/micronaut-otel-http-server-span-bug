package com.example

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import jakarta.inject.Singleton
import reactor.core.publisher.Mono
import java.time.Duration

@Introspected
@Serdeable
class Book(val id: String, val name: String, val pageCount: Int, val author: Author)

@Introspected
@Serdeable
class Author(val id: String, val firstName: String, val lastName: String)

@Singleton
class DbRepository(private val requestScopedThing: RequestScopedThing) {

    fun findAllBooks(): Mono<List<Book>> {
        requestScopedThing.printId()

        return Mono
            .delay(Duration.ofMillis(0))
            .map {
                books }
    }


    companion object {
        private val books = listOf(
            Book("book-1", "Harry Potter and the Philosopher's Stone", 223, Author("author-1", "Joanne", "Rowling")),
            Book("book-2", "Moby Dick", 635, Author("author-2", "Herman", "Melville")),
            Book("book-3", "Interview with the vampire", 371, Author("author-3", "Anne", "Rice"))
        )
    }
}