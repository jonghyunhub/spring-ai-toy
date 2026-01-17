package io.jonghyun.boilerplate.chat.repository

import io.jonghyun.boilerplate.chat.domain.ChatEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRepository : JpaRepository<ChatEntity, Long> {
    fun findByThreadIdOrderByCreatedAtAsc(threadId: Long): List<ChatEntity>
    fun findTop10ByThreadIdOrderByCreatedAtDesc(threadId: Long): List<ChatEntity>
    fun findByThreadIdInOrderByCreatedAtAsc(threadIds: List<Long>): List<ChatEntity>
    fun deleteByThreadId(threadId: Long)
}
