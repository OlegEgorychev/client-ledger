package com.clientledger.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    CANCELED
}

@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clientId"), Index("dateKey"), Index("startsAt"), Index("status")]
)
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientId: Long,
    val title: String,
    val startsAt: Long, // timestamp millis
    val dateKey: String, // ISO "YYYY-MM-DD"
    val durationMinutes: Int = 60, // длительность сеанса в минутах, по умолчанию 60
    val incomeCents: Long, // >= 0
    val isPaid: Boolean,
    val status: String = AppointmentStatus.COMPLETED.name, // SCHEDULED, COMPLETED, CANCELED
    val canceledAt: Long? = null, // timestamp when canceled
    val cancelReason: String? = null, // optional reason for cancellation
    val isTestData: Boolean = false, // Flag to identify test data
    val createdAt: Long
)


