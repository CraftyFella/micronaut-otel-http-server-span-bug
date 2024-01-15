package com.example

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.opentelemetry.api.trace.Tracer
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
class DbRepository(private val tracer: Tracer) {

    fun findAllBooks(): Mono<List<Book>> {
        val startSpan = tracer.spanBuilder("findAllBooks").startSpan()
        val makeCurrent = startSpan.makeCurrent()

        // Having a delay even for 0 MS causes the bug
        // Replacing with mono.just doesn't cause the bug
        return Mono.delay(Duration.ofMillis(0)).map { books }
            .doOnNext {
                makeCurrent.close()
                startSpan.end()
            }

    }

    fun findAllAuthors(): Mono<List<Author>> {
        val startSpan = tracer.spanBuilder("findAllAuthors").startSpan()

        return Mono.delay(Duration.ofMillis(0)).map { books.map(Book::author) }
            .doOnNext {
                startSpan.end()
            }
    }

    companion object {
        private val books = listOf(
            Book("book-1", "Harry Potter and the Philosopher's Stone", 223, Author("author-1", "Joanne", "Rowling")),
            Book("book-2", "Moby Dick", 635, Author("author-2", "Herman", "Melville")),
            Book("book-3", "Interview with the vampire", 371, Author("author-3", "Anne", "Rice"))
        )
    }
}