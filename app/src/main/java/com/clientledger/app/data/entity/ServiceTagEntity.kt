package com.clientledger.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_tags",
    indices = [Index("name", unique = true)]
)
data class ServiceTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // unique
    val defaultPrice: Int, // in cents
    val isActive: Boolean = true,
    val sortOrder: Int = 0 // for ordering in UI
)
