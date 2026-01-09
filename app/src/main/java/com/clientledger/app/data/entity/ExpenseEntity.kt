package com.clientledger.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [Index("dateKey"), Index("spentAt")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val spentAt: Long, // timestamp millis
    val dateKey: String, // ISO "YYYY-MM-DD"
    val totalAmountCents: Long, // >= 0, sum of all items
    val note: String? = null, // optional note
    val createdAt: Long
)


