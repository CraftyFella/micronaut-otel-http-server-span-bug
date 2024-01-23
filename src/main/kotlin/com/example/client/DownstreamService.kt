package com.example.client

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import reactor.core.publisher.Mono

@Client("https://httpstat.us/")
interface DownstreamService {
    @Get(uri = "/400")
    fun sad(): Mono<HttpResponse<String>>

    @Get(uri = "/200")
    fun happy(): Mono<HttpResponse<String>>
}
