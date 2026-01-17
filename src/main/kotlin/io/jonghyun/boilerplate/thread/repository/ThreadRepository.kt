package io.jonghyun.boilerplate.thread.repository

import io.jonghyun.boilerplate.thread.domain.ThreadEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ThreadRepository : JpaRepository<ThreadEntity, Long> {
    fun findByIdAndUserId(id: Long, userId: Long): ThreadEntity?
    fun findAllByUserIdOrderByLastChatAtDesc(userId: Long): List<ThreadEntity>
}
