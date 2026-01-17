package io.jonghyun.boilerplate.client.ai.dto

data class ClaudeResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeContentDto>
)

data class ClaudeContentDto(
    val type: String,
    val text: String
)