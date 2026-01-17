package io.jonghyun.boilerplate.client.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * AiClient 인터페이스 계약 테스트
 * 모든 AiClient 구현체는 이 테스트를 통과해야 합니다.
 */
@DisplayName("AiClient 인터페이스 계약 테스트")
abstract class AiClientContractTest {

    /**
     * 테스트할 AiClient 구현체를 반환합니다.
     * 각 구현체별 테스트 클래스에서 오버라이드해야 합니다.
     */
    abstract fun createAiClient(): AiClient

    @Test
    @DisplayName("chat - 질문에 대한 답변을 반환해야 함")
    fun chatShouldReturnAnswer() {
        // given
        val aiClient = createAiClient()
        val messages = listOf(
            ChatMessage(MessageRole.USER, "안녕하세요"),
        )

        // when
        val answer = aiClient.chat(messages, null)

        // then
        assertNotNull(answer)
        assertTrue(answer.isNotEmpty())
    }

    @Test
    @DisplayName("chat - 모델을 지정하여 답변을 받을 수 있어야 함")
    fun chatWithModelShouldReturnAnswer() {
        // given
        val aiClient = createAiClient()
        val messages = listOf(
            ChatMessage(MessageRole.USER, "테스트 질문"),
        )
        val model = "gpt-4"

        // when
        val answer = aiClient.chat(messages, model)

        // then
        assertNotNull(answer)
        assertTrue(answer.isNotEmpty())
    }

    @Test
    @DisplayName("chat - 대화 컨텍스트를 포함한 질문에 답변해야 함")
    fun chatWithContextShouldReturnAnswer() {
        // given
        val aiClient = createAiClient()
        val messages = listOf(
            ChatMessage(MessageRole.USER, "내 이름은 철수야"),
            ChatMessage(MessageRole.ASSISTANT, "안녕하세요, 철수님!"),
            ChatMessage(MessageRole.USER, "내 이름이 뭐라고?"),
        )

        // when
        val answer = aiClient.chat(messages, null)

        // then
        assertNotNull(answer)
        assertTrue(answer.isNotEmpty())
    }

    @Test
    @DisplayName("chatStream - 스트리밍 방식으로 답변을 받을 수 있어야 함")
    fun chatStreamShouldStreamAnswer() {
        // given
        val aiClient = createAiClient()
        val messages = listOf(
            ChatMessage(MessageRole.USER, "긴 답변 부탁해"),
        )
        val chunks = mutableListOf<String>()

        // when
        aiClient.chatStream(messages, null) { chunk ->
            chunks.add(chunk)
        }

        // then
        assertTrue(chunks.isNotEmpty(), "적어도 하나의 청크가 전달되어야 함")
        val fullAnswer = chunks.joinToString("")
        assertFalse(fullAnswer.isEmpty(), "전체 답변이 비어있으면 안 됨")
    }

    @Test
    @DisplayName("chatStream - 빈 메시지 리스트에도 오류 없이 동작해야 함")
    fun chatStreamWithEmptyMessagesShouldNotFail() {
        // given
        val aiClient = createAiClient()
        val messages = emptyList<ChatMessage>()
        var chunkCount = 0

        // when & then (예외가 발생하지 않아야 함)
        aiClient.chatStream(messages, null) {
            chunkCount++
        }
        // 빈 메시지여도 최소한 실행은 되어야 함
        assertTrue(chunkCount >= 0)
    }
}
