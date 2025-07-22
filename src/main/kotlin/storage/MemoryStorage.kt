package me.gusandr.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.gusandr.config.Config.BLOCK_TIMEOUT_MINUTES
import me.gusandr.config.Config.MAX_MESSAGES_MINUTES_IN_CACHE
import me.gusandr.config.Config.MAX_MESSAGE_IN_INTERVAL
import me.gusandr.config.Config.MESSAGES_LIMIT_SECONDS
import java.util.concurrent.TimeUnit

@Deprecated("use DatabaseStorage", ReplaceWith("DatabaseStorage"), DeprecationLevel.WARNING)
object MemoryStorage : Storage {
    private val userMessages = mutableMapOf<Long, MutableList<Long>>()
    private val blockedUsers = mutableMapOf<Long, Long>()
    private val userMessagesMutex = Mutex()
    private val blockedUsersMutex = Mutex()

    override suspend fun isUserBlocked(id: Long): Boolean {
        return blockedUsersMutex.withLock {
            blockedUsers[id]?.let { blockTime ->
                val currentTime = System.currentTimeMillis()
                if (currentTime - blockTime < TimeUnit.MINUTES.toMillis(BLOCK_TIMEOUT_MINUTES)) {
                    true
                } else {
                    blockedUsers.remove(id)
                    false
                }
            } ?: false
        }
    }

    override suspend fun isMessagesTimeoutOut(id: Long): Boolean {
        return userMessagesMutex.withLock {
            val now = System.currentTimeMillis()
            val userMessagesList = userMessages[id] ?: mutableListOf()

            val recentMessages = userMessagesList.filter { now - it < TimeUnit.SECONDS.toMillis(MESSAGES_LIMIT_SECONDS) }
            userMessages[id] = recentMessages.toMutableList()

            recentMessages.isEmpty()
        }
    }

    override suspend fun canMessage(id: Long): Boolean {
        return !isMessagesTimeoutOut(id) && !isUserBlocked(id)
    }

    override suspend fun addUserMessage(id: Long) {
        return userMessagesMutex.withLock {
            val now = System.currentTimeMillis()
            val messages = userMessages.getOrPut(id) { mutableListOf() }
            messages.add(now)

            val hourAgo = now - TimeUnit.MINUTES.toMillis(MAX_MESSAGES_MINUTES_IN_CACHE)
            val recentMessages = messages.filter { it > hourAgo }
            userMessages[id] = recentMessages.toMutableList()

            if (messages.size > MAX_MESSAGE_IN_INTERVAL) {
                blockedUsersMutex.withLock {
                    blockedUsers[id] = now
                }
            }
        }
    }
}