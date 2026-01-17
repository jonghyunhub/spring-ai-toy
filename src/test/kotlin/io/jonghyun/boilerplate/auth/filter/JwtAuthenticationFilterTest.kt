package io.jonghyun.boilerplate.auth.filter

import io.jonghyun.boilerplate.auth.application.JwtTokenProvider
import io.jonghyun.boilerplate.user.domain.UserEntity
import io.jonghyun.boilerplate.user.repository.UserRepository
import io.restassured.RestAssured
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.TestConstructor

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DisplayName("JwtAuthenticationFilter 통합 테스트")
class JwtAuthenticationFilterTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    @LocalServerPort private val port: Int,
) {

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        // 테스트 전 데이터베이스 초기화
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("제외 경로 테스트 - /auth/sign-in 토큰 없이 접근 가능")
    fun excludedPathSignIn() {
        // when & then
        RestAssured
            .given()
            .contentType("application/json")
            .body("""{"email": "test@example.com", "password": "password"}""")
            .`when`()
            .post("/auth/sign-in")
            .then()
            // 유효하지 않은 자격 증명이므로 500이 나올 수 있지만, 401(인증 필터)이 아님을 확인
            .statusCode(org.hamcrest.Matchers.not(HttpStatus.UNAUTHORIZED.value()))
    }

    @Test
    @DisplayName("제외 경로 테스트 - /auth/sign-up 토큰 없이 접근 가능")
    fun excludedPathSignUp() {
        // when & then
        RestAssured
            .given()
            .contentType("application/json")
            .body("""{"email": "newuser@example.com", "password": "password123", "name": "Test User"}""")
            .`when`()
            .post("/auth/sign-up")
            .then()
            .statusCode(HttpStatus.CREATED.value())
    }

    @Test
    @DisplayName("유효한 토큰으로 인증 성공 - 200 OK")
    fun validTokenAuthentication() {
        // given
        val user = UserEntity(
            email = "authuser@example.com",
            password = passwordEncoder.encode("password123"),
            name = "Auth User",
        )
        val savedUser = userRepository.save(user)
        val validToken = jwtTokenProvider.generateAccessToken(savedUser.id)

        // when & then
        RestAssured
            .given()
            .header("Authorization", "Bearer $validToken")
            .`when`()
            .get("/auth")
            .then()
            .statusCode(HttpStatus.OK.value())
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 401 Unauthorized 반환")
    fun invalidTokenReturns401() {
        // given
        val invalidToken = "invalid.jwt.token"

        // when & then
        RestAssured
            .given()
            .header("Authorization", "Bearer $invalidToken")
            .`when`()
            .get("/auth")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
    }

    @Test
    @DisplayName("Authorization 헤더 없을 때 401 Unauthorized 반환")
    fun noAuthorizationHeaderReturns401() {
        // when & then
        RestAssured
            .given()
            .`when`()
            .get("/auth")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
    }

    @Test
    @DisplayName("Bearer 접두사 없는 토큰으로 401 Unauthorized 반환")
    fun tokenWithoutBearerPrefixReturns401() {
        // given
        val user = UserEntity(
            email = "user@example.com",
            password = passwordEncoder.encode("password123"),
            name = "Test User",
        )
        val savedUser = userRepository.save(user)
        val token = jwtTokenProvider.generateAccessToken(savedUser.id)

        // when & then
        RestAssured
            .given()
            .header("Authorization", token) // Bearer 접두사 없이
            .`when`()
            .get("/auth")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
    }

    @Test
    @DisplayName("만료된 토큰으로 401 Unauthorized 반환")
    fun expiredTokenReturns401() {
        // given
        val user = UserEntity(
            email = "user@example.com",
            password = passwordEncoder.encode("password123"),
            name = "Test User",
        )
        val savedUser = userRepository.save(user)

        // 만료된 토큰 생성 (validity를 음수로 설정)
        val expiredTokenProvider = JwtTokenProvider(
            secretKey = "test-secret-key-for-unit-and-integration-tests-must-be-at-least-256-bits-long",
            accessTokenValidity = -1000L,
        )
        val expiredToken = expiredTokenProvider.generateAccessToken(savedUser.id)

        // when & then
        RestAssured
            .given()
            .header("Authorization", "Bearer $expiredToken")
            .`when`()
            .get("/auth")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
    }

    @Test
    @DisplayName("제외 경로 테스트 - /actuator 토큰 없이 접근 가능")
    fun excludedPathActuator() {
        // when & then
        RestAssured
            .given()
            .`when`()
            .get("/actuator/health")
            .then()
            // actuator 엔드포인트가 존재하면 200, 없으면 404이지만 401은 아님
            .statusCode(org.hamcrest.Matchers.not(HttpStatus.UNAUTHORIZED.value()))
    }
}
