package io.jonghyun.boilerplate.auth.application

import io.jonghyun.boilerplate.user.domain.UserEntity
import io.jonghyun.boilerplate.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder

@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    private lateinit var authService: AuthService
    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        passwordEncoder = mockk()
        jwtTokenProvider = mockk()
        authService = AuthService(userRepository, passwordEncoder, jwtTokenProvider)
    }

    @Test
    @DisplayName("회원가입 성공")
    fun signUpSuccess() {
        // given
        val email = "test@example.com"
        val password = "password123"
        val name = "Test User"
        val encodedPassword = "encodedPassword123"

        every { userRepository.existsByEmail(email) } returns false
        every { passwordEncoder.encode(password) } returns encodedPassword
        every { userRepository.save(any()) } answers {
            val user = firstArg<UserEntity>()
            user
        }

        // when
        val result = authService.signUp(email, password, name)

        // then
        assertNotNull(result)
        assertEquals(email, result.email)
        assertEquals(encodedPassword, result.password)
        assertEquals(name, result.name)

        verify(exactly = 1) { userRepository.existsByEmail(email) }
        verify(exactly = 1) { passwordEncoder.encode(password) }
        verify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    fun signUpFailDuplicateEmail() {
        // given
        val email = "duplicate@example.com"
        val password = "password123"
        val name = "Test User"

        every { userRepository.existsByEmail(email) } returns true

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            authService.signUp(email, password, name)
        }

        assertEquals("Email already exists: $email", exception.message)
        verify(exactly = 1) { userRepository.existsByEmail(email) }
        verify(exactly = 0) { passwordEncoder.encode(any()) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    @DisplayName("로그인 성공")
    fun loginSuccess() {
        // given
        val email = "test@example.com"
        val password = "password123"
        val encodedPassword = "encodedPassword123"
        val userId = 1L
        val accessToken = "generated-access-token"

        val user = mockk<UserEntity>()
        every { user.id } returns userId
        every { user.password } returns encodedPassword

        every { userRepository.findByEmail(email) } returns user
        every { passwordEncoder.matches(password, encodedPassword) } returns true
        every { jwtTokenProvider.generateAccessToken(userId) } returns accessToken

        // when
        val result = authService.login(email, password)

        // then
        assertEquals(accessToken, result.first)
        assertEquals(userId, result.second)

        verify(exactly = 1) { userRepository.findByEmail(email) }
        verify(exactly = 1) { passwordEncoder.matches(password, encodedPassword) }
        verify(exactly = 1) { jwtTokenProvider.generateAccessToken(userId) }
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 이메일")
    fun loginFailInvalidEmail() {
        // given
        val email = "invalid@example.com"
        val password = "password123"

        every { userRepository.findByEmail(email) } returns null

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            authService.login(email, password)
        }

        assertEquals("Invalid email or password", exception.message)
        verify(exactly = 1) { userRepository.findByEmail(email) }
        verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    fun loginFailInvalidPassword() {
        // given
        val email = "test@example.com"
        val password = "wrongPassword"
        val encodedPassword = "encodedPassword123"

        val user = mockk<UserEntity>()
        every { user.password } returns encodedPassword

        every { userRepository.findByEmail(email) } returns user
        every { passwordEncoder.matches(password, encodedPassword) } returns false

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            authService.login(email, password)
        }

        assertEquals("Invalid email or password", exception.message)
        verify(exactly = 1) { userRepository.findByEmail(email) }
        verify(exactly = 1) { passwordEncoder.matches(password, encodedPassword) }
        verify(exactly = 0) { jwtTokenProvider.generateAccessToken(any()) }
    }
}
