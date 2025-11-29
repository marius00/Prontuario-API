package prontuario.al.exception

import prontuario.al.logger.GraphqlLoggingFilter.Companion.logger
import graphql.ErrorClassification
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder

data class GraphqlException(
    override val message: String,
    val errorType: ErrorClassification = CustomErrorClassification.BAD_REQUEST,
    val causedBy: Exception? = null,
    val errorCode: GraphqlExceptionErrorCode? = null,
) : Exception(message) {
    fun toGraphQLError(): GraphQLError {
        logger.warn(throwable = causedBy, message = { message })
        val fallback: String = if (errorType is CustomErrorClassification) {
            errorType.name
        } else {
            "GENERIC"
        }

        val errorCodeStr = errorCode?.name ?: fallback
        return GraphqlErrorBuilder
            .newError()
            .message(message)
            .errorType(errorType)
            .extensions(mapOf("classification" to errorCodeStr))
            .build()
    }
}

enum class GraphqlExceptionErrorCode {
    ALREADY_EXISTS,
    VALIDATION,
    UNAUTHORIZED,
    INTERNAL_SERVER_ERROR,
}
