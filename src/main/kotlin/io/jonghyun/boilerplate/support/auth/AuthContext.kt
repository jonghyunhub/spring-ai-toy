package io.jonghyun.boilerplate.support.auth

object AuthContext {
    private val userIdHolder = ThreadLocal<Long>()

    fun setUserId(userId: Long) {
        userIdHolder.set(userId)
    }

    fun getUserId(): Long? {
        return userIdHolder.get()
    }

    fun clear() {
        userIdHolder.remove()
    }
}