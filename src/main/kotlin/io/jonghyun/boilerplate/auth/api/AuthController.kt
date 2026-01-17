package io.jonghyun.boilerplate.auth.api

import io.jonghyun.boilerplate.auth.api.req.SignInRequest
import io.jonghyun.boilerplate.auth.api.req.SignUpRequest
import io.jonghyun.boilerplate.auth.api.res.SignInResponse
import io.jonghyun.boilerplate.auth.application.AuthService
import io.jonghyun.boilerplate.support.auth.CurrentUser
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/sign-up")
    fun signUp(@RequestBody request: SignUpRequest): ResponseEntity<HttpStatus> {
        authService.signUp(
            email = request.email,
            password = request.password,
            name = request.name,
        )
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/sign-in")
    fun signIn(@RequestBody request: SignInRequest): ResponseEntity<SignInResponse> {
        val (accessToken, userId) = authService.login(
            email = request.email,
            password = request.password,
        )

        val response = SignInResponse(
            accessToken = accessToken,
            userId = userId,
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun testLogin(@CurrentUser userId: Long): String {
        return "Authenticated user ID: $userId"
    }
}