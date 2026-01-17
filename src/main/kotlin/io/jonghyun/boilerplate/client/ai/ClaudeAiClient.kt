package io.jonghyun.boilerplate.client.ai

import com.fasterxml.jackson.databind.ObjectMapper
import feign.Response
import io.jonghyun.boilerplate.client.ai.dto.ClaudeMessageDto
import io.jonghyun.boilerplate.client.ai.dto.ClaudeRequest
import io.jonghyun.boilerplate.client.ai.dto.ClaudeStreamResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader

@Component("claudeAiClient")
class ClaudeAiClient(
    private val claudeFeignClient: ClaudeFeignClient,
    @Value("\${claude.api-key}") private val apiKey: String, // application.yml에서 키 관리
    private val objectMapper: ObjectMapper
) : AiClient {

    // 기본 모델: 가장 저렴한 Haiku 사용
    private val defaultModel = "claude-3-haiku-20240307"

    override fun chat(messages: List<ChatMessage>, model: String?): String {
        // 1. 요청 변환 (ChatMessage -> ClaudeMessageDto)
        val claudeMessages = messages.map { msg ->
            ClaudeMessageDto(
                role = mapRole(msg.role),
                content = msg.content
            )
        }

        // 2. 요청 객체 생성
        val request = ClaudeRequest(
            model = model ?: defaultModel,
            messages = claudeMessages
        )

        // 3. Feign 호출 및 예외 처리 (간단하게 try-catch 없이 작성했으나 실무에선 필요)
        val response = claudeFeignClient.createMessage(
            apiKey = apiKey,
            request = request
        )

        // 4. 응답 텍스트 추출
        return response.content.firstOrNull()?.text ?: "답변을 생성하지 못했습니다."
    }


    override fun chatStream(messages: List<ChatMessage>, model: String?, onChunk: (String) -> Unit) {
        // 1. 요청 객체 생성 (stream = true 설정 필수!)
        // *주의: ClaudeRequest DTO에 val stream: Boolean = false 필드를 추가해야 합니다.
        val request = ClaudeRequest(
            model = model ?: defaultModel,
            messages = messages.map { ClaudeMessageDto(mapRole(it.role), it.content) },
            stream = true
        )

        var response: Response? = null
        try {
            // 2. Feign 호출 (Response 객체 받기)
            response = claudeFeignClient.createMessageStream(apiKey = apiKey, request = request)

            // 3. InputStream으로 데이터 읽기
            val inputStream = response.body().asInputStream()
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // 4. SSE 포맷 파싱 (data: 로 시작하는 줄만 처리)
                if (line!!.startsWith("data: ")) {
                    val jsonPart = line!!.substring(6) // "data: " 제거

                    // "[DONE]" 메시지가 오면 종료
                    if (jsonPart == "[DONE]") break

                    try {
                        val streamResponse = objectMapper.readValue(jsonPart, ClaudeStreamResponse::class.java)

                        // 5. 텍스트 델타가 있으면 콜백 호출
                        if (streamResponse.type == "content_block_delta" && streamResponse.delta?.type == "text_delta") {
                            streamResponse.delta.text?.let { onChunk(it) }
                        }
                    } catch (e: Exception) {
                        // 파싱 에러는 무시하고 계속 진행 (핑이나 다른 이벤트일 수 있음)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onChunk("\n[오류 발생: ${e.message}]")
        } finally {
            // 6. 리소스 정리
            response?.close()
        }
    }

    private fun mapRole(role: MessageRole): String {
        return when (role) {
            MessageRole.USER -> "user"
            MessageRole.ASSISTANT -> "assistant"
        }
    }
}