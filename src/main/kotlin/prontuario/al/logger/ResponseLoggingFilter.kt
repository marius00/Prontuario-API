package prontuario.al.logger

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingResponseWrapper
import java.io.IOException
import java.io.UnsupportedEncodingException

@Component
class ResponseLoggingFilter : OncePerRequestFilter() {
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val wrappedResponse = ContentCachingResponseWrapper(response)
        filterChain.doFilter(request, wrappedResponse)

        // Only log response for /graphql endpoint
        if (request.requestURI == "/graphql") {
            logResponse(wrappedResponse)
        }

        wrappedResponse.copyBodyToResponse() // Important: copy the cached body back to the original response
    }

    @Throws(UnsupportedEncodingException::class)
    private fun logResponse(response: ContentCachingResponseWrapper) {
        val content = response.contentAsByteArray
        if (content.size > 0) {
            val responseBody = String(content, charset(response.characterEncoding))
            logger.info("RESPONSE : {$responseBody}")
        }
    }
}
