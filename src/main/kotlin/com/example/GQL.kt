package com.example

import graphql.GraphQL
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.TypeRuntimeWiring
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.core.io.ResourceResolver
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
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
                    .dataFetcher("bookById", graphQLDataFetchers.bookByIdDataFetcher()))
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