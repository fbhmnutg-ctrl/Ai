package com.example.engine

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Manages automated storage of GGUF models into organized folders
 * within the system Download directory:
 *   Downloads/<Model_Name>/<model_filename>.gguf
 *
 * Ensures both downloaded models and imported models are neatly organized
 * and accessible by both native inference engines and the user's file manager.
 */
object ModelStorageManager {

    private const val TAG = "ModelStorageManager"

    /**
     * Cleans a model name to be safe for filesystem folder creation.
     */
    fun sanitizeFolderName(modelName: String): String {
        val sanitized = modelName.trim()
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        return sanitized.ifEmpty { "Model_${System.currentTimeMillis() % 10000}" }
    }

    /**
     * Gets the system Download directory, with reliable fallback to app-specific external download directory.
     */
    fun getDownloadsDirectory(context: Context): File {
        val publicDownload = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (publicDownload != null && (publicDownload.exists() || publicDownload.mkdirs())) {
            return publicDownload
        }

        val externalDownload = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (externalDownload != null && (externalDownload.exists() || externalDownload.mkdirs())) {
            return externalDownload
        }

        // Internal app fallback
        val internalDownload = File(context.filesDir, "downloads").apply { mkdirs() }
        return internalDownload
    }

    /**
     * Creates and returns a dedicated folder inside the Download directory named after the model.
     * Example: /storage/emulated/0/Download/Llama_3.2_1B/
     */
    fun getModelFolderInDownloads(context: Context, modelName: String): File {
        val downloadsDir = getDownloadsDirectory(context)
        val folderName = sanitizeFolderName(modelName)
        val modelFolder = File(downloadsDir, folderName)
        if (!modelFolder.exists()) {
            val created = modelFolder.mkdirs()
            Log.d(TAG, "Created model folder: ${modelFolder.absolutePath} (success: $created)")
        }
        return modelFolder
    }

    /**
     * Resolves the target file destination inside the model's download folder.
     */
    fun getTargetModelFile(context: Context, modelName: String, fileName: String): File {
        val modelFolder = getModelFolderInDownloads(context, modelName)
        val safeFileName = fileName.ifBlank { "${sanitizeFolderName(modelName)}.gguf" }
        return File(modelFolder, safeFileName)
    }

    /**
     * Copies an imported model (from Uri) into the model's dedicated folder inside Downloads,
     * as well as returning the internal accessible File for native llama.cpp execution.
     */
    fun saveImportedModel(
        context: Context,
        modelName: String,
        fileName: String,
        uri: Uri
    ): Pair<File, File?> {
        val cleanName = modelName.ifBlank { fileName.substringBeforeLast('.') }
        val targetDownloadFile = getTargetModelFile(context, cleanName, fileName)

        // Internal app file for direct Native memory-mapping
        val internalModelsDir = File(context.filesDir, "models").apply { mkdirs() }
        val internalModelFolder = File(internalModelsDir, sanitizeFolderName(cleanName)).apply { mkdirs() }
        val internalFile = File(internalModelFolder, fileName)

        try {
            // Copy to internal accessible storage
            context.contentResolver.openInputStream(uri)?.use { inStream ->
                FileOutputStream(internalFile).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }

            // Also copy to Download folder inside model subfolder
            if (internalFile.exists() && internalFile.length() > 0) {
                try {
                    FileInputStream(internalFile).use { inStream ->
                        FileOutputStream(targetDownloadFile).use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    Log.i(TAG, "Imported model saved to Downloads: ${targetDownloadFile.absolutePath}")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not write duplicate to public download folder: ${e.message}")
                }
            }

            return Pair(internalFile, if (targetDownloadFile.exists()) targetDownloadFile else null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import model from uri: ${e.message}", e)
            return Pair(internalFile, null)
        }
    }

    /**
     * Saves a downloaded model file directly into the model's dedicated folder inside Downloads,
     * and keeps an internal reference for native JNI loading.
     */
    fun organizeDownloadedModel(
        context: Context,
        modelName: String,
        fileName: String,
        downloadedTempFile: File
    ): File {
        val downloadTarget = getTargetModelFile(context, modelName, fileName)
        
        // Also ensure an internal copy exists for native ABI
        val internalModelsDir = File(context.filesDir, "models").apply { mkdirs() }
        val internalModelFolder = File(internalModelsDir, sanitizeFolderName(modelName)).apply { mkdirs() }
        val internalFile = File(internalModelFolder, fileName)

        try {
            // First move/copy to Download folder
            if (downloadedTempFile.exists()) {
                if (downloadTarget.exists()) downloadTarget.delete()
                
                // Copy to internal storage
                FileInputStream(downloadedTempFile).use { inStream ->
                    FileOutputStream(internalFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }

                // Copy to downloads folder
                FileInputStream(downloadedTempFile).use { inStream ->
                    FileOutputStream(downloadTarget).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }

                // Clean up temp file
                downloadedTempFile.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error organizing downloaded model: ${e.message}")
        }

        // Return internal file if valid, otherwise downloadTarget
        return if (internalFile.exists() && internalFile.length() > 0) internalFile else downloadTarget
    }
}
