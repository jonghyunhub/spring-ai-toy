package io.jonghyun.boilerplate.thread.repository

import io.jonghyun.boilerplate.thread.domain.ThreadEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ThreadRepository : JpaRepository<ThreadEntity, Long> {
    fun findByIdAndUserId(id: Long, userId: Long): ThreadEntity?
    fun findAllByUserIdOrderByLastChatAtDesc(userId: Long): List<ThreadEntity>
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<ThreadEntity>
    fun findAllBy(pageable: Pageable): Page<ThreadEntity>
}
