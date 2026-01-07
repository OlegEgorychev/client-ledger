package com.clientledger.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object TelegramUtils {
    /**
     * Нормализует Telegram username:
     * - убирает пробелы (trim)
     * - убирает ведущий "@"
     * @param username Telegram username (может быть с @ или без)
     * @return нормализованный username без @ и пробелов, или null если username пустой/невалидный
     */
    fun normalizeUsername(username: String?): String? {
        if (username.isNullOrBlank()) return null
        
        val normalized = username.trim().removePrefix("@")
        return if (normalized.isNotBlank()) normalized else null
    }

    /**
     * Проверяет, является ли Telegram username валидным для открытия чата
     * @param username Telegram username
     * @return true если username валиден (не пустой после нормализации)
     */
    fun isValidUsername(username: String?): Boolean {
        return normalizeUsername(username) != null
    }

    /**
     * Открывает чат в Telegram по username
     * Если Telegram не установлен, открывает t.me в браузере
     * @param context Контекст приложения
     * @param username Telegram username (может быть с @ или без)
     * @return true если чат успешно открыт, false если произошла ошибка
     */
    fun openTelegramChat(context: Context, username: String?): Boolean {
        val normalizedUsername = normalizeUsername(username) ?: return false
        
        return try {
            val url = "https://t.me/$normalizedUsername"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}


