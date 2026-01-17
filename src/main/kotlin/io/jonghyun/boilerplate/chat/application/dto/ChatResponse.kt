package io.jonghyun.boilerplate.chat.application.dto

import java.time.LocalDateTime

data class ChatResponse(
    val chatId: Long,
    val threadId: Long,
    val question: String,
    val answer: String,
    val createdAt: LocalDateTime,
)