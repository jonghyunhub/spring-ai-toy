package io.jonghyun.boilerplate.chat.api.req

data class CreateChatRequest(
    val question: String,
    val model: String? = null,
    val isStreaming: Boolean = false,
)
