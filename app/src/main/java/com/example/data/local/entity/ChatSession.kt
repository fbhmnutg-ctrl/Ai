package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val modelId: String,
    val modelName: String,
    val engineType: String = "LOCAL_GGUF", // "LOCAL_GGUF" or "OLLAMA"
    val systemPrompt: String = "You are a helpful, concise AI assistant running locally on mobile.",
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val contextLength: Int = 4096,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
