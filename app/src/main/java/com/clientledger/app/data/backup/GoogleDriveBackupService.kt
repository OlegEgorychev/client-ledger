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
                .setFields("id, name")
                .execute()
            
            Log.d(TAG, "Backup uploaded successfully: ${uploadedFile.id} - ${uploadedFile.name}")
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
            // Search for existing folder
            val query = "name='$folderName' and mimeType='application/vnd.google-apps.folder' and trashed=false"
            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()
            
            val folders = result.files
            if (folders != null && folders.isNotEmpty()) {
                // Folder exists, return its ID
                return@withContext folders[0].id
            }
            
            // Folder doesn't exist, create it
            val folderMetadata = com.google.api.services.drive.model.File().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
            }
            
            val folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute()
            
            Log.d(TAG, "Created backup folder: ${folder.id}")
            folder.id
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finding/creating backup folder", e)
            throw e
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
}
