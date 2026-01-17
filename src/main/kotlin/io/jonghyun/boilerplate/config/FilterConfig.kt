package io.jonghyun.boilerplate.config

import io.jonghyun.boilerplate.auth.application.JwtTokenProvider
import io.jonghyun.boilerplate.auth.filter.JwtAuthenticationFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FilterConfig(
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @Bean
    fun jwtAuthenticationFilter(): FilterRegistrationBean<JwtAuthenticationFilter> {
        return FilterRegistrationBean<JwtAuthenticationFilter>().apply {
            filter = JwtAuthenticationFilter(jwtTokenProvider)
            order = 1
            addUrlPatterns("/*")
        }
    }
}