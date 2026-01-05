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
    val title: String,
    val spentAt: Long, // timestamp millis
    val dateKey: String, // ISO "YYYY-MM-DD"
    val amountCents: Long, // >= 0
    val createdAt: Long
)


