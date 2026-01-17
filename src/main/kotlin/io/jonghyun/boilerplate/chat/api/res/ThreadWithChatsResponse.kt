package io.jonghyun.boilerplate.chat.api.res

import java.time.LocalDateTime

data class ThreadWithChatsResponse(
    val threadId: Long,
    val userId: Long,
    val lastChatAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val chats: List<ChatSummary>,
)

data class ChatSummary(
    val chatId: Long,
    val question: String,
    val answer: String,
    val model: String,
    val createdAt: LocalDateTime,
)
