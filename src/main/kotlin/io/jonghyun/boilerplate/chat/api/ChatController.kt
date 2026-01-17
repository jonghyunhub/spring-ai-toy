package io.jonghyun.boilerplate.chat.api

import io.jonghyun.boilerplate.chat.api.req.CreateChatRequest
import io.jonghyun.boilerplate.chat.api.res.ChatApiResponse
import io.jonghyun.boilerplate.chat.api.res.ChatStreamChunk
import io.jonghyun.boilerplate.chat.application.ChatService
import io.jonghyun.boilerplate.support.auth.CurrentUser
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException

@RestController
@RequestMapping("/threads/{threadId}/chats")
class ChatController(
    private val chatService: ChatService,
) {

    @PostMapping
    fun createChat(
        @CurrentUser userId: Long,
        @PathVariable threadId: Long,
        @RequestBody request: CreateChatRequest,
    ): ResponseEntity<Any> {
        // 스트리밍 요청일 경우 스트리밍 엔드포인트로 리다이렉트
        if (request.isStreaming) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to "Use streaming endpoint for streaming requests"))
        }

        val response = chatService.createChat(
            userId = userId,
            threadId = threadId,
            question = request.question,
            model = request.model,
        )

        return ResponseEntity.ok(
            ChatApiResponse(
                chatId = response.chatId,
                threadId = response.threadId,
                question = response.question,
                answer = response.answer,
                createdAt = response.createdAt,
            ),
        )
    }

    @PostMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun createChatStream(
        @CurrentUser userId: Long,
        @PathVariable threadId: Long,
        @RequestBody request: CreateChatRequest,
    ): SseEmitter {
        val emitter = SseEmitter(60000L) // 60초 타임아웃

        Thread {
            try {
                val chatId = chatService.createChatStream(
                    userId = userId,
                    threadId = threadId,
                    question = request.question,
                    model = request.model,
                ) { chunk ->
                    try {
                        emitter.send(
                            SseEmitter.event()
                                .data(ChatStreamChunk(content = chunk, isDone = false)),
                        )
                    } catch (e: IOException) {
                        emitter.completeWithError(e)
                    }
                }

                // 마지막 이벤트 전송
                emitter.send(
                    SseEmitter.event()
                        .data(ChatStreamChunk(chatId = chatId, isDone = true)),
                )
                emitter.complete()
            } catch (e: Exception) {
                emitter.completeWithError(e)
            }
        }.start()

        return emitter
    }
}
