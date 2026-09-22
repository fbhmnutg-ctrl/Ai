package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ChatMessage
import com.example.data.local.entity.ChatSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionById(sessionId: Long): Flow<ChatSession?>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionByIdDirect(sessionId: Long): ChatSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Update
    suspend fun updateSession(session: ChatSession)

    @Query("UPDATE chat_sessions SET updatedAt = :timestamp WHERE id = :sessionId")
    suspend fun touchSession(sessionId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllSessions()

    // Messages
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Update
    suspend fun updateMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearMessagesForSession(sessionId: Long)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Long)
}
