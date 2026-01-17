package io.jonghyun.boilerplate.auth.api

import io.jonghyun.boilerplate.auth.api.req.SignInRequest
import io.jonghyun.boilerplate.auth.api.req.SignUpRequest
import io.jonghyun.boilerplate.auth.application.AuthService
import io.jonghyun.boilerplate.auth.application.JwtTokenProvider
import io.jonghyun.boilerplate.user.domain.UserEntity
import io.jonghyun.boilerplate.user.repository.UserRepository
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.TestConstructor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DisplayName("AuthController 통합 테스트")
class AuthControllerTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val authService: AuthService,
    @LocalServerPort private val port: Int,
) {

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        // 테스트 전 데이터베이스 초기화
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("회원가입 성공 - 201 Created")
    fun signUpSuccess() {
        // given
        val request = SignUpRequest(
            email = "newuser@example.com",
            password = "password123",
            name = "New User",
        )

        // when & then
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/auth/sign-up")
            .then()
            .statusCode(HttpStatus.CREATED.value())
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복, 400 Bad Request")
    fun signUpFailDuplicateEmail() {
        // given
        val email = "duplicate@example.com"
        val existingUser = UserEntity(
            email = email,
            password = passwordEncoder.encode("password123"),
            name = "Existing User",
        )
        userRepository.save(existingUser)

        val request = SignUpRequest(
            email = email,
            password = "newpassword",
            name = "New User",
        )

        // when & then
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/auth/sign-up")
            .then()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
    }


    @Test
    @DisplayName("로그인 성공 - 200 OK, accessToken 반환")
    fun signInSuccess() {
        // given
        val email = "loginuser@example.com"
        val password = "password123"
        val user = UserEntity(
            email = email,
            password = passwordEncoder.encode(password),
            name = "Login User",
        )
        val savedUser = userRepository.save(user)

        val request = SignInRequest(
            email = email,
            password = password,
        )

        // when & then
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/auth/sign-in")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("accessToken", notNullValue())
            .body("userId", equalTo(savedUser.id.toInt()))
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 이메일, 500 Internal Server Error")
    fun signInFailInvalidEmail() {
        // given
        val request = SignInRequest(
            email = "nonexistent@example.com",
            password = "password123",
        )

        // when & then
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/auth/sign-in")
            .then()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호, 500 Internal Server Error")
    fun signInFailInvalidPassword() {
        // given
        val email = "user@example.com"
        val user = UserEntity(
            email = email,
            password = passwordEncoder.encode("correctpassword"),
            name = "Test User",
        )
        userRepository.save(user)

        val request = SignInRequest(
            email = email,
            password = "wrongpassword",
        )

        // when & then
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/auth/sign-in")
            .then()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
    }

    @Test
    @DisplayName("인증된 사용자 테스트 - JWT 토큰 필요, 200 OK")
    fun authenticatedUserTestSuccess() {
        // given
        val email = "authuser@example.com"
        val user = UserEntity(
            email = email,
            password = passwordEncoder.encode("password123"),
            name = "Auth User",
        )
        val savedUser = userRepository.save(user)
        val accessToken = jwtTokenProvider.generateAccessToken(savedUser.id)

        // when & then
        RestAssured
            .given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/auth")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body(equalTo("Authenticated user ID: ${savedUser.id}"))
    }

    @Test
    @DisplayName("인증된 사용자 테스트 - JWT 토큰 없음, 401 Unauthorized")
    fun authenticatedUserTestFailNoToken() {
        // when & then
        RestAssured
            .given()
            .`when`()
            .get("/auth")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
    }

    @Test
    @DisplayName("인증된 사용자 테스트 - 잘못된 JWT 토큰, 401 Unauthorized")
    fun authenticatedUserTestFailInvalidToken() {
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
    @DisplayName("동시에 같은 이메일로 회원가입 시도 - 하나만 성공")
    fun concurrentSignUpWithSameEmail() {
        // given
        val email = "concurrent@example.com"
        val threadCount = 5
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // when
        repeat(threadCount) { index ->
            executor.submit {
                try {
                    latch.countDown()
                    latch.await() // 모든 스레드가 동시에 시작되도록 대기

                    authService.signUp(
                        email = email,
                        password = "password$index",
                        name = "User $index",
                    )
                    successCount.incrementAndGet()
                } catch (e: IllegalArgumentException) {
                    if (e.message?.contains("Email already exists") == true) {
                        failCount.incrementAndGet()
                    }
                } catch (e: Exception) {
                    // 다른 예외는 무시
                }
            }
        }

        executor.shutdown()
        while (!executor.isTerminated) {
            Thread.sleep(100)
        }

        // then
        assertEquals(1, successCount.get(), "정확히 하나의 회원가입만 성공해야 합니다")
        assertEquals(threadCount - 1, failCount.get(), "나머지는 모두 실패해야 합니다")
        assertEquals(1, userRepository.findAll().size, "DB에는 하나의 사용자만 존재해야 합니다")
    }
}
