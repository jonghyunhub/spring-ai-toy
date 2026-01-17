package io.jonghyun.boilerplate.auth.application

import io.jonghyun.boilerplate.user.domain.UserEntity
import io.jonghyun.boilerplate.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
) {

    @Transactional(readOnly = true)
    fun login(email: String, password: String): Pair<String, Long> {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Invalid email or password")

        if (!passwordEncoder.matches(password, user.password)) {
            throw IllegalArgumentException("Invalid email or password")
        }

        val accessToken = jwtTokenProvider.generateAccessToken(user.id)
        return Pair(accessToken, user.id)
    }

    @Transactional
    fun signUp(email: String, password: String, name: String): UserEntity {
        userRepository.getAdvisoryLock(email.hashCode().toLong())

        try {
            if (userRepository.existsByEmail(email)) {
                throw IllegalArgumentException("Email already exists: $email")
            }

            val encodedPassword = passwordEncoder.encode(password)
            val userEntity = UserEntity(email, encodedPassword, name)
            return userRepository.save(userEntity)
        } finally {
            userRepository.releaseAdvisoryLock(email.hashCode().toLong())
        }
    }
}
