package prontuario.al.exception

import graphql.ErrorClassification

enum class CustomErrorClassification : ErrorClassification {
    INTERNAL_SERVER_ERROR,
    BAD_REQUEST,
    UNAUTHORIZED,
}
