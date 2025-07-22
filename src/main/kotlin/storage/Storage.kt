package me.gusandr.storage

interface Storage {
    suspend fun isUserBlocked(id: Long): Boolean
    suspend fun isMessagesTimeoutOut(id: Long): Boolean
    suspend fun canMessage(id: Long): Boolean
    suspend fun addUserMessage(id: Long)
}