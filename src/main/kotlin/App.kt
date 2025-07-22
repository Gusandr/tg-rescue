package me.gusandr

import me.gusandr.bot.TelegramBot
import me.gusandr.config.Config
import me.gusandr.storage.DatabaseStorage
import me.gusandr.storage.Storage

object App {

    @JvmStatic
    fun main(args: Array<String>) {
        Class.forName("org.sqlite.JDBC")
        val storage: Storage = DatabaseStorage
        val bot = TelegramBot(
            tokenBot = Config.TOKEN,
            adminId = Config.ADMIN_ID,
            storage = storage
        )
        bot.start()
    }
}