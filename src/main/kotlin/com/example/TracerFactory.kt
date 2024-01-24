package com.example

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import jakarta.inject.Singleton

@Factory
class TracerFactory {
	@Singleton
	fun tracer(openTelemetry: OpenTelemetry, @Value("\${micronaut.application.name:app}") name: String): Tracer {
		return openTelemetry.getTracer(name)
	}

	@Singleton
	fun openTelemetry(): OpenTelemetry {
		return GlobalOpenTelemetry.get()
	}
}