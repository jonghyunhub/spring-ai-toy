package io.jonghyun.boilerplate.support.error

class CoreException(
    val errorType: ErrorType,
    val data: Any? = null,
) : RuntimeException(errorType.message)
