package me.gusandr.config

object Config {
    const val TOKEN = ""
    const val ADMIN_ID = 0L
    const val MESSAGES_LIMIT_SECONDS = 3L
    const val MAX_MESSAGES_MINUTES_IN_CACHE = 30L
    const val MAX_MESSAGE_IN_INTERVAL = 5L
    const val BLOCK_TIMEOUT_MINUTES = 15L
    const val LOCK_CLEANUP_THRESHOLD = 1000L

    object Messages {
        const val NO_USERNAME = "❌ Необходим username для использования бота"
        const val BLOCKED = "🚫 Вы временно заблокированы"
        const val TOO_FAST_CALLBACK = "⏰ Подождите $MESSAGES_LIMIT_SECONDS секунды перед отправкой следующего сообщения"
        const val EDIT_MESSAGE = "✍️ <b>Отправьте ваше сообщение</b>\n\nМожете отправить текст, фото, документ, видео, аудио или голосовое сообщение."
        const val ACCESS_DENIED = "❌ <b>Доступ запрещен</b>\n\nДля использования бота необходимо установить username в настройках Telegram."
        const val ACCESS_DENIED_EXTENDED = "❌ <b>Доступ запрещен</b>\n\nДля использования бота необходимо установить username в настройках Telegram.\n\n📱 <i>Настройки → Редактировать профиль → Имя пользователя</i>"
        const val TEMP_BLOCK = "🚫 <b>Временная блокировка</b>\n\nВы заблокированы на $BLOCK_TIMEOUT_MINUTES минут за подозрение в спам-атаке."
        const val TOO_FAST_MESSAGE = "⏰ <b>Слишком быстро!</b>\n\nСообщение не было доставлено.\nПожалуйста, подождите $MESSAGES_LIMIT_SECONDS секунды перед отправкой следующего сообщения.\n\n⚠️ <i>Это поможет избежать блокировки за спам.</i>"
        const val LIMIT_EXCEEDED = "🚫 <b>Лимит превышен!</b>\n\nВы заблокированы на $BLOCK_TIMEOUT_MINUTES минут по подозрению в спам-атаке.\n\n⏰ <i>Попробуйте позже.</i>"
        const val SENDING_ERROR = "❌ <b>Ошибка отправки</b>\n\nПроизошла ошибка при отправке сообщения. Попробуйте позже."

        const val MESSAGE_SENT = "✅ <b>Сообщение отправлено!</b>\n\n📤 Ваше сообщение успешно доставлено."
        const val NEW_MESSAGE = "📨 <b>НОВОЕ СООБЩЕНИЕ</b>\n\n%s"

        fun welcomeMessage(firstName: String) = """
            👋 <b>Привет, $firstName!</b>
            
            Этот бот поможет вам связаться со мной, если у вас нет возможности написать в ЛС.
            
            📝 <b>Что можно отправить:</b>
            • Текстовые сообщения
            • Фотографии и изображения
            • Документы и файлы
            • Видео и аудио
            • Голосовые сообщения
            
            🚀 <b>Готовы начать?</b>
        """.trimIndent()

        fun userInfo(firstName: String, lastName: String?, username: String?) = """
            👤 <b>Пользователь:</b> $firstName ${lastName ?: ""}
            📧 <b>Username:</b> @${username ?: "N/A"}
            ${"─".repeat(30)}
        """.trimIndent()
    }

    object Buttons {
        const val WRITE_MESSAGE = "📝 Написать сообщение"
        const val WRITE_AGAIN = "📝 Написать ещё"
    }
}