package com.clientledger.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Личное дело мастера (врач, встреча, выходной и т.п.).
 * Не связано с клиентами и не участвует в статистике дохода —
 * нужно только чтобы видеть занятое время при планировании записей.
 */
@Entity(
    tableName = "personal_events",
    indices = [Index("dateKey")]
)
data class PersonalEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val dateKey: String, // ISO "YYYY-MM-DD"
    val isAllDay: Boolean = false,
    val startMinute: Int, // минуты от начала дня (0..1439); для «весь день» = 0
    val endMinute: Int, // минуты от начала дня (1..1440); для «весь день» = 1440
    val note: String? = null,
    val createdAt: Long
) {
    /** Пересекается ли с интервалом [start, end) в минутах от начала дня. */
    fun overlaps(start: Int, end: Int): Boolean =
        isAllDay || (start < endMinute && end > startMinute)

    fun timeRangeText(): String =
        if (isAllDay) "Весь день" else "${formatMinutes(startMinute)}–${formatMinutes(endMinute)}"

    companion object {
        const val ALL_DAY_START = 0
        const val ALL_DAY_END = 24 * 60

        fun formatMinutes(minutes: Int): String =
            String.format("%02d:%02d", minutes / 60, minutes % 60)
    }
}
