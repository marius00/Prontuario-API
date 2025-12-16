package prontuario.al.config

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsRuntimeWiring
import graphql.schema.*
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaDirectiveWiring
import graphql.schema.idl.SchemaDirectiveWiringEnvironment
import java.text.Normalizer
import java.util.function.BiFunction

@DgsComponent
class AccentRemoverDirective : SchemaDirectiveWiring {

    companion object {
        private const val NAME = "AccentRemover"
    }

    @DgsRuntimeWiring
    fun runtimeWiring(builder: RuntimeWiring.Builder): RuntimeWiring.Builder {
        return builder.directiveWiring(AccentRemoverDirective())
    }

    override fun onField(env: SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition>): GraphQLFieldDefinition {
        val field = env.element
        if (field.getDirective(NAME) == null) {
            return field
        }

        val parentType = env.fieldsContainer as GraphQLObjectType

        // build a data fetcher that transforms the given value to uppercase/lowercase
        val coordinates = FieldCoordinates.coordinates(env.fieldsContainer.name, field.name)
        val originalFetcher = env.codeRegistry.getDataFetcher(coordinates, field)
        val dataFetcher = DataFetcherFactories.wrapDataFetcher(originalFetcher, transformFieldValue())

        // now change the field definition to use the new uppercase/lowercase data fetcher
        env.codeRegistry.dataFetcher(parentType, field, dataFetcher)
        return field
    }

    fun transformFieldValue(): BiFunction<DataFetchingEnvironment, Any?, Any?> {
        return BiFunction { _, value ->
            if (value is String) {
                Normalizer.normalize(value, Normalizer.Form.NFKD)
            } else {
                value
            }
        }
    }
}
