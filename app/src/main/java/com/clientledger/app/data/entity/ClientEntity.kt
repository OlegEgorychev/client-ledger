package com.clientledger.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val gender: String, // "male", "female", "other"
    val birthDate: String? = null, // ISO "YYYY-MM-DD"
    val phone: String,
    val telegram: String? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)


