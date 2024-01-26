package com.example

import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQL
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLSchema
import graphql.schema.idl.*
import io.micronaut.configuration.graphql.GraphQLExecutionInputCustomizer
import io.micronaut.configuration.graphql.GraphQLInvocation
import io.micronaut.configuration.graphql.GraphQLInvocationData
import io.micronaut.context.BeanProvider
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Primary
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.core.io.ResourceResolver
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import jakarta.inject.Singleton
import org.dataloader.DataLoaderRegistry
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture

@Singleton
class GraphQLDataFetchers(private val dbRepository: DbRepository) {

    fun bookByIdDataFetcher(): DataFetcher<CompletableFuture<Book>> {
        return DataFetcher { dataFetchingEnvironment: DataFetchingEnvironment ->
            val bookId: String = dataFetchingEnvironment.getArgument("id")
            dbRepository.findAllBooks()
                .map {
                    it.first { book: Book -> (book.id == bookId) }
                }.toFuture()
        }
    }


}

@Factory
class GraphQLFactory {

    @Bean
    @Singleton
    fun graphQL(resourceResolver: ResourceResolver, graphQLDataFetchers: GraphQLDataFetchers): GraphQL {
        val schemaParser = SchemaParser()

        val typeRegistry = TypeDefinitionRegistry()
        val graphqlSchema = resourceResolver.getResourceAsStream("classpath:schema.graphqls")

        return if (graphqlSchema.isPresent) {
            typeRegistry.merge(schemaParser.parse(BufferedReader(InputStreamReader(graphqlSchema.get()))))
            val runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type(
                    TypeRuntimeWiring.newTypeWiring("Query")
                        .dataFetcher("bookById", graphQLDataFetchers.bookByIdDataFetcher())
                )
                .build()
            val schemaGenerator = SchemaGenerator()
            val graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring)
            GraphQL.newGraphQL(graphQLSchema).build()
        } else {
            LOG.debug("No GraphQL services found, returning empty schema")
            GraphQL.Builder(GraphQLSchema.newSchema().build()).build()
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(GraphQLFactory::class.java)
    }
}

@Singleton
class FixedGraphQLCustomizer : GraphQLExecutionInputCustomizer {
    override fun customize(
        executionInput: ExecutionInput,
        httpRequest: HttpRequest<*>?,
        httpResponse: MutableHttpResponse<String>?
    ): Publisher<ExecutionInput> =
        Flux.just(executionInput)
}

@Singleton
@Primary
class FixedGraphQLInvocation(
    private val graphQL: GraphQL,
    private val customizer: GraphQLExecutionInputCustomizer,
    private val dataLoaderRegistry: BeanProvider<DataLoaderRegistry>?
) : GraphQLInvocation {
    override fun invoke(
        invocationData: GraphQLInvocationData?,
        httpRequest: HttpRequest<*>?,
        httpResponse: MutableHttpResponse<String>?
    ): Publisher<ExecutionResult> {
        val executionInputBuilder = ExecutionInput.newExecutionInput()
            .query(invocationData!!.query)
            .operationName(invocationData.operationName)
            .variables(invocationData.variables)
        if (dataLoaderRegistry != null) {
            executionInputBuilder.dataLoaderRegistry(dataLoaderRegistry.get())
        }
        val executionInput = executionInputBuilder.build()
        return Flux
            .from(customizer.customize(executionInput, httpRequest, httpResponse))
            .flatMap { customizedExecutionInput: ExecutionInput? ->
                Mono.fromFuture {
                    try {
                        return@fromFuture graphQL.executeAsync(customizedExecutionInput)
                    } catch (e: Throwable) {
                        val future: CompletableFuture<ExecutionResult> = CompletableFuture()
                        future.completeExceptionally(e)
                        return@fromFuture future
                    }
                }
            }
    }
}