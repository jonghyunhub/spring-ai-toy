package io.jonghyun.boilerplate.client.exmaple

import io.jonghyun.boilerplate.client.exmaple.model.ExampleClientResult

internal data class ExampleResponseDto(
    val exampleResponseValue: String,
) {
    fun toResult(): ExampleClientResult {
        return ExampleClientResult(exampleResponseValue)
    }
}
