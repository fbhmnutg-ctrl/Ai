package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.LocalModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM local_models ORDER BY isDownloaded DESC, lastUsedTimestamp DESC, name ASC")
    fun getAllModels(): Flow<List<LocalModelEntity>>

    @Query("SELECT * FROM local_models WHERE isDownloaded = 1 ORDER BY lastUsedTimestamp DESC")
    fun getDownloadedModels(): Flow<List<LocalModelEntity>>

    @Query("SELECT * FROM local_models WHERE source = 'UPLOADED' OR source = 'IMPORTED' OR id LIKE 'imported-%' ORDER BY lastUsedTimestamp DESC, name ASC")
    fun getUploadedModels(): Flow<List<LocalModelEntity>>

    @Query("SELECT * FROM local_models WHERE id = :id LIMIT 1")
    fun getModelById(id: String): Flow<LocalModelEntity?>

    @Query("SELECT * FROM local_models WHERE id = :id LIMIT 1")
    suspend fun getModelByIdDirect(id: String): LocalModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: LocalModelEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertModels(models: List<LocalModelEntity>)

    @Update
    suspend fun updateModel(model: LocalModelEntity)

    @Query("UPDATE local_models SET isDownloaded = :isDownloaded, downloadProgress = :progress, filePath = :filePath WHERE id = :id")
    suspend fun updateDownloadState(id: String, isDownloaded: Boolean, progress: Float, filePath: String?)

    @Query("UPDATE local_models SET lastUsedTimestamp = :timestamp WHERE id = :id")
    suspend fun markModelUsed(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM local_models WHERE id = :id")
    suspend fun deleteModel(id: String)
}
