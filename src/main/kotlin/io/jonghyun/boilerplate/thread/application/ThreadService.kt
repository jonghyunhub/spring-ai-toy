package io.jonghyun.boilerplate.thread.application

import io.jonghyun.boilerplate.chat.repository.ChatRepository
import io.jonghyun.boilerplate.thread.domain.ThreadEntity
import io.jonghyun.boilerplate.thread.repository.ThreadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ThreadService(
    private val threadRepository: ThreadRepository,
    private val chatRepository: ChatRepository,
) {

    @Transactional
    fun createThread(userId: Long): ThreadResponse {
        val thread = ThreadEntity(userId = userId)
        val savedThread = threadRepository.save(thread)

        return ThreadResponse(
            threadId = savedThread.id,
            userId = savedThread.userId,
            lastChatAt = savedThread.lastChatAt,
            createdAt = savedThread.createdAt,
        )
    }

    @Transactional(readOnly = true)
    fun getThreads(userId: Long): List<ThreadResponse> {
        val threads = threadRepository.findAllByUserIdOrderByLastChatAtDesc(userId)

        return threads.map { thread ->
            ThreadResponse(
                threadId = thread.id,
                userId = thread.userId,
                lastChatAt = thread.lastChatAt,
                createdAt = thread.createdAt,
            )
        }
    }

    @Transactional(readOnly = true)
    fun getThread(userId: Long, threadId: Long): ThreadResponse {
        val thread = threadRepository.findByIdAndUserId(threadId, userId)
            ?: throw IllegalArgumentException("Thread not found or access denied")

        return ThreadResponse(
            threadId = thread.id,
            userId = thread.userId,
            lastChatAt = thread.lastChatAt,
            createdAt = thread.createdAt,
        )
    }

    @Transactional
    fun deleteThread(userId: Long, threadId: Long) {
        // 1. 스레드 존재 여부 & 권한 검증 (자신이 생성한 스레드만 삭제 가능)
        val thread = threadRepository.findByIdAndUserId(threadId, userId)
            ?: throw IllegalArgumentException("Thread not found or access denied")

        // 2. 연관된 모든 채팅 삭제
        chatRepository.deleteByThreadId(threadId)

        // 3. 스레드 삭제
        threadRepository.delete(thread)
    }
}
