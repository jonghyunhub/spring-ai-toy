package io.jonghyun.boilerplate.thread.domain

import io.jonghyun.boilerplate.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "threads")
class ThreadEntity(
    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    var lastChatAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity() {

    fun updateLastChatAt() {
        this.lastChatAt = LocalDateTime.now()
    }
}
