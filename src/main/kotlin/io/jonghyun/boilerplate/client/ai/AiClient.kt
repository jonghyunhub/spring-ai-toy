package io.jonghyun.boilerplate.client.ai

interface AiClient {
    /**
     * AI 모델에게 질문하고 답변을 받습니다.
     *
     * @param messages 대화 컨텍스트 (이전 채팅 기록 + 현재 질문)
     * @param model 사용할 AI 모델 (null이면 기본 모델 사용)
     * @return AI의 답변
     */
    fun chat(messages: List<ChatMessage>, model: String?): String

    /**
     * AI 모델에게 질문하고 스트리밍 방식으로 답변을 받습니다.
     *
     * @param messages 대화 컨텍스트
     * @param model 사용할 AI 모델
     * @param onChunk 청크가 도착할 때마다 호출되는 콜백
     */
    fun chatStream(messages: List<ChatMessage>, model: String?, onChunk: (String) -> Unit)
}

data class ChatMessage(
    val role: MessageRole,
    val content: String,
)

enum class MessageRole {
    USER,
    ASSISTANT,
}
