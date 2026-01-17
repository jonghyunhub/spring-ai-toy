package io.jonghyun.boilerplate.thread.application

import io.jonghyun.boilerplate.chat.repository.ChatRepository
import io.jonghyun.boilerplate.thread.domain.ThreadEntity
import io.jonghyun.boilerplate.thread.repository.ThreadRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("ThreadService 단위 테스트")
class ThreadServiceTest {

    private lateinit var threadService: ThreadService
    private lateinit var threadRepository: ThreadRepository
    private lateinit var chatRepository: ChatRepository

    @BeforeEach
    fun setUp() {
        threadRepository = mockk()
        chatRepository = mockk()
        threadService = ThreadService(threadRepository, chatRepository)
    }

    @Test
    @DisplayName("스레드 생성 성공")
    fun createThreadSuccess() {
        // given
        val userId = 1L
        val thread = mockk<ThreadEntity>()
        every { thread.id } returns 100L
        every { thread.userId } returns userId
        every { thread.lastChatAt } returns java.time.LocalDateTime.now()
        every { thread.createdAt } returns java.time.LocalDateTime.now()
        every { threadRepository.save(any()) } returns thread

        // when
        val result = threadService.createThread(userId)

        // then
        assertNotNull(result)
        assertEquals(100L, result.threadId)
        assertEquals(userId, result.userId)
        verify(exactly = 1) { threadRepository.save(any()) }
    }

    @Test
    @DisplayName("사용자의 스레드 목록 조회 성공")
    fun getThreadsSuccess() {
        // given
        val userId = 1L
        val thread1 = mockk<ThreadEntity>()
        val thread2 = mockk<ThreadEntity>()

        every { thread1.id } returns 1L
        every { thread1.userId } returns userId
        every { thread1.lastChatAt } returns java.time.LocalDateTime.now()
        every { thread1.createdAt } returns java.time.LocalDateTime.now()

        every { thread2.id } returns 2L
        every { thread2.userId } returns userId
        every { thread2.lastChatAt } returns java.time.LocalDateTime.now().minusHours(1)
        every { thread2.createdAt } returns java.time.LocalDateTime.now().minusHours(1)

        every { threadRepository.findAllByUserIdOrderByLastChatAtDesc(userId) } returns listOf(thread1, thread2)

        // when
        val result = threadService.getThreads(userId)

        // then
        assertEquals(2, result.size)
        assertEquals(1L, result[0].threadId)
        assertEquals(2L, result[1].threadId)
        verify(exactly = 1) { threadRepository.findAllByUserIdOrderByLastChatAtDesc(userId) }
    }

    @Test
    @DisplayName("스레드 조회 성공")
    fun getThreadSuccess() {
        // given
        val userId = 1L
        val threadId = 100L
        val thread = mockk<ThreadEntity>()

        every { thread.id } returns threadId
        every { thread.userId } returns userId
        every { thread.lastChatAt } returns java.time.LocalDateTime.now()
        every { thread.createdAt } returns java.time.LocalDateTime.now()
        every { threadRepository.findByIdAndUserId(threadId, userId) } returns thread

        // when
        val result = threadService.getThread(userId, threadId)

        // then
        assertNotNull(result)
        assertEquals(threadId, result.threadId)
        assertEquals(userId, result.userId)
        verify(exactly = 1) { threadRepository.findByIdAndUserId(threadId, userId) }
    }

    @Test
    @DisplayName("스레드 조회 실패 - 존재하지 않거나 권한 없음")
    fun getThreadFailNotFoundOrAccessDenied() {
        // given
        val userId = 1L
        val threadId = 999L
        every { threadRepository.findByIdAndUserId(threadId, userId) } returns null

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            threadService.getThread(userId, threadId)
        }
        assertEquals("Thread not found or access denied", exception.message)
        verify(exactly = 1) { threadRepository.findByIdAndUserId(threadId, userId) }
    }
}
