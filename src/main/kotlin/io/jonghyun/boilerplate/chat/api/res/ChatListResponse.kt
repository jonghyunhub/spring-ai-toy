package io.jonghyun.boilerplate.chat.api.res

data class ChatListResponse(
    val threads: List<ThreadWithChatsResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
)
