package io.jonghyun.boilerplate.auth.api.res

data class SignInResponse(
    val accessToken: String,
    val userId: Long,
)