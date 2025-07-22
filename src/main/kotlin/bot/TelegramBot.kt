package me.gusandr.bot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.CallbackQueryHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.handlers.HandleMessage
import com.github.kotlintelegrambot.dispatcher.handlers.MessageHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import me.gusandr.config.Config.Buttons.WRITE_AGAIN
import me.gusandr.config.Config.Buttons.WRITE_MESSAGE
import me.gusandr.config.Config.Messages
import me.gusandr.config.Config.Messages.ACCESS_DENIED
import me.gusandr.config.Config.Messages.ACCESS_DENIED_EXTENDED
import me.gusandr.config.Config.Messages.EDIT_MESSAGE
import me.gusandr.config.Config.Messages.LIMIT_EXCEEDED
import me.gusandr.config.Config.Messages.MESSAGE_SENT
import me.gusandr.config.Config.Messages.NEW_MESSAGE
import me.gusandr.config.Config.Messages.NO_USERNAME
import me.gusandr.config.Config.Messages.SENDING_ERROR
import me.gusandr.config.Config.Messages.TEMP_BLOCK
import me.gusandr.config.Config.Messages.TOO_FAST_CALLBACK
import me.gusandr.config.Config.Messages.TOO_FAST_MESSAGE
import me.gusandr.config.Config.Messages.welcomeMessage
import me.gusandr.storage.Storage

class TelegramBot(
    private val tokenBot: String,
    private val adminId: Long,
    private val storage: Storage
) {
    private val bot = bot {
        this.token = tokenBot
        dispatch {
            command("start") { handleStart(this) }
            callbackQuery("write_message") { handleWriteMessageCallback(this) }
            message { handleMessage(this) }
        }
    }

    fun start() = bot.startPolling()

    private suspend fun handleStart(env: CommandHandlerEnvironment) {
        val user = env.message.from ?: return
        val chatId = env.message.chat.id

        if (user.username.isNullOrEmpty()) {
            env.bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = ACCESS_DENIED_EXTENDED,
                parseMode = ParseMode.HTML
            )
            return
        }

        if (user.isBlocked(storage)) {
            env.bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = TEMP_BLOCK,
                parseMode = ParseMode.HTML
            )
            return
        }

        val keyboard = InlineKeyboardMarkup.create(
            listOf(InlineKeyboardButton.CallbackData(WRITE_MESSAGE, "write_message"))
        )

        env.bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = welcomeMessage(user.firstName),
            parseMode = ParseMode.HTML,
            replyMarkup = keyboard
        )
    }

    private suspend fun handleWriteMessageCallback(callback: CallbackQueryHandlerEnvironment) {
        val user = callback.callbackQuery.from
        val message = callback.callbackQuery.message ?: return
        val chatId = message.chat.id

        when {
            user.username.isNullOrEmpty() -> callback.bot.answerCallbackQuery(
                callbackQueryId = callback.callbackQuery.id,
                text = NO_USERNAME,
                showAlert = true
            )

            user.isBlocked(storage) -> callback.bot.answerCallbackQuery(
                callbackQueryId = callback.callbackQuery.id,
                text = Messages.BLOCKED,
                showAlert = true
            )

            !user.isMessagesTimeoutOut(storage) -> callback.bot.answerCallbackQuery(
                callbackQueryId = callback.callbackQuery.id,
                text = TOO_FAST_CALLBACK,
                showAlert = true
            )

            else -> {
                callback.bot.editMessageText(
                    chatId = ChatId.fromId(chatId),
                    messageId = message.messageId,
                    text = EDIT_MESSAGE,
                    parseMode = ParseMode.HTML
                )
                callback.bot.answerCallbackQuery(callback.callbackQuery.id)
            }
        }
    }

    private suspend fun handleMessage(context: MessageHandlerEnvironment) {
        val message = context.message
        val user = message.from ?: return
        val chatId = message.chat.id

        if (message.text?.startsWith("/") == true) return

        when {
            user.username.isNullOrEmpty() -> context.bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = ACCESS_DENIED,
                parseMode = ParseMode.HTML
            )

            user.isBlocked(storage) -> context.bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = TEMP_BLOCK,
                parseMode = ParseMode.HTML
            )

            !user.isMessagesTimeoutOut(storage) -> {
                context.bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = TOO_FAST_MESSAGE,
                    parseMode = ParseMode.HTML
                )
                user.addMessageToStorage(storage)
            }

            else -> processUserMessage(user, message, chatId, context.bot)
        }
    }

    private suspend fun processUserMessage(
        user: User,
        message: Message,
        chatId: Long,
        bot: Bot
    ) {
        try {
            bot.sendMessage(
                chatId = ChatId.fromId(adminId),
                text = NEW_MESSAGE.format(user.getInfo()),
                parseMode = ParseMode.HTML
            )

            bot.forwardMessage(
                chatId = ChatId.fromId(adminId),
                fromChatId = ChatId.fromId(chatId),
                messageId = message.messageId
            )

            if (user.canMessage(storage)) {
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = LIMIT_EXCEEDED,
                    parseMode = ParseMode.HTML
                )
            } else {
                val keyboard = InlineKeyboardMarkup.create(
                    listOf(InlineKeyboardButton.CallbackData(WRITE_AGAIN, "write_message"))
                )

                user.addMessageToStorage(storage)

                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = MESSAGE_SENT,
                    parseMode = ParseMode.HTML,
                    replyMarkup = keyboard
                )
            }
        } catch (e: Exception) {
            println(e.stackTraceToString())
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = SENDING_ERROR,
                parseMode = ParseMode.HTML
            )
        }
    }
}