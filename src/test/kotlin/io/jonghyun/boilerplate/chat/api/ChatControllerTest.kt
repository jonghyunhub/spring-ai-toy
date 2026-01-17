package io.jonghyun.boilerplate.chat.api

import io.jonghyun.boilerplate.auth.application.JwtTokenProvider
import io.jonghyun.boilerplate.chat.api.req.CreateChatRequest
import io.jonghyun.boilerplate.chat.repository.ChatRepository
import io.jonghyun.boilerplate.thread.repository.ThreadRepository
import io.jonghyun.boilerplate.user.domain.UserEntity
import io.jonghyun.boilerplate.user.repository.UserRepository
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Assertions.assertTrue
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
@DisplayName("ChatController 통합 테스트")
class ChatControllerTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val threadRepository: ThreadRepository,
    private val chatRepository: ChatRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    @LocalServerPort private val port: Int,
) {

    private lateinit var accessToken: String
    private var userId: Long = 0
    private var threadId: Long = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port

        // 테스트 전 데이터베이스 초기화
        chatRepository.deleteAll()
        threadRepository.deleteAll()
        userRepository.deleteAll()

        // 테스트 사용자 생성 및 토큰 발급
        val user = UserEntity(
            email = "chattest@example.com",
            password = passwordEncoder.encode("password123"),
            name = "Chat Test User",
        )
        val savedUser = userRepository.save(user)
        userId = savedUser.id
        accessToken = jwtTokenProvider.generateAccessToken(userId)

        // 테스트용 스레드 생성
        val createThreadResponse = RestAssured
            .given()
            .header("Authorization", "Bearer $accessToken")
            .post("/threads")
            .then()
            .extract()

        threadId = createThreadResponse.path<Int>("threadId").toLong()
    }

    @Test
    @DisplayName("일반 채팅 생성 성공 - 200 OK")
    fun createChatSuccess() {
        // given
        val request = CreateChatRequest(
            question = "안녕하세요",
            model = null,
            isStreaming = false,
        )

        // when & then
        RestAssured
            .given()
            .header("Authorization", "Bearer $accessToken")
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/chats")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("chatId", greaterThan(0))
            .body("threadId", equalTo(threadId.toInt()))
            .body("question", equalTo("안녕하세요"))
            .body("answer", notNullValue())
    }

    @Test
    @DisplayName("모델 지정하여 채팅 생성 성공 - 200 OK")
    fun createChatWithModelSuccess() {
        // given
        val request = CreateChatRequest(
            question = "코틀린이 뭐야?",
            model = "gpt-4",
            isStreaming = false,
        )

        // when & then
        RestAssured
            .given()
            .header("Authorization", "Bearer $accessToken")
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/chats")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("answer", notNullValue())
    }
    

    @Test
    @DisplayName("컨텍스트가 포함된 채팅 생성 성공")
    fun createChatWithContextSuccess() {
        // given - 첫 번째 채팅 생성
        val firstRequest = CreateChatRequest(
            question = "내 이름은 철수야",
            isStreaming = false,
        )

        RestAssured
            .given()
            .header("Authorization", "Bearer $accessToken")
            .contentType(ContentType.JSON)
            .body(firstRequest)
            .post("/chats")

        // 두 번째 채팅 (컨텍스트 포함)
        val secondRequest = CreateChatRequest(
            question = "내 이름이 뭐라고?",
            isStreaming = false,
        )

        // when & then
        RestAssured
            .given()
            .header("Authorization", "Bearer $accessToken")
            .contentType(ContentType.JSON)
            .body(secondRequest)
            .`when`()
            .post("/chats")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("question", equalTo("내 이름이 뭐라고?"))
            .body("answer", notNullValue())

        // 채팅이 2개 저장되었는지 확인
        val chats = chatRepository.findByThreadIdOrderByCreatedAtAsc(threadId)
        assertTrue(chats.size == 2)
    }

    @Test
    @DisplayName("스트리밍 채팅 생성 성공 - SSE 응답")
    fun createChatStreamSuccess() {
        // given
        val request = CreateChatRequest(
            question = "긴 답변 부탁해",
            isStreaming = true,
        )

        // when
        val response = RestAssured
            .given()
            .header("Authorization", "Bearer $accessToken")
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/chats/stream")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .asString()

        // then
        assertTrue(response.isNotEmpty(), "SSE 응답이 비어있으면 안 됨")
        assertTrue(response.contains("data:"), "SSE 형식이어야 함")
    }

    @Test
    @DisplayName("채팅 생성 실패 - 존재하지 않는 스레드, 500 Internal Server Error")
    fun createChatFailThreadNotFound() {
        // given
        val nonExistentThreadId = 99999L
        val request = CreateChatRequest(
            question = "테스트 질문",
            isStreaming = false,
        )

        // when & then
        RestAssured
            .given()
            .header("Authorization", "Bearer $accessToken")
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/threads/$nonExistentThreadId/chats")
            .then()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
    }

    @Test
    @DisplayName("채팅 생성 실패 - 토큰 없음, 401 Unauthorized")
    fun createChatFailNoToken() {
        // given
        val request = CreateChatRequest(
            question = "테스트 질문",
            isStreaming = false,
        )

        // when & then
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/chats")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
    }
}
