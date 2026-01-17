package io.jonghyun.boilerplate.client.ai

import org.junit.jupiter.api.DisplayName

/**
 * MockAiClient 테스트
 * AiClientContractTest를 상속받아 계약을 검증합니다.
 */
@DisplayName("MockAiClient 테스트")
class MockAiClientTest : AiClientContractTest() {

    override fun createAiClient(): AiClient {
        return MockAiClient()
    }
}
