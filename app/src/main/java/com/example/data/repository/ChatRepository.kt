package com.example.data.repository

import com.example.data.local.dao.ChatDao
import com.example.data.local.entity.ChatMessage
import com.example.data.local.entity.ChatSession
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {

    fun getAllSessions(): Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getSessionById(sessionId: Long): Flow<ChatSession?> = chatDao.getSessionById(sessionId)

    suspend fun getSessionByIdDirect(sessionId: Long): ChatSession? = chatDao.getSessionByIdDirect(sessionId)

    suspend fun createSession(
        title: String,
        modelId: String,
        modelName: String,
        engineType: String = "LOCAL_GGUF",
        systemPrompt: String = "You are a helpful local AI assistant.",
        temperature: Float = 0.7f,
        topP: Float = 0.9f
    ): Long {
        val session = ChatSession(
            title = title,
            modelId = modelId,
            modelName = modelName,
            engineType = engineType,
            systemPrompt = systemPrompt,
            temperature = temperature,
            topP = topP
        )
        return chatDao.insertSession(session)
    }

    suspend fun updateSession(session: ChatSession) {
        chatDao.updateSession(session)
    }

    suspend fun touchSession(sessionId: Long) {
        chatDao.touchSession(sessionId)
    }

    suspend fun deleteSession(sessionId: Long) {
        chatDao.deleteSession(sessionId)
    }

    fun getMessages(sessionId: Long): Flow<List<ChatMessage>> = chatDao.getMessagesForSession(sessionId)

    suspend fun insertMessage(message: ChatMessage): Long = chatDao.insertMessage(message)

    suspend fun updateMessage(message: ChatMessage) = chatDao.updateMessage(message)

    suspend fun clearMessages(sessionId: Long) = chatDao.clearMessagesForSession(sessionId)

    suspend fun deleteMessage(messageId: Long) = chatDao.deleteMessage(messageId)
}
