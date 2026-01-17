package io.jonghyun.boilerplate.auth.api.req

data class SignInRequest(
    val email: String,
    val password: String,
)
