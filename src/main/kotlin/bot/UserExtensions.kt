package me.gusandr.bot

import com.github.kotlintelegrambot.entities.User
import me.gusandr.config.Config
import me.gusandr.storage.Storage

suspend fun User.isBlocked(storage: Storage): Boolean = storage.isUserBlocked(this.id)

suspend fun User.isMessagesTimeoutOut(storage: Storage): Boolean = storage.isMessagesTimeoutOut(this.id)

suspend fun User.canMessage(storage: Storage): Boolean = storage.canMessage(this.id)

suspend fun User.addMessageToStorage(storage: Storage) = storage.addUserMessage(this.id)

fun User.getInfo(): String {
    return Config.Messages.userInfo(
        firstName = this.firstName,
        lastName = this.lastName,
        username = this.username
    )
}