package com.example.doubles

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Replaces
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import jakarta.inject.Singleton

@Factory
class InMemorySpanProcessorFactory {
    @Singleton
    @Replaces(bean = SpanProcessor::class)
    fun spanProcessor(spanExporter: InMemorySpanExporter): SpanProcessor {
        return SimpleSpanProcessor.create(spanExporter)
    }

}