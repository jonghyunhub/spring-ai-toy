package io.jonghyun.boilerplate.auth.filter

import io.jonghyun.boilerplate.auth.application.JwtTokenProvider
import io.jonghyun.boilerplate.support.auth.AuthContext
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private val EXCLUDED_PATHS = listOf(
            "/auth/sign-in",
            "/auth/sign-up",
            "/actuator",
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            if (shouldSkipFilter(request)) {
                filterChain.doFilter(request, response)
                return
            }

            val token = extractToken(request)
            if (token != null && jwtTokenProvider.validateToken(token)) {
                val userId = jwtTokenProvider.getUserId(token)
                AuthContext.setUserId(userId)
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing JWT token")
                return
            }

            filterChain.doFilter(request, response)
        } finally {
            AuthContext.clear()
        }
    }

    private fun shouldSkipFilter(request: HttpServletRequest): Boolean {
        val requestUri = request.requestURI
        return EXCLUDED_PATHS.any { requestUri.startsWith(it) }
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(AUTHORIZATION_HEADER) ?: return null

        return if (bearerToken.startsWith(BEARER_PREFIX)) {
            bearerToken.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }
}
