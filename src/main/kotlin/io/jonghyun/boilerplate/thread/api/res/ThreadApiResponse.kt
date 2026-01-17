package io.jonghyun.boilerplate.thread.api.res

import java.time.LocalDateTime

data class ThreadApiResponse(
    val threadId: Long,
    val userId: Long,
    val lastChatAt: LocalDateTime,
    val createdAt: LocalDateTime,
)
