package prontuario.al.logger

import graphql.execution.instrumentation.ExecutionStrategyInstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.SimplePerformantInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationExecutionStrategyParameters
import graphql.language.EnumValue
import graphql.schema.GraphQLObjectType
import io.github.oshai.kotlinlogging.NamedKLogging
import org.springframework.stereotype.Component

@Component
class GraphqlLoggingFilter : SimplePerformantInstrumentation() {
    companion object : NamedKLogging("GELF_UDP")

    override fun beginExecutionStrategy(
        parameters: InstrumentationExecutionStrategyParameters?,
        state: InstrumentationState?,
    ): ExecutionStrategyInstrumentationContext? {
        val endpoint = parameters
            ?.executionStrategyParameters
            ?.fields
            ?.keys
            ?.first()
        if (endpoint != null) {
            LoggingUtil.mdcPut("graphql_endpoint", endpoint)
        }

        val type = (
            parameters
                ?.executionStrategyParameters
                ?.executionStepInfo
                ?.type as? GraphQLObjectType
        )?.name

        if (type == "Query" && endpoint != null) {
            val arguments = parameters.executionStrategyParameters.fields.subFields[endpoint]
                ?.singleField
                ?.arguments
            arguments?.forEach { arg ->
                if (arg.value is EnumValue) {
                    LoggingUtil.mdcPut("graphql.arg." + arg.name, arg.value.toString())
                }

                // TODO: Split this into more types..
                // TODO: Stick this into a json object or something?
                LoggingUtil.mdcPut("graphql.arg." + arg.name, arg.value.toString())
            }
        }
        LoggingUtil.mdcPut("requestId", LoggingUtil.generateUUID())
        logger.info { "Received GraphQL $type: $endpoint" }

        return ExecutionStrategyInstrumentationContext.NOOP
    }
}
