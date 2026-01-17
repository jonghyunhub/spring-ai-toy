package io.jonghyun.boilerplate.thread.api

import io.jonghyun.boilerplate.support.auth.CurrentUser
import io.jonghyun.boilerplate.thread.api.res.ThreadApiResponse
import io.jonghyun.boilerplate.thread.application.ThreadService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/threads")
class ThreadController(
    private val threadService: ThreadService,
) {

    @PostMapping
    fun createThread(@CurrentUser userId: Long): ResponseEntity<ThreadApiResponse> {
        val response = threadService.createThread(userId)

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ThreadApiResponse(
                threadId = response.threadId,
                userId = response.userId,
                lastChatAt = response.lastChatAt,
                createdAt = response.createdAt,
            ),
        )
    }

    @GetMapping
    fun getThreads(@CurrentUser userId: Long): ResponseEntity<List<ThreadApiResponse>> {
        val threads = threadService.getThreads(userId)

        return ResponseEntity.ok(
            threads.map { thread ->
                ThreadApiResponse(
                    threadId = thread.threadId,
                    userId = thread.userId,
                    lastChatAt = thread.lastChatAt,
                    createdAt = thread.createdAt,
                )
            },
        )
    }

    @GetMapping("/{threadId}")
    fun getThread(
        @CurrentUser userId: Long,
        @PathVariable threadId: Long,
    ): ResponseEntity<ThreadApiResponse> {
        val response = threadService.getThread(userId, threadId)

        return ResponseEntity.ok(
            ThreadApiResponse(
                threadId = response.threadId,
                userId = response.userId,
                lastChatAt = response.lastChatAt,
                createdAt = response.createdAt,
            ),
        )
    }

    @DeleteMapping("/{threadId}")
    fun deleteThread(
        @CurrentUser userId: Long,
        @PathVariable threadId: Long,
    ): ResponseEntity<Void> {
        threadService.deleteThread(userId, threadId)
        return ResponseEntity.noContent().build()
    }
}
