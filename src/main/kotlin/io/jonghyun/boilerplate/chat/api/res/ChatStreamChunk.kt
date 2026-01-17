package io.jonghyun.boilerplate.chat.api.res

data class ChatStreamChunk(
    val content: String? = null,
    val chatId: Long? = null,
    val isDone: Boolean = false,
)
