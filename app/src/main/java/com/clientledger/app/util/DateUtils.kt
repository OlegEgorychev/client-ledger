package com.clientledger.app.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Extension functions - top-level для прямого использования
fun LocalDate.toMillis(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun LocalDateTime.toMillis(): Long {
    return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun Long.toLocalDate(): LocalDate {
    return java.time.Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

fun Long.toLocalDateTime(): LocalDateTime {
    return java.time.Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
}

fun LocalDate.toDateKey(): String {
    return toString()
}

fun String.toLocalDate(): LocalDate {
    return LocalDate.parse(this)
}

object DateUtils {
    fun dateTimeToLocalDateTime(millis: Long): LocalDateTime {
        return java.time.Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    fun toLocalDate(dateKey: String): LocalDate {
        return LocalDate.parse(dateKey)
    }

    fun formatDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale("ru"))
        return date.format(formatter)
    }

    fun formatMonth(month: YearMonth): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale("ru"))
        return month.format(formatter)
    }

    fun formatDateTime(dateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", java.util.Locale("ru"))
        return dateTime.format(formatter)
    }

    fun getStartOfMonth(month: YearMonth): LocalDate {
        return month.atDay(1)
    }

    fun getEndOfMonth(month: YearMonth): LocalDate {
        return month.atEndOfMonth()
    }

    fun getStartOfYear(year: Int): LocalDate {
        return LocalDate.of(year, 1, 1)
    }

    fun getEndOfYear(year: Int): LocalDate {
        return LocalDate.of(year, 12, 31)
    }
}


