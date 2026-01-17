package io.jonghyun.boilerplate.client.ai.dto

data class ClaudeRequest(
    val model: String,
    val max_tokens: Int = 1024, // 응답 최대 토큰 수
    val messages: List<ClaudeMessageDto>,
    val system: String? = null, // 시스템 프롬프트가 필요하면 사용
    val stream: Boolean = false
)

data class ClaudeMessageDto(
    val role: String,
    val content: String
)