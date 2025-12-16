package prontuario.al.config

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsRuntimeWiring
import graphql.language.EnumValue
import graphql.schema.*
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaDirectiveWiring
import graphql.schema.idl.SchemaDirectiveWiringEnvironment
import graphql.validation.rules.OnValidationErrorStrategy
import graphql.validation.rules.ValidationRules
import graphql.validation.schemawiring.ValidationSchemaWiring
import prontuario.al.generated.types.Case
import java.util.function.BiFunction

@DgsComponent
class StringCaseDirective : SchemaDirectiveWiring {

    companion object {
        private const val NAME = "StringCase"
        private const val ARGUMENT_NAME = "case"
    }

    @DgsRuntimeWiring
    fun runtimeWiring(builder: RuntimeWiring.Builder): RuntimeWiring.Builder {
        return builder.directiveWiring(StringCaseDirective())
    }

    @Suppress("DEPRECATION")
    override fun onField(env: SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition>): GraphQLFieldDefinition {
        val field = env.element
        if (field.getDirective(NAME) == null) {
            return field
        }
        val casingAsString = (field.getDirective(NAME).getArgument(ARGUMENT_NAME).argumentValue.value as EnumValue).name
        val casing = Case.valueOf(casingAsString)

        val parentType = env.fieldsContainer as GraphQLObjectType

        // build a data fetcher that transforms the given value to uppercase/lowercase
        val coordinates = FieldCoordinates.coordinates(env.fieldsContainer.name, field.name)
        val originalFetcher = env.codeRegistry.getDataFetcher(coordinates, field)
        val dataFetcher = DataFetcherFactories.wrapDataFetcher(originalFetcher, transformFieldValue(casing))

        // now change the field definition to use the new uppercase/lowercase data fetcher
        env.codeRegistry.dataFetcher(parentType, field, dataFetcher)
        return field
    }

    fun transformFieldValue(casing: Case): BiFunction<DataFetchingEnvironment, Any?, Any?> {
        return BiFunction { _, value ->
            if (value is String) {
                when (casing) {
                    Case.LOWER -> value.lowercase()
                    Case.UPPER -> value.uppercase()
                }
            } else {
                value
            }
        }
    }
}
@DgsComponent
class AddExtendedValidationDirectiveWiring {
    @DgsRuntimeWiring
    fun addGraphqlJavaExtendedValidationDirectives(builder: RuntimeWiring.Builder): RuntimeWiring.Builder {
        // We register all the directives (see directives.graphqls) of graphql-java-extended-validation and let dgs know
        val validationRules: ValidationRules = ValidationRules.newValidationRules()
            .onValidationErrorStrategy(OnValidationErrorStrategy.RETURN_NULL)
            .build()

        // This will rewrite your data fetchers when rules apply to them so that they get validated
        val schemaWiring = ValidationSchemaWiring(validationRules)
        return builder.directiveWiring(schemaWiring)
    }
}
