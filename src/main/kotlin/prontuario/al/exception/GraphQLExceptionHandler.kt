package prontuario.al.exception

import graphql.GraphQLError
import graphql.schema.DataFetchingEnvironment
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.stereotype.Component

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class GraphQLExceptionHandler : DataFetcherExceptionResolverAdapter() {
    override fun resolveToSingleError(
        exception: Throwable,
        env: DataFetchingEnvironment,
    ): GraphQLError =
        when (exception) {
            is GraphqlException -> exception.toGraphQLError()
            is AuthorizationDeniedException -> throw GraphqlException(
                "Unauthorized",
                errorType = CustomErrorClassification.UNAUTHORIZED,
                errorCode = GraphqlExceptionErrorCode.UNAUTHORIZED,
            )

            else -> handleGenericException(exception)
        }

    private fun handleGenericException(exception: Throwable): GraphQLError {
        logger.warn(exception.message, exception)
        throw GraphqlException(
            "An unexpected error occurred",
            errorType = CustomErrorClassification.INTERNAL_SERVER_ERROR,
            errorCode = GraphqlExceptionErrorCode.INTERNAL_SERVER_ERROR,
        )
    }
}
