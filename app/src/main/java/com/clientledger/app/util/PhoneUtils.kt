package com.clientledger.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object PhoneUtils {
    /**
     * Открывает системный dialer с указанным номером телефона
     * @param context Контекст приложения
     * @param phoneNumber Номер телефона в формате E.164 (например, "+79161234567")
     * @return true если dialer успешно открыт, false если произошла ошибка
     */
    fun openDialer(context: Context, phoneNumber: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Проверяет, является ли номер телефона валидным для звонка
     * @param phoneNumber Номер телефона
     * @return true если номер валиден (не пустой и начинается с +)
     */
    fun isValidPhoneForCall(phoneNumber: String?): Boolean {
        return !phoneNumber.isNullOrBlank() && phoneNumber.startsWith("+")
    }
}

