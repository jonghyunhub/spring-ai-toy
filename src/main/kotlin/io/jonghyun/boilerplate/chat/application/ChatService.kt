package io.jonghyun.boilerplate.chat.application

import io.jonghyun.boilerplate.chat.api.res.ChatListResponse
import io.jonghyun.boilerplate.chat.api.res.ChatSummary
import io.jonghyun.boilerplate.chat.api.res.ThreadWithChatsResponse
import io.jonghyun.boilerplate.chat.application.dto.ChatResponse
import io.jonghyun.boilerplate.chat.domain.ChatEntity
import io.jonghyun.boilerplate.chat.repository.ChatRepository
import io.jonghyun.boilerplate.client.ai.AiClient
import io.jonghyun.boilerplate.client.ai.ChatMessage
import io.jonghyun.boilerplate.client.ai.MessageRole
import io.jonghyun.boilerplate.thread.repository.ThreadRepository
import io.jonghyun.boilerplate.user.domain.UserRole
import io.jonghyun.boilerplate.user.repository.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChatService(
    private val threadRepository: ThreadRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
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
            model = model ?: "default",
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
            model = model ?: "default",
        )
        val savedChat = chatRepository.save(chat)

        // 5. Thread의 lastChatAt 업데이트
        thread.updateLastChatAt()
        threadRepository.save(thread)

        return savedChat.id
    }

    @Transactional(readOnly = true)
    fun getChatList(userId: Long, pageable: Pageable): ChatListResponse {
        // 1. 사용자 조회 및 권한 확인
        val user = userRepository.findById(userId).orElseThrow {
            throw IllegalArgumentException("User not found")
        }

        // 2. 권한에 따라 스레드 조회
        val threadPage = if (user.userRole == UserRole.ADMIN) {
            // 관리자는 모든 스레드 조회
            threadRepository.findAllBy(pageable)
        } else {
            // 일반 유저는 자신의 스레드만 조회
            threadRepository.findAllByUserId(userId, pageable)
        }

        // 3. 조회된 스레드가 없으면 빈 응답 반환
        if (threadPage.content.isEmpty()) {
            return ChatListResponse(
                threads = emptyList(),
                totalElements = threadPage.totalElements,
                totalPages = threadPage.totalPages,
                currentPage = threadPage.number,
                pageSize = threadPage.size,
            )
        }

        // 4. 스레드 ID 목록 추출
        val threadIds = threadPage.content.map { it.id }

        // 5. 해당 스레드들의 모든 채팅 조회 (N+1 방지)
        val chats = chatRepository.findByThreadIdInOrderByCreatedAtAsc(threadIds)

        // 6. 스레드별로 채팅 그룹화
        val chatsByThreadId = chats.groupBy { it.threadId }

        // 7. 응답 생성
        val threadWithChats = threadPage.content.map { thread ->
            val threadChats = chatsByThreadId[thread.id] ?: emptyList()
            ThreadWithChatsResponse(
                threadId = thread.id,
                userId = thread.userId,
                lastChatAt = thread.lastChatAt,
                createdAt = thread.createdAt,
                chats = threadChats.map { chat ->
                    ChatSummary(
                        chatId = chat.id,
                        question = chat.question,
                        answer = chat.answer,
                        model = chat.model,
                        createdAt = chat.createdAt,
                    )
                },
            )
        }

        return ChatListResponse(
            threads = threadWithChats,
            totalElements = threadPage.totalElements,
            totalPages = threadPage.totalPages,
            currentPage = threadPage.number,
            pageSize = threadPage.size,
        )
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
