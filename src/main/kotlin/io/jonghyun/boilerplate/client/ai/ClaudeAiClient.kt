package io.jonghyun.boilerplate.client.ai

import io.jonghyun.boilerplate.client.ai.dto.ClaudeMessageDto
import io.jonghyun.boilerplate.client.ai.dto.ClaudeRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component("claudeAiClient")
class ClaudeAiClient(
    private val claudeFeignClient: ClaudeFeignClient,
    @Value("\${claude.api-key}") private val apiKey: String // application.yml에서 키 관리
) : AiClient {

    // 기본 모델: 가장 저렴한 Haiku 사용
    private val defaultModel = "claude-3-haiku-20240307"

    override fun chat(messages: List<ChatMessage>, model: String?): String {
        // 1. 요청 변환 (ChatMessage -> ClaudeMessageDto)
        val claudeMessages = messages.map { msg ->
            ClaudeMessageDto(
                role = mapRole(msg.role),
                content = msg.content
            )
        }

        // 2. 요청 객체 생성
        val request = ClaudeRequest(
            model = model ?: defaultModel,
            messages = claudeMessages
        )

        // 3. Feign 호출 및 예외 처리 (간단하게 try-catch 없이 작성했으나 실무에선 필요)
        val response = claudeFeignClient.createMessage(
            apiKey = apiKey,
            request = request
        )

        // 4. 응답 텍스트 추출
        return response.content.firstOrNull()?.text ?: "답변을 생성하지 못했습니다."
    }

    /**
     * Feign은 기본적으로 Blocking 방식이므로,
     * 간단한 구현을 위해 전체 응답을 받은 후 한 번에 onChunk를 호출
     * (진정한 스트리밍을 원하신다면 WebClient를 사용해야 합니다.)
     */
    override fun chatStream(messages: List<ChatMessage>, model: String?, onChunk: (String) -> Unit) {
        val fullResponse = chat(messages, model)
        onChunk(fullResponse)
    }

    private fun mapRole(role: MessageRole): String {
        return when (role) {
            MessageRole.USER -> "user"
            MessageRole.ASSISTANT -> "assistant"
        }
    }
}