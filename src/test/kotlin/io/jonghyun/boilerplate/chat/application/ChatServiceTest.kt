package io.jonghyun.boilerplate.chat.application

import io.jonghyun.boilerplate.chat.domain.ChatEntity
import io.jonghyun.boilerplate.chat.repository.ChatRepository
import io.jonghyun.boilerplate.client.ai.AiClient
import io.jonghyun.boilerplate.client.ai.ChatMessage
import io.jonghyun.boilerplate.thread.domain.ThreadEntity
import io.jonghyun.boilerplate.thread.repository.ThreadRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@DisplayName("ChatService 단위 테스트")
class ChatServiceTest {

    private lateinit var chatService: ChatService
    private lateinit var threadRepository: ThreadRepository
    private lateinit var chatRepository: ChatRepository
    private lateinit var aiClient: AiClient

    @BeforeEach
    fun setUp() {
        threadRepository = mockk()
        chatRepository = mockk()
        aiClient = mockk()
        chatService = ChatService(threadRepository, chatRepository, aiClient)
    }

    @Test
    @DisplayName("채팅 생성 성공 - 컨텍스트 없음")
    fun createChatSuccessWithoutContext() {
        // given
        val userId = 1L
        val threadId = 100L
        val question = "안녕하세요"
        val answer = "안녕하세요! 무엇을 도와드릴까요?"

        val thread = mockk<ThreadEntity>()
        every { thread.updateLastChatAt() } returns Unit
        every { threadRepository.findByIdAndUserId(threadId, userId) } returns thread
        every { threadRepository.save(thread) } returns thread

        every { chatRepository.findTop10ByThreadIdOrderByCreatedAtDesc(threadId) } returns emptyList()

        val messagesSlot = slot<List<ChatMessage>>()
        every { aiClient.chat(capture(messagesSlot), any()) } returns answer

        val savedChat = mockk<ChatEntity>()
        every { savedChat.id } returns 1L
        every { savedChat.threadId } returns threadId
        every { savedChat.question } returns question
        every { savedChat.answer } returns answer
        every { savedChat.createdAt } returns LocalDateTime.now()
        every { chatRepository.save(any()) } returns savedChat

        // when
        val result = chatService.createChat(userId, threadId, question, null)

        // then
        assertNotNull(result)
        assertEquals(1L, result.chatId)
        assertEquals(question, result.question)
        assertEquals(answer, result.answer)

        // 메시지가 올바르게 구성되었는지 확인
        assertEquals(1, messagesSlot.captured.size)
        assertEquals(question, messagesSlot.captured[0].content)

        verify(exactly = 1) { threadRepository.findByIdAndUserId(threadId, userId) }
        verify(exactly = 1) { aiClient.chat(any(), null) }
        verify(exactly = 1) { chatRepository.save(any()) }
        verify(exactly = 1) { thread.updateLastChatAt() }
    }

    @Test
    @DisplayName("채팅 생성 성공 - 이전 컨텍스트 포함")
    fun createChatSuccessWithContext() {
        // given
        val userId = 1L
        val threadId = 100L
        val newQuestion = "내 이름이 뭐라고?"

        val thread = mockk<ThreadEntity>()
        every { thread.updateLastChatAt() } returns Unit
        every { threadRepository.findByIdAndUserId(threadId, userId) } returns thread
        every { threadRepository.save(thread) } returns thread

        // 이전 채팅 기록
        val previousChat = mockk<ChatEntity>()
        every { previousChat.question } returns "내 이름은 철수야"
        every { previousChat.answer } returns "안녕하세요, 철수님!"
        every { chatRepository.findTop10ByThreadIdOrderByCreatedAtDesc(threadId) } returns listOf(previousChat)

        val messagesSlot = slot<List<ChatMessage>>()
        val answer = "철수님이십니다!"
        every { aiClient.chat(capture(messagesSlot), any()) } returns answer

        val savedChat = mockk<ChatEntity>()
        every { savedChat.id } returns 2L
        every { savedChat.threadId } returns threadId
        every { savedChat.question } returns newQuestion
        every { savedChat.answer } returns answer
        every { savedChat.createdAt } returns LocalDateTime.now()
        every { chatRepository.save(any()) } returns savedChat

        // when
        val result = chatService.createChat(userId, threadId, newQuestion, null)

        // then
        assertNotNull(result)
        assertEquals(answer, result.answer)

        // 컨텍스트가 포함되었는지 확인 (이전 질문 + 답변 + 새 질문 = 3개 메시지)
        assertEquals(3, messagesSlot.captured.size)

        verify(exactly = 1) { chatRepository.findTop10ByThreadIdOrderByCreatedAtDesc(threadId) }
    }

    @Test
    @DisplayName("채팅 생성 실패 - 스레드 권한 없음")
    fun createChatFailAccessDenied() {
        // given
        val userId = 1L
        val threadId = 999L
        val question = "테스트 질문"

        every { threadRepository.findByIdAndUserId(threadId, userId) } returns null

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            chatService.createChat(userId, threadId, question, null)
        }
        assertEquals("Thread not found or access denied", exception.message)

        verify(exactly = 1) { threadRepository.findByIdAndUserId(threadId, userId) }
        verify(exactly = 0) { aiClient.chat(any(), any()) }
        verify(exactly = 0) { chatRepository.save(any()) }
    }

    @Test
    @DisplayName("스트리밍 채팅 생성 성공")
    fun createChatStreamSuccess() {
        // given
        val userId = 1L
        val threadId = 100L
        val question = "긴 답변 부탁해"

        val thread = mockk<ThreadEntity>()
        every { thread.updateLastChatAt() } returns Unit
        every { threadRepository.findByIdAndUserId(threadId, userId) } returns thread
        every { threadRepository.save(thread) } returns thread

        every { chatRepository.findTop10ByThreadIdOrderByCreatedAtDesc(threadId) } returns emptyList()

        val chunks = mutableListOf<String>()
        every {
            aiClient.chatStream(any(), any(), any())
        } answers {
            val onChunk = thirdArg<(String) -> Unit>()
            onChunk("안녕")
            onChunk("하세요")
            onChunk("!")
        }

        val savedChat = mockk<ChatEntity>()
        every { savedChat.id } returns 1L
        every { chatRepository.save(any()) } returns savedChat

        // when
        val chatId = chatService.createChatStream(userId, threadId, question, null) { chunk ->
            chunks.add(chunk)
        }

        // then
        assertEquals(1L, chatId)
        assertEquals(3, chunks.size)
        assertEquals("안녕", chunks[0])
        assertEquals("하세요", chunks[1])
        assertEquals("!", chunks[2])

        verify(exactly = 1) { aiClient.chatStream(any(), any(), any()) }
        verify(exactly = 1) { chatRepository.save(any()) }
    }
}
