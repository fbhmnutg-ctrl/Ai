package com.example.data.repository

import com.example.data.local.dao.ModelDao
import com.example.data.local.entity.LocalModelEntity
import kotlinx.coroutines.flow.Flow

class ModelRepository(private val modelDao: ModelDao) {

    fun getAllModels(): Flow<List<LocalModelEntity>> = modelDao.getAllModels()

    fun getDownloadedModels(): Flow<List<LocalModelEntity>> = modelDao.getDownloadedModels()

    fun getModelById(id: String): Flow<LocalModelEntity?> = modelDao.getModelById(id)

    suspend fun getModelByIdDirect(id: String): LocalModelEntity? = modelDao.getModelByIdDirect(id)

    suspend fun insertModel(model: LocalModelEntity) = modelDao.insertModel(model)

    suspend fun updateModel(model: LocalModelEntity) = modelDao.updateModel(model)

    suspend fun updateDownloadState(id: String, isDownloaded: Boolean, progress: Float, filePath: String?) {
        modelDao.updateDownloadState(id, isDownloaded, progress, filePath)
    }

    suspend fun markModelUsed(id: String) {
        modelDao.markModelUsed(id)
    }

    suspend fun deleteModel(id: String) = modelDao.deleteModel(id)
}
