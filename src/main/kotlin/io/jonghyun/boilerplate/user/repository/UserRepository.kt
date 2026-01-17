package io.jonghyun.boilerplate.user.repository

import io.jonghyun.boilerplate.user.domain.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByEmail(email: String): UserEntity?
    fun existsByEmail(email: String): Boolean

    @Query(value = "SELECT pg_advisory_lock(:lockId)", nativeQuery = true)
    fun getAdvisoryLock(@Param("lockId") lockId: Long)

    @Query(value = "SELECT pg_advisory_unlock(:lockId)", nativeQuery = true)
    fun releaseAdvisoryLock(@Param("lockId") lockId: Long)

}