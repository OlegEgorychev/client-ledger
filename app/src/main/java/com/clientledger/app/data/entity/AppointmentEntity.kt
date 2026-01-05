package com.clientledger.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    indices = [Index("clientId"), Index("dateKey"), Index("startsAt")]
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
    val createdAt: Long
)


