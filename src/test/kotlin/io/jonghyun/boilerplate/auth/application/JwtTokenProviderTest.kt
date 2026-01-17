package io.jonghyun.boilerplate.auth.application

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Date

@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider
    private val testSecretKey = "test-secret-key-for-unit-and-integration-tests-must-be-at-least-256-bits-long"
    private val accessTokenValidity = 3600000L // 1 hour

    @BeforeEach
    fun setUp() {
        jwtTokenProvider = JwtTokenProvider(testSecretKey, accessTokenValidity)
    }

    @Test
    @DisplayName("액세스 토큰 생성 성공")
    fun generateAccessTokenSuccess() {
        // given
        val userId = 1L

        // when
        val token = jwtTokenProvider.generateAccessToken(userId)

        // then
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    @Test
    @DisplayName("유효한 토큰 검증 성공")
    fun validateTokenSuccess() {
        // given
        val userId = 1L
        val token = jwtTokenProvider.generateAccessToken(userId)

        // when
        val isValid = jwtTokenProvider.validateToken(token)

        // then
        assertTrue(isValid)
    }

    @Test
    @DisplayName("토큰에서 사용자 ID 추출 성공")
    fun getUserIdSuccess() {
        // given
        val userId = 123L
        val token = jwtTokenProvider.generateAccessToken(userId)

        // when
        val extractedUserId = jwtTokenProvider.getUserId(token)

        // then
        assertEquals(userId, extractedUserId)
    }

    @Test
    @DisplayName("만료된 토큰 검증 실패")
    fun validateExpiredTokenFail() {
        // given
        val userId = 1L
        val expiredTokenValidity = -1000L // 이미 만료된 토큰
        val expiredTokenProvider = JwtTokenProvider(testSecretKey, expiredTokenValidity)
        val token = expiredTokenProvider.generateAccessToken(userId)

        // when
        val isValid = jwtTokenProvider.validateToken(token)

        // then
        assertFalse(isValid)
    }

    @Test
    @DisplayName("잘못된 서명의 토큰 검증 실패")
    fun validateInvalidSignatureTokenFail() {
        // given
        val userId = 1L
        val differentSecretKey = "different-secret-key-for-testing-invalid-signature-must-be-at-least-256-bits"
        val differentTokenProvider = JwtTokenProvider(differentSecretKey, accessTokenValidity)
        val token = differentTokenProvider.generateAccessToken(userId)

        // when
        val isValid = jwtTokenProvider.validateToken(token)

        // then
        assertFalse(isValid)
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 검증 실패")
    fun validateMalformedTokenFail() {
        // given
        val invalidToken = "invalid.token.format"

        // when
        val isValid = jwtTokenProvider.validateToken(invalidToken)

        // then
        assertFalse(isValid)
    }

    @Test
    @DisplayName("빈 토큰 검증 실패")
    fun validateEmptyTokenFail() {
        // given
        val emptyToken = ""

        // when
        val isValid = jwtTokenProvider.validateToken(emptyToken)

        // then
        assertFalse(isValid)
    }

    @Test
    @DisplayName("토큰이 올바른 만료 시간을 가지고 있는지 확인")
    fun verifyTokenExpiration() {
        // given
        val userId = 1L
        val token = jwtTokenProvider.generateAccessToken(userId)

        // when
        val key = Keys.hmacShaKeyFor(testSecretKey.toByteArray())
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        val now = Date()
        val expectedExpiration = Date(now.time + accessTokenValidity)

        // then
        assertNotNull(claims.expiration)
        assertTrue(claims.expiration.after(now))
        // 만료 시간이 1초 이내의 오차로 예상 시간과 일치하는지 확인
        assertTrue(Math.abs(claims.expiration.time - expectedExpiration.time) < 1000)
    }
}
