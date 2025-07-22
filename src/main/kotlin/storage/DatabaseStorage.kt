package me.gusandr.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.gusandr.Users
import me.gusandr.config.Config
import me.gusandr.sqldelight.hockey.data.UsersQueries
import java.io.File
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object DatabaseStorage : Storage {
    private val dbPath = File(System.getProperty("user.home"), "tg_rescue_data/users.db").absolutePath

    private val driver: SqlDriver by lazy {
        File(dbPath).parentFile.mkdirs()

        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:$dbPath",
            properties = Properties().apply {
                put("foreign_keys", "true")
                put("journal_mode", "WAL")
                put("enable_native_loading", "false")
            }
        )

        if (!File(dbPath).exists()) {
            Users.Schema.create(driver)
        }

        driver
    }

    private val database = Users(driver)
    private val userQueries: UsersQueries = database.usersQueries
    private val userLocks = ConcurrentHashMap<Long, Mutex>()

    private fun getMutexForUser(userId: Long): Mutex {
        if (userLocks.size > Config.LOCK_CLEANUP_THRESHOLD) {
            cleanUpStaleLocks()
        }
        return userLocks.computeIfAbsent(userId) { Mutex() }
    }

    private fun cleanUpStaleLocks() {
        val iterator = userLocks.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.tryLock()) {
                try {
                    iterator.remove()
                } finally {
                    entry.value.unlock()
                }
            }
        }
    }

    override suspend fun isUserBlocked(id: Long): Boolean = withContext(Dispatchers.IO) {
        executeSafe {
            val user = userQueries.selectBanTime(id).executeAsOneOrNull() ?: return@executeSafe false
            val blockTime = user.blockTime ?: return@executeSafe false

            if (isBlockActive(blockTime)) {
                true
            } else {
                clearUserBlockStatus(id)
                false
            }
        }
    }

    override suspend fun isMessagesTimeoutOut(id: Long): Boolean = withContext(Dispatchers.IO) {
        executeSafe {
            val now = System.currentTimeMillis()
            val messages = getUserMessages(id)
            hasNoRecentMessages(messages, now)
        }
    }

    override suspend fun canMessage(id: Long): Boolean =
        !isMessagesTimeoutOut(id) && !isUserBlocked(id)

    override suspend fun addUserMessage(id: Long) {
        val mutex = getMutexForUser(id)
        mutex.withLock {
            withContext(Dispatchers.IO) {
                executeSafe {
                    database.transaction {
                        val now = System.currentTimeMillis()
                        val cacheDurationMillis = TimeUnit.MINUTES.toMillis(Config.MAX_MESSAGES_MINUTES_IN_CACHE)
                        val timeThreshold = now - cacheDurationMillis

                        val currentMessages = getUserMessages(id)
                        val recentMessages = currentMessages.filter { it > timeThreshold }
                        val updatedMessages = (recentMessages + now).sorted()

                        updateUserMessages(id, updatedMessages.joinToString(" "))

                        if (shouldBlockUser(updatedMessages)) {
                            blockUser(id, now)
                        }
                    }
                }
            }
        }
    }

    private fun isBlockActive(blockTime: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val blockDurationMillis = TimeUnit.MINUTES.toMillis(Config.BLOCK_TIMEOUT_MINUTES)
        return currentTime - blockTime < blockDurationMillis
    }

    private fun clearUserBlockStatus(id: Long) {
        userQueries.changeBanStatus(isBanned = false, blockTime = 0L, userId = id)
    }

    private fun hasNoRecentMessages(messages: List<Long>, currentTime: Long): Boolean {
        if (messages.isEmpty()) return true

        val timeLimit = TimeUnit.SECONDS.toMillis(Config.MESSAGES_LIMIT_SECONDS)
        return messages.none { currentTime - it < timeLimit }
    }

    private fun getUserMessages(userId: Long): List<Long> {
        return userQueries.selectMessageUser(userId).executeAsOneOrNull()
            ?.messageIds
            ?.takeIf { it.isNotBlank() }
            ?.splitToLongList()
            ?: emptyList()
    }

    private fun String.splitToLongList(): List<Long> {
        return split("\\s+".toRegex())
            .mapNotNull { it.toLongOrNull() }
    }

    private fun updateUserMessages(userId: Long, messages: String) {
        if (userQueries.selectMessageUser(userId).executeAsOneOrNull() == null) {
            userQueries.insert(userId = userId, messageIds = messages, isBanned = false)
        } else {
            userQueries.changeMessage(messageIds = messages, userId = userId)
        }
    }

    private fun shouldBlockUser(messages: List<Long>): Boolean {
        if (messages.size <= Config.MAX_MESSAGE_IN_INTERVAL) return false

        val timeThreshold = TimeUnit.SECONDS.toMillis(Config.MESSAGES_LIMIT_SECONDS)
        val rapidMessagePairs = countRapidMessagePairs(messages, timeThreshold)

        return rapidMessagePairs > Config.MAX_MESSAGE_IN_INTERVAL
    }

    private fun countRapidMessagePairs(messages: List<Long>, maxGap: Long): Int {
        val sortedMessages = messages.sorted()
        return sortedMessages.zipWithNext { first, second ->
            if (second - first < maxGap) 1 else 0
        }.sum()
    }

    private fun blockUser(userId: Long, blockTime: Long) {
        userQueries.changeBanStatus(isBanned = true, blockTime = blockTime, userId = userId)
    }

    private suspend fun <T> executeSafe(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: Exception) {
            println("Database operation failed: ${e.message}")
            when (e) {
                is IllegalStateException -> throw e
                else -> throw Exception("Database operation failed", e)
            }
        }
    }
}