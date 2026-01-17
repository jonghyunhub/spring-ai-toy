package io.jonghyun.boilerplate.thread.application

import java.time.LocalDateTime

data class ThreadResponse(
    val threadId: Long,
    val userId: Long,
    val lastChatAt: LocalDateTime,
    val createdAt: LocalDateTime,
)
