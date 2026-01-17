package io.jonghyun.boilerplate.client.ai

import org.springframework.stereotype.Component

@Component("mockAiClient")
class MockAiClient : AiClient {

    override fun chat(messages: List<ChatMessage>, model: String?): String {
        val lastQuestion = messages.lastOrNull { it.role == MessageRole.USER }?.content
            ?: return "질문을 입력해주세요."

        // Mock 응답 생성
        return generateMockResponse(lastQuestion, model)
    }

    override fun chatStream(messages: List<ChatMessage>, model: String?, onChunk: (String) -> Unit) {
        val lastQuestion = messages.lastOrNull { it.role == MessageRole.USER }?.content
            ?: return

        val response = generateMockResponse(lastQuestion, model)

        // 응답을 청크로 나눠서 전송 (스트리밍 시뮬레이션)
        val chunks = response.chunked(5) // 5글자씩 나눔
        chunks.forEach { chunk ->
            Thread.sleep(50) // 스트리밍 효과를 위한 딜레이
            onChunk(chunk)
        }
    }

    private fun generateMockResponse(question: String, model: String?): String {
        val modelInfo = model?.let { " (모델: $it)" } ?: ""
        return """
            안녕하세요! Mock AI 입니다$modelInfo.

            질문: "$question"

            이것은 실제 AI 응답이 아닌 Mock 응답입니다.
            실제 AI Client 구현체를 연결하면 실제 AI의 답변을 받을 수 있습니다.

            현재 시각: ${java.time.LocalDateTime.now()}
        """.trimIndent()
    }
}
