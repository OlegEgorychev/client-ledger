package com.clientledger.app.data.backup

import com.clientledger.app.data.entity.*

/**
 * Complete snapshot of the database for backup/restore.
 * All entities use stable IDs to maintain referential integrity.
 */
data class BackupPayload(
    val createdAt: String, // ISO datetime string
    val appVersion: String, // App version name (e.g., "0.1.17")
    val schemaVersion: Int, // Database schema version (currently 9)
    
    // Core entities (all must be present, can be empty lists)
    val clients: List<ClientEntity>,
    val appointments: List<AppointmentEntity>,
    val expenses: List<ExpenseEntity>,
    val expenseItems: List<ExpenseItemEntity>,
    
    // Service-related entities
    val serviceTags: List<ServiceTagEntity>,
    val appointmentServices: List<AppointmentServiceEntity>
)
