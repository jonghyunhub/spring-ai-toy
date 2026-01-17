package io.jonghyun.boilerplate.chat.application

import io.jonghyun.boilerplate.chat.application.dto.ChatResponse
import io.jonghyun.boilerplate.chat.domain.ChatEntity
import io.jonghyun.boilerplate.chat.repository.ChatRepository
import io.jonghyun.boilerplate.client.ai.AiClient
import io.jonghyun.boilerplate.client.ai.ChatMessage
import io.jonghyun.boilerplate.client.ai.MessageRole
import io.jonghyun.boilerplate.thread.repository.ThreadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChatService(
    private val threadRepository: ThreadRepository,
    private val chatRepository: ChatRepository,
    private val aiClient: AiClient,
) {

    @Transactional
    fun createChat(
        userId: Long,
        threadId: Long,
        question: String,
        model: String?,
    ): ChatResponse {
        // 1. 스레드 존재 여부 & 권한 검증
        val thread = threadRepository.findByIdAndUserId(threadId, userId)
            ?: throw IllegalArgumentException("Thread not found or access denied")

        // 2. 이전 채팅 조회 (컨텍스트 생성)
        val messages = buildChatMessages(threadId, question)

        // 3. AI API 호출
        val answer = aiClient.chat(messages, model)

        // 4. Chat 저장
        val chat = ChatEntity(
            threadId = threadId,
            question = question,
            answer = answer,
        )
        val savedChat = chatRepository.save(chat)

        // 5. Thread의 lastChatAt 업데이트
        thread.updateLastChatAt()
        threadRepository.save(thread)

        return ChatResponse(
            chatId = savedChat.id,
            threadId = savedChat.threadId,
            question = savedChat.question,
            answer = savedChat.answer,
            createdAt = savedChat.createdAt,
        )
    }

    @Transactional
    fun createChatStream(
        userId: Long,
        threadId: Long,
        question: String,
        model: String?,
        onChunk: (String) -> Unit,
    ): Long {
        // 1. 스레드 존재 여부 & 권한 검증
        val thread = threadRepository.findByIdAndUserId(threadId, userId)
            ?: throw IllegalArgumentException("Thread not found or access denied")

        // 2. 이전 채팅 조회 (컨텍스트 생성)
        val messages = buildChatMessages(threadId, question)

        // 3. AI API 스트리밍 호출
        val fullAnswer = StringBuilder()
        aiClient.chatStream(messages, model) { chunk ->
            fullAnswer.append(chunk)
            onChunk(chunk)
        }

        // 4. Chat 저장
        val chat = ChatEntity(
            threadId = threadId,
            question = question,
            answer = fullAnswer.toString(),
        )
        val savedChat = chatRepository.save(chat)

        // 5. Thread의 lastChatAt 업데이트
        thread.updateLastChatAt()
        threadRepository.save(thread)

        return savedChat.id
    }

    private fun buildChatMessages(threadId: Long, newQuestion: String): List<ChatMessage> {
        // 최근 10개의 채팅만 컨텍스트로 전달 (토큰 제한 고려)
        val recentChats = chatRepository
            .findTop10ByThreadIdOrderByCreatedAtDesc(threadId)
            .reversed()

        val contextMessages = recentChats.flatMap { chat ->
            listOf(
                ChatMessage(role = MessageRole.USER, content = chat.question),
                ChatMessage(role = MessageRole.ASSISTANT, content = chat.answer),
            )
        }

        return contextMessages + ChatMessage(role = MessageRole.USER, content = newQuestion)
    }
}
