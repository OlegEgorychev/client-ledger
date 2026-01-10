package com.clientledger.app.data.backup

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.gms.tasks.Tasks
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

private const val TAG = "GoogleDriveBackupService"

/**
 * Service for uploading backups to Google Drive.
 * Requires user to sign in with Google account first.
 */
class GoogleDriveBackupService(private val context: Context) {
    
    private val driveScopes = listOf(DriveScopes.DRIVE_FILE)
    
    /**
     * Get Google Sign-In client for Drive access.
     * 
     * Note: Google Play Services automatically detects the Android OAuth Client ID based on:
     * - App package name (from AndroidManifest.xml)
     * - Signing certificate SHA-1 fingerprint
     * 
     * These must match the OAuth Client ID configuration in Google Cloud Console.
     * No explicit Client ID configuration in code is needed for Android apps.
     * The string resource "google_oauth_client_id" is optional and used only for validation/display.
     */
    fun getGoogleSignInClient(): GoogleSignInClient? {
        try {
            // Google Play Services will automatically use the Android OAuth Client ID
            // that matches the app's package name and SHA-1 in Google Cloud Console
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                .build()
            
            return GoogleSignIn.getClient(context, signInOptions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Google Sign-In client. Make sure OAuth Client ID is configured in Google Cloud Console with matching package name and SHA-1", e)
            return null
        }
    }
    
    /**
     * Check if user is already signed in.
     */
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && account.grantedScopes?.contains(Scope(DriveScopes.DRIVE_FILE)) == true
    }
    
    /**
     * Get current signed-in account.
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    /**
     * Upload backup file to Google Drive.
     * File will be saved in "ClientLedger Backups" folder.
     * 
     * @param backupFile Local backup file to upload
     * @param backupFileName Name for the file in Drive (e.g., "backup_001_2024-01-15_14-30.json")
     * @return Result with Drive file ID on success
     */
    suspend fun uploadBackupToDrive(
        backupFile: File,
        backupFileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(Exception("Not signed in to Google"))
            
            val hasDriveScope = account.grantedScopes?.contains(Scope(DriveScopes.DRIVE_FILE)) == true
            if (!hasDriveScope) {
                return@withContext Result.failure(Exception("Drive scope not granted"))
            }
            
            // Create Drive service using GoogleAccountCredential
            val credential = GoogleAccountCredential.usingOAuth2(context, driveScopes)
            credential.selectedAccount = account.account
            
            val driveService = Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("Client Ledger")
                .build()
            
            // Find or create "ClientLedger Backups" folder
            val folderId = findOrCreateBackupFolder(driveService)
            
            // Upload file to Drive
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = backupFileName
                parents = listOf(folderId)
            }
            
            val fileContent = FileInputStream(backupFile)
            val mediaContent = com.google.api.client.http.FileContent(
                "application/json",
                backupFile
            )
            
            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name, parents, createdTime")
                .execute()
            
            Log.d(TAG, "Backup uploaded successfully: ${uploadedFile.id} - ${uploadedFile.name}")
            Log.d(TAG, "Backup file parents: ${uploadedFile.parents}")
            Log.d(TAG, "Backup file createdTime: ${uploadedFile.createdTime?.value}")
            Log.d(TAG, "Backup uploaded to folder ID: $folderId")
            
            // Verify file was actually uploaded by getting it back
            try {
                val verifyFile = driveService.files().get(uploadedFile.id)
                    .setFields("id, name, parents, createdTime, modifiedTime")
                    .execute()
                Log.d(TAG, "Verification: File exists with parents: ${verifyFile.parents}")
            } catch (e: Exception) {
                Log.e(TAG, "Warning: Could not verify uploaded file", e)
            }
            
