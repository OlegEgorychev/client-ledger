package com.clientledger.app.data.backup

import android.content.Context
import android.util.Log
import com.clientledger.app.data.repository.LedgerRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

private const val TAG = "BackupScheduler"
private const val DEBOUNCE_DELAY_MS = 2000L // 2 seconds debounce window

/**
 * Scheduler for automatic backups after data changes.
 * Uses debouncing to avoid multiple backups when multiple changes happen quickly.
 */
class BackupScheduler(
    private val context: Context,
    private val repository: LedgerRepository,
    private val appVersion: String,
    private val googleDriveService: GoogleDriveBackupService? = null
) {
    private val backupRepository = BackupRepository(context, repository)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var backupJob: Job? = null
    private val _lastBackupTimestamp = MutableStateFlow<String?>(null)
    val lastBackupTimestamp: StateFlow<String?> = _lastBackupTimestamp
    
    /**
     * Trigger a backup after data change.
     * This will cancel any pending backup and schedule a new one after debounce delay.
     */
    fun scheduleBackup() {
        // Cancel any existing pending backup
        backupJob?.cancel()
        
        // Schedule new backup after debounce delay
        backupJob = scope.launch {
            try {
                delay(DEBOUNCE_DELAY_MS)
                performBackup()
            } catch (e: CancellationException) {
                // Backup was cancelled, this is expected when new changes arrive
                Log.d(TAG, "Backup cancelled due to new data change")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform scheduled backup", e)
            }
        }
    }
    
    /**
     * Perform backup immediately (for manual backup actions).
     */
    suspend fun performBackupNow(): Result<BackupRepository.BackupFileInfo> = withContext(Dispatchers.IO) {
        // Cancel any pending backup
        backupJob?.cancel()
        
        performBackup()
    }
    
    /**
     * Internal method to perform the actual backup.
     */
    private suspend fun performBackup(): Result<BackupRepository.BackupFileInfo> {
        return try {
            Log.d(TAG, "Creating backup snapshot...")
            val payload = backupRepository.createBackupSnapshot(appVersion)
            
            Log.d(TAG, "Writing backup to file...")
            val result = backupRepository.writeBackup(payload)
            
            result.onSuccess { backupInfo ->
                _lastBackupTimestamp.value = payload.createdAt
                Log.d(TAG, "Backup completed successfully: ${backupInfo.fileName} at ${payload.createdAt}")
                
                // Upload to Google Drive if service is available and user is signed in
                googleDriveService?.let { driveService ->
                    if (driveService.isSignedIn()) {
                        scope.launch {
                            driveService.uploadBackupToDrive(backupInfo.file, backupInfo.fileName)
                                .onSuccess { fileId ->
                                    Log.d(TAG, "Backup uploaded to Google Drive: $fileId")
                                }
                                .onFailure { error ->
                                    Log.e(TAG, "Failed to upload backup to Google Drive", error)
                                    // Don't fail the backup if Drive upload fails
                                }
                        }
                    }
                }
            }.onFailure { error ->
                Log.e(TAG, "Backup failed", error)
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception during backup", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get the timestamp of the latest backup.
     */
    suspend fun getLatestBackupTimestamp(): String? {
        return backupRepository.getLatestBackupTimestamp()
    }
    
    /**
     * Cancel any pending backup and cleanup.
     */
    fun cancel() {
        backupJob?.cancel()
        scope.cancel()
    }
}
