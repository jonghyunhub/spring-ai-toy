package io.jonghyun.boilerplate.chat.api.res

import java.time.LocalDateTime

data class ChatApiResponse(
    val chatId: Long,
    val threadId: Long,
    val question: String,
    val answer: String,
    val createdAt: LocalDateTime,
)
