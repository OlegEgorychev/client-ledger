package com.clientledger.app.data.backup

import android.content.Context
import android.util.Log
import com.clientledger.app.data.entity.*
import com.clientledger.app.data.repository.LedgerRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Type
import java.time.Instant
import java.time.format.DateTimeFormatter

private const val TAG = "BackupRepository"
private const val BACKUP_DIR = "backups"
private const val LATEST_BACKUP_FILE = "latest_backup.json"
private const val HISTORY_DIR = "history"
private const val MAX_HISTORY_BACKUPS = 10
private const val CURRENT_SCHEMA_VERSION = 8

/**
 * Repository for creating, reading, and restoring backups.
 * Backups are stored in app-private storage: /files/backups/
 */
class BackupRepository(
    private val context: Context,
    private val repository: LedgerRepository
) {
    private val backupDir: File = File(context.filesDir, BACKUP_DIR).apply {
        if (!exists()) mkdirs()
    }
    
    private val historyDir: File = File(backupDir, HISTORY_DIR).apply {
        if (!exists()) mkdirs()
    }
    
    private val latestBackupFile: File = File(backupDir, LATEST_BACKUP_FILE)
    
    // Gson with custom serializers for enums
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(ExpenseTag::class.java, ExpenseTagSerializer())
        .create()
    
    /**
     * Create a backup snapshot from current database state.
     */
    suspend fun createBackupSnapshot(appVersion: String): BackupPayload = withContext(Dispatchers.IO) {
        // Fetch all entities from database
        val clients = repository.getAllClientsSync()
        val appointments = repository.getAllAppointmentsSync()
        val expenses = repository.getAllExpensesSync()
        val expenseItems = repository.getAllExpenseItemsSync()
        val serviceTags = repository.getAllTagsSync()
        val appointmentServices = repository.getAllAppointmentServicesSync()
        
        // Create backup payload
        BackupPayload(
            createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            appVersion = appVersion,
            schemaVersion = CURRENT_SCHEMA_VERSION,
            clients = clients,
            appointments = appointments,
            expenses = expenses,
            expenseItems = expenseItems,
            serviceTags = serviceTags,
            appointmentServices = appointmentServices
        )
    }
    
    /**
     * Write backup payload to latest_backup.json and optionally save to history.
     */
    suspend fun writeBackup(payload: BackupPayload): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Write to latest_backup.json
            FileWriter(latestBackupFile).use { writer ->
                gson.toJson(payload, writer)
            }
            
            // Also save to history with timestamp
            val timestamp = payload.createdAt.replace(":", "-").replace(".", "-")
            val historyFile = File(historyDir, "backup_${timestamp}.json")
            FileWriter(historyFile).use { writer ->
                gson.toJson(payload, writer)
            }
            
            // Clean up old history files (keep only last N)
            cleanupOldHistoryBackups()
            
            Log.d(TAG, "Backup written successfully to ${latestBackupFile.absolutePath}")
            Result.success(latestBackupFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write backup", e)
            Result.failure(e)
        }
    }
    
    /**
     * Read backup from file.
     */
    suspend fun readBackup(file: File): Result<BackupPayload> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || !file.canRead()) {
                return@withContext Result.failure(IllegalArgumentException("Backup file does not exist or is not readable"))
            }
            
            FileReader(file).use { reader ->
                val payload = gson.fromJson(reader, BackupPayload::class.java)
                Result.success(payload)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read backup from ${file.absolutePath}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get the latest backup file if it exists.
     */
    fun getLatestBackupFile(): File? = if (latestBackupFile.exists()) latestBackupFile else null
    
    /**
     * Get the timestamp of the latest backup, or null if no backup exists.
     */
    suspend fun getLatestBackupTimestamp(): String? = withContext(Dispatchers.IO) {
        getLatestBackupFile()?.let { file ->
            readBackup(file).getOrNull()?.createdAt
        }
    }
    
    /**
     * Restore database from backup payload.
     * This clears all existing data and replaces it with backup data.
     * Uses ID mappings to maintain referential integrity.
     */
    suspend fun restoreFromBackup(payload: BackupPayload): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Validate schema version compatibility
            if (payload.schemaVersion > CURRENT_SCHEMA_VERSION) {
                return@withContext Result.failure(
                    IllegalArgumentException("Backup schema version (${payload.schemaVersion}) is newer than current app version ($CURRENT_SCHEMA_VERSION). Please update the app.")
                )
            }
            
            // Clear all existing data
            repository.clearAllDataForRestore()
            
            // ID mappings for maintaining referential integrity
            val clientIdMap = mutableMapOf<Long, Long>()
            val appointmentIdMap = mutableMapOf<Long, Long>()
            val expenseIdMap = mutableMapOf<Long, Long>()
            val serviceTagIdMap = mutableMapOf<Long, Long>()
            
            // 1. Insert clients (no dependencies)
            payload.clients.forEach { backupClient ->
                val newId = repository.insertClient(backupClient.copy(id = 0))
                clientIdMap[backupClient.id] = newId
            }
            
            // 2. Insert service tags (no dependencies)
            payload.serviceTags.forEach { backupTag ->
                val newId = repository.insertTag(backupTag.copy(id = 0))
                serviceTagIdMap[backupTag.id] = newId
            }
            
            // 3. Insert appointments (depends on clients)
            payload.appointments.forEach { backupAppointment ->
                val newClientId = clientIdMap[backupAppointment.clientId] ?: backupAppointment.clientId
                val newId = repository.insertAppointment(
                    backupAppointment.copy(id = 0, clientId = newClientId)
                )
                appointmentIdMap[backupAppointment.id] = newId
            }
            
            // 4. Insert appointment services (depends on appointments and service tags)
            val appointmentServicesToInsert = mutableListOf<AppointmentServiceEntity>()
            payload.appointmentServices.forEach { backupService ->
                val newAppointmentId = appointmentIdMap[backupService.appointmentId] ?: return@forEach
                val newServiceTagId = serviceTagIdMap[backupService.serviceTagId] ?: return@forEach
                
                appointmentServicesToInsert.add(
                    AppointmentServiceEntity(
                        id = 0,
                        appointmentId = newAppointmentId,
                        serviceTagId = newServiceTagId,
                        priceForThisTag = backupService.priceForThisTag,
                        sortOrder = backupService.sortOrder
                    )
                )
            }
            // Insert all appointment services at once
            if (appointmentServicesToInsert.isNotEmpty()) {
                repository.insertAppointmentServices(appointmentServicesToInsert)
            }
            
            // 5. Insert expenses with their items (depends on nothing)
            // Group items by expense ID before inserting expenses
            val expenseItemsByExpense = payload.expenseItems.groupBy { it.expenseId }
            
            payload.expenses.forEach { backupExpense ->
                val newId = repository.insertExpense(backupExpense.copy(id = 0))
                expenseIdMap[backupExpense.id] = newId
            }
            
            // Insert expense items after all expenses are created
            expenseItemsByExpense.forEach { (oldExpenseId, items) ->
                val newExpenseId = expenseIdMap[oldExpenseId] ?: return@forEach
                val expense = repository.getExpenseById(newExpenseId) ?: return@forEach
                val newItems = items.map { item ->
                    ExpenseItemEntity(
                        id = 0,
                        expenseId = newExpenseId,
                        tag = item.tag,
                        amountCents = item.amountCents
                    )
                }
                repository.saveExpenseWithItems(expense, newItems)
            }
            
            Log.d(TAG, "Backup restored successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore backup", e)
            Result.failure(e)
        }
    }
    
    private fun cleanupOldHistoryBackups() {
        try {
            val backupFiles = historyDir.listFiles()
                ?.filter { it.isFile && it.name.startsWith("backup_") && it.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() }
                ?: return
            
            if (backupFiles.size > MAX_HISTORY_BACKUPS) {
                backupFiles.drop(MAX_HISTORY_BACKUPS).forEach { file ->
                    file.delete()
                    Log.d(TAG, "Deleted old backup: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cleanup old backups", e)
        }
    }
}

// Custom serializers for ExpenseTag enum
private class ExpenseTagSerializer : JsonSerializer<ExpenseTag>, JsonDeserializer<ExpenseTag> {
    override fun serialize(
        src: ExpenseTag?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src?.name ?: "")
    }
    
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ExpenseTag? {
        return json?.asString?.let { 
            try {
                ExpenseTag.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
    }
}
