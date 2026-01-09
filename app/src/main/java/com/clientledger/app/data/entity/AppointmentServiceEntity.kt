package com.clientledger.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "appointment_services",
    foreignKeys = [
        ForeignKey(
            entity = AppointmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["appointmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ServiceTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["serviceTagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("appointmentId"),
        Index("serviceTagId"),
        Index("appointmentId", "serviceTagId", unique = true) // prevent duplicate tags per appointment
    ]
)
data class AppointmentServiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appointmentId: Long,
    val serviceTagId: Long,
    val priceForThisTag: Int, // in cents, allows override from default
    val sortOrder: Int = 0 // for ordering tags in UI
)
