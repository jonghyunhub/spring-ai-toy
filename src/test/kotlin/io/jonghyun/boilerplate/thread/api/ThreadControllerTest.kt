package io.jonghyun.boilerplate.thread.api

import io.jonghyun.boilerplate.auth.application.JwtTokenProvider
import io.jonghyun.boilerplate.thread.repository.ThreadRepository
import io.jonghyun.boilerplate.user.domain.UserEntity
import io.jonghyun.boilerplate.user.repository.UserRepository
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.hasSize
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
@DisplayName("ThreadController 통합 테스트")
class ThreadControllerTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val threadRepository: ThreadRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    @LocalServerPort private val port: Int,
) {

    private lateinit var accessToken: String
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port

        // 테스트 전 데이터베이스 초기화
        threadRepository.deleteAll()
        userRepository.deleteAll()

        // 테스트 사용자 생성 및 토큰 발급
        val user = UserEntity(
            email = "threadtest@example.com",
            password = passwordEncoder.encode("password123"),
            name = "Thread Test User",
        )
        val savedUser = userRepository.save(user)
        userId = savedUser.id
        accessToken = jwtTokenProvider.generateAccessToken(userId)
    }

    @Test
    @DisplayName("스레드 생성 성공 - 201 Created")
    fun createThreadSuccess() {
        // when & then
        RestAssured
            .given()
            .header("Authorization", "Bearer $accessToken")
            .contentType(ContentType.JSON)
            .`when`()
            .post("/threads")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("threadId", greaterThan(0))
            .body("userId", equalTo(userId.toInt()))
    }

    @Test
    @DisplayName("스레드 목록 조회 성공 - 200 OK")
    fun getThreadsSuccess() {
        // given - 스레드 2개 생성
        RestAssured
            .given()
            .header("Authorization", "Bearer $accessToken")
            .post("/threads")

        RestAssured
            .given()
            .header("Authorization", "Bearer $accessToken")
            .post("/threads")

        // when & then
        RestAssured
            .given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/threads")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("$", hasSize<Int>(2))
    }

    @Test
    @DisplayName("스레드 조회 성공 - 200 OK")
    fun getThreadSuccess() {
        // given - 스레드 생성
        val createResponse = RestAssured
            .given()
            .header("Authorization", "Bearer $accessToken")
            .post("/threads")
            .then()
            .extract()

        val threadId = createResponse.path<Int>("threadId")

        // when & then
        RestAssured
            .given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/threads/$threadId")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("threadId", equalTo(threadId))
            .body("userId", equalTo(userId.toInt()))
    }

    @Test
    @DisplayName("스레드 조회 실패 - 다른 사용자의 스레드, 500 Internal Server Error")
    fun getThreadFailAccessDenied() {
        // given - 다른 사용자 생성
        val otherUser = UserEntity(
            email = "other@example.com",
            password = passwordEncoder.encode("password123"),
            name = "Other User",
        )
        val savedOtherUser = userRepository.save(otherUser)
        val otherToken = jwtTokenProvider.generateAccessToken(savedOtherUser.id)

        // 현재 사용자로 스레드 생성
        val createResponse = RestAssured
            .given()
            .header("Authorization", "Bearer $accessToken")
            .post("/threads")
            .then()
            .extract()

        val threadId = createResponse.path<Int>("threadId")

        // when & then - 다른 사용자로 조회 시도
        RestAssured
            .given()
            .header("Authorization", "Bearer $otherToken")
            .`when`()
            .get("/threads/$threadId")
            .then()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
    }

    @Test
    @DisplayName("스레드 생성 실패 - 토큰 없음, 401 Unauthorized")
    fun createThreadFailNoToken() {
        // when & then
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .`when`()
            .post("/threads")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
    }
}
