package com.clientledger.app.util

/**
 * Утилиты для форматирования телефонных номеров для отображения
 */
object PhoneFormatter {
    /**
     * Форматирует номер телефона для отображения (добавляет пробелы для читаемости)
     * @param phoneNumber Номер в формате E.164 (например, "+79161234567")
     * @return Отформатированный номер (например, "+7 916 123 45 67")
     */
    fun formatForDisplay(phoneNumber: String?): String {
        if (phoneNumber.isNullOrBlank()) {
            return "Не указан"
        }

        val country = Countries.findCountryByPhoneNumber(phoneNumber)
        if (country == null) {
            return phoneNumber // Возвращаем как есть, если не удалось определить страну
        }

        val numberWithoutCode = Countries.extractNumberWithoutCode(phoneNumber, country)
        
        // Форматирование для России (+7)
        if (country.code == "+7" && numberWithoutCode.length == 10) {
            // +7 916 123 45 67
            return "${country.code} ${numberWithoutCode.substring(0, 3)} ${numberWithoutCode.substring(3, 6)} ${numberWithoutCode.substring(6, 8)} ${numberWithoutCode.substring(8)}"
        }
        
        // Для других стран - простое форматирование (группы по 3-4 цифры)
        return when {
            numberWithoutCode.length <= 6 -> "${country.code} $numberWithoutCode"
            numberWithoutCode.length <= 9 -> {
                val part1 = numberWithoutCode.substring(0, 3)
                val part2 = numberWithoutCode.substring(3)
                "${country.code} $part1 $part2"
            }
            else -> {
                val part1 = numberWithoutCode.substring(0, 3)
                val part2 = numberWithoutCode.substring(3, 6)
                val part3 = numberWithoutCode.substring(6)
                "${country.code} $part1 $part2 $part3"
            }
        }
    }
}

