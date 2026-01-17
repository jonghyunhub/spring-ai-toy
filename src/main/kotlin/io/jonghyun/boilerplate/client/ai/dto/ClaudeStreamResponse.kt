package io.jonghyun.boilerplate.client.ai.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true) // 모르는 필드는 무시 (필수)
data class ClaudeStreamResponse(
    val type: String?,
    val delta: ClaudeStreamDelta?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ClaudeStreamDelta(
    val type: String?,
    val text: String?
)