            Result.success(uploadedFile.id)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload backup to Drive", e)
            Result.failure(e)
        }
    }
    
    /**
     * Find or create "ClientLedger Backups" folder in Drive.
     */
    private suspend fun findOrCreateBackupFolder(driveService: Drive): String = withContext(Dispatchers.IO) {
        val folderName = "ClientLedger Backups"
        
        try {
            // Search for existing folder - use escaped quotes for name
            val query = "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            Log.d(TAG, "Searching for folder with query: $query")
            
            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, parents)")
                .execute()
            
            val folders = result.files
            if (folders != null && folders.isNotEmpty()) {
                val folder = folders[0]
                Log.d(TAG, "Found existing backup folder: ${folder.id} - ${folder.name} (parents: ${folder.parents})")
                return@withContext folder.id
            }
            
            Log.d(TAG, "Backup folder not found, creating new one...")
            
            // Folder doesn't exist, create it
            val folderMetadata = com.google.api.services.drive.model.File().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
            }
            
            val folder = driveService.files().create(folderMetadata)
                .setFields("id, name, parents")
                .execute()
            
            Log.d(TAG, "Created backup folder: ${folder.id} - ${folder.name} (parents: ${folder.parents})")
            folder.id
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finding/creating backup folder", e)
            Log.e(TAG, "Exception details: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    /**
     * Get list of backup files from Google Drive.
     * Returns list of file IDs sorted by creation time (newest first).
     */
    suspend fun listBackupsFromDrive(): Result<List<BackupFileInfo>> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(Exception("Not signed in to Google"))
            
            val hasDriveScope = account.grantedScopes?.contains(Scope(DriveScopes.DRIVE_FILE)) == true
            if (!hasDriveScope) {
                return@withContext Result.failure(Exception("Drive scope not granted"))
            }
            
            // Create Drive service
            val credential = GoogleAccountCredential.usingOAuth2(context, driveScopes)
            credential.selectedAccount = account.account
            
            val driveService = Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("Client Ledger")
                .build()
            
            // Find backup folder
            val folderId = try {
                findOrCreateBackupFolder(driveService)
            } catch (e: Exception) {
                Log.e(TAG, "Error finding backup folder", e)
                return@withContext Result.failure(Exception("Failed to find backup folder: ${e.message}"))
            }
            
            Log.d(TAG, "Searching for backups in folder ID: $folderId")
            
            // First, try to list ALL files in the folder to see what's there
            val simpleQuery = "'$folderId' in parents and trashed = false"
            Log.d(TAG, "Step 1: Simple query for all files in folder: $simpleQuery")
            
            val allFilesResult = try {
                driveService.files().list()
                    .setQ(simpleQuery)
                    .setFields("files(id, name, createdTime, modifiedTime, parents, mimeType)")
                    .setPageSize(100)
                    .execute()
            } catch (e: Exception) {
                Log.e(TAG, "Error listing all files in folder", e)
                return@withContext Result.failure(Exception("Failed to list files in folder: ${e.message}"))
            }
            
            val allFiles = allFilesResult.files ?: emptyList()
            Log.d(TAG, "Total files in folder: ${allFiles.size}")
            allFiles.forEach { file ->
                Log.d(TAG, "  - File: ${file.name} (ID: ${file.id}, parents: ${file.parents}, mimeType: ${file.mimeType})")
            }
            
            // Now filter backup files
            val backupFiles = allFiles
                .filter { file -> 
                    file.name.startsWith("backup_", ignoreCase = true) &&
                    file.name.endsWith(".json", ignoreCase = true)
                }
                .sortedByDescending { it.createdTime?.value ?: 0L }
                .map { file ->
                    Log.d(TAG, "Found backup file: ${file.name} (ID: ${file.id}, created: ${file.createdTime?.value})")
                    BackupFileInfo(
                        fileId = file.id,
                        fileName = file.name,
                        createdTime = file.createdTime?.value ?: 0L,
                        modifiedTime = file.modifiedTime?.value ?: 0L
                    )
                }
            
            Log.d(TAG, "Found ${backupFiles.size} backup files after filtering")
            
            if (backupFiles.isEmpty()) {
                // Try alternative search - search all files with "backup_" in name
                Log.d(TAG, "No backups found in folder, trying alternative search...")
                try {
                    val altQuery = "name contains 'backup_' and trashed=false"
                    val altResult = driveService.files().list()
                        .setQ(altQuery)
                        .setOrderBy("createdTime desc")
                        .setFields("files(id, name, createdTime, modifiedTime, parents, mimeType)")
                        .setPageSize(50)
                        .execute()
                    
                    val altFiles = altResult.files ?: emptyList()
                    Log.d(TAG, "Alternative search found ${altFiles.size} files with 'backup_' in name")
                    
                    // Filter JSON files and check if they're in the backup folder
                    val altBackupFiles = altFiles
                        .filter { file -> 
                            file.name.endsWith(".json", ignoreCase = true) &&
                            file.name.contains("backup_")
                        }
                        .map { file ->
                            Log.d(TAG, "Alternative file: ${file.name} (ID: ${file.id}, parents: ${file.parents})")
                            BackupFileInfo(
                                fileId = file.id,
                                fileName = file.name,
                                createdTime = file.createdTime?.value ?: 0L,
                                modifiedTime = file.modifiedTime?.value ?: 0L
                            )
                        }
                    
                    if (altBackupFiles.isNotEmpty()) {
                        Log.d(TAG, "Found ${altBackupFiles.size} backup files via alternative search")
                        return@withContext Result.success(altBackupFiles)
                    }
                    
                    // Log all found files for debugging
                    altFiles.take(10).forEach { file ->
                        Log.d(TAG, "Found file (not backup): ${file.name} (parents: ${file.parents})")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in alternative search", e)
                }
                
                return@withContext Result.failure(Exception("No backup files found in Google Drive. Make sure backups are being uploaded successfully. Check Logcat for details."))
            }
            
            Result.success(backupFiles)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list backups from Drive", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download the latest backup file from Google Drive.
     * @return Result with local file path
     */
    suspend fun downloadLatestBackupFromDrive(): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Get list of backups
            val backupsResult = listBackupsFromDrive()
            val backups = backupsResult.getOrNull() ?: return@withContext Result.failure(Exception("No backups found in Drive"))
            
            if (backups.isEmpty()) {
                return@withContext Result.failure(Exception("No backup files found in Google Drive"))
            }
            
            // Get the latest backup (first in list, sorted by creation time desc)
            val latestBackup = backups[0]
            
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(Exception("Not signed in to Google"))
            
            // Create Drive service
            val credential = GoogleAccountCredential.usingOAuth2(context, driveScopes)
            credential.selectedAccount = account.account
            
            val driveService = Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("Client Ledger")
                .build()
            
            // Download file
            val outputDir = File(context.cacheDir, "drive_backups").apply {
                if (!exists()) mkdirs()
            }
            val outputFile = File(outputDir, latestBackup.fileName)
            
            val fileContent = driveService.files().get(latestBackup.fileId)
                .executeMediaAsInputStream()
            
            FileOutputStream(outputFile).use { output ->
                fileContent.use { input ->
                    input.copyTo(output)
                }
            }
            
            Log.d(TAG, "Downloaded backup from Drive: ${outputFile.absolutePath}")
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download backup from Drive", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sign out from Google account.
     */
    suspend fun signOut() = withContext(Dispatchers.Main) {
        val client = getGoogleSignInClient() ?: return@withContext
        try {
            Tasks.await(client.signOut())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sign out", e)
        }
    }
    
    /**
     * Data class for backup file info from Google Drive.
     */
    data class BackupFileInfo(
        val fileId: String,
        val fileName: String,
        val createdTime: Long,
        val modifiedTime: Long
    )
}
