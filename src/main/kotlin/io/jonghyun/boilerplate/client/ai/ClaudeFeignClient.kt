package io.jonghyun.boilerplate.client.ai

import feign.Response
import io.jonghyun.boilerplate.client.ai.dto.ClaudeRequest
import io.jonghyun.boilerplate.client.ai.dto.ClaudeResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader

@FeignClient(name = "claudeClient", url = "https://api.anthropic.com")
interface ClaudeFeignClient {

    @PostMapping("/v1/messages")
    fun createMessage(
        @RequestHeader("x-api-key") apiKey: String,
        @RequestHeader("anthropic-version") version: String = "2023-06-01", // 필수 버전 헤더
        @RequestHeader("content-type") contentType: String = "application/json",
        @RequestBody request: ClaudeRequest
    ): ClaudeResponse


    @PostMapping("/v1/messages")
    fun createMessageStream(
        @RequestHeader("x-api-key") apiKey: String,
        @RequestHeader("anthropic-version") version: String = "2023-06-01",
        @RequestHeader("content-type") contentType: String = "application/json",
        @RequestBody request: ClaudeRequest
    ): Response
}