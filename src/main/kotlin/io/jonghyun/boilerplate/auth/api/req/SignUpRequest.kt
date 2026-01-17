package io.jonghyun.boilerplate.auth.api.req

data class SignUpRequest(
    val email: String,
    val password: String,
    val name : String
)