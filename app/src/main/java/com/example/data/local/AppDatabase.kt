package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.ModelDao
import com.example.data.local.entity.ChatMessage
import com.example.data.local.entity.ChatSession
import com.example.data.local.entity.LocalModelEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ChatSession::class,
        ChatMessage::class,
        LocalModelEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun modelDao(): ModelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pocket_ollama_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: AppDatabase) {
                val modelDao = database.modelDao()
                val chatDao = database.chatDao()

                // Curated mobile-friendly GGUF models
                val initialModels = listOf(
                    LocalModelEntity(
                        id = "hf-smollm2-135m-test",
                        name = "SmolLM2 135M (Hugging Face)",
                        filename = "SmolLM2-135M-Instruct-Q4_K_M.gguf",
                        architecture = "llama",
                        quantization = "Q4_K_M",
                        parameterCount = "135M",
                        sizeBytes = 89128960L, // ~85 MB (Lightweight test template)
                        requiredRamMb = 220,
                        contextLength = 2048,
                        isDownloaded = false,
                        downloadProgress = 0f,
                        source = "HUGGING_FACE",
                        description = "Ultra-lightweight test template from Hugging Face website (huggingface.co/HuggingFaceTB/SmolLM2-135M). Designed for fast mobile evaluation and CPU latency testing.",
                        isFavorite = true,
                        downloadUrl = "https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf"
                    ),
                    LocalModelEntity(
                        id = "llama-3.2-1b-instruct-q4",
                        name = "Llama 3.2 1B Instruct",
                        filename = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
                        architecture = "llama",
                        quantization = "Q4_K_M",
                        parameterCount = "1.23B",
                        sizeBytes = 786432000L, // ~750 MB
                        requiredRamMb = 1200,
                        contextLength = 4096,
                        isDownloaded = false,
                        downloadProgress = 0f,
                        filePath = null,
                        source = "LOCAL_GGUF",
                        description = "Meta's flagship compact model. Blazing fast inference on mobile CPUs (18-25 tok/s).",
                        isFavorite = true,
                        lastUsedTimestamp = System.currentTimeMillis(),
                        downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
                    ),
                    LocalModelEntity(
                        id = "deepseek-r1-1.5b-q4",
                        name = "DeepSeek-R1 Distill 1.5B",
                        filename = "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
                        architecture = "qwen2",
                        quantization = "Q4_K_M",
                        parameterCount = "1.54B",
                        sizeBytes = 1181116000L, // ~1.1 GB
                        requiredRamMb = 1800,
                        contextLength = 4096,
                        isDownloaded = false,
                        downloadProgress = 0f,
                        filePath = null,
                        source = "LOCAL_GGUF",
                        description = "State-of-the-art mobile reasoning model. Outputs step-by-step <think> chains.",
                        isFavorite = true,
                        lastUsedTimestamp = System.currentTimeMillis() - 100000,
                        downloadUrl = "https://huggingface.co/bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf"
                    ),
                    LocalModelEntity(
                        id = "qwen-2.5-0.5b-q4",
                        name = "Qwen 2.5 0.5B Instruct",
                        filename = "Qwen2.5-0.5B-Instruct-Q4_K_M.gguf",
                        architecture = "qwen2",
                        quantization = "Q4_K_M",
                        parameterCount = "0.49B",
                        sizeBytes = 398458880L, // ~380 MB
                        requiredRamMb = 650,
                        contextLength = 2048,
                        isDownloaded = false,
                        downloadProgress = 0f,
                        filePath = null,
                        source = "LOCAL_GGUF",
                        description = "Ultra-featherweight model. Runs with near-instant generation on entry-level devices.",
                        isFavorite = false,
                        downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
                    ),
                    LocalModelEntity(
                        id = "llama-3.2-3b-instruct-q4",
                        name = "Llama 3.2 3B Instruct",
                        filename = "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
                        architecture = "llama",
                        quantization = "Q4_K_M",
                        parameterCount = "3.21B",
                        sizeBytes = 2097152000L, // ~1.95 GB
                        requiredRamMb = 3100,
                        contextLength = 8192,
                        isDownloaded = false,
                        downloadProgress = 0f,
                        source = "LOCAL_GGUF",
                        description = "High performance coding & reasoning model. Best for devices with 6GB+ RAM."
                    ),
                    LocalModelEntity(
                        id = "smollm2-1.7b-instruct-q4",
                        name = "SmolLM2 1.7B Instruct",
                        filename = "SmolLM2-1.7B-Instruct-Q4_K_M.gguf",
                        architecture = "llama",
                        quantization = "Q4_K_M",
                        parameterCount = "1.71B",
                        sizeBytes = 1101004800L, // ~1.02 GB
                        requiredRamMb = 1600,
                        contextLength = 4096,
                        isDownloaded = false,
                        downloadProgress = 0f,
                        source = "LOCAL_GGUF",
                        description = "Hugging Face's compact specialist. Remarkable general knowledge and math accuracy."
                    ),
                    LocalModelEntity(
                        id = "phi-3.5-mini-3.8b-q4",
                        name = "Phi-3.5 Mini 3.8B",
                        filename = "Phi-3.5-mini-instruct-Q4_K_M.gguf",
                        architecture = "phi3",
                        quantization = "Q4_K_M",
                        parameterCount = "3.82B",
                        sizeBytes = 2390753280L, // ~2.22 GB
                        requiredRamMb = 3600,
                        contextLength = 4096,
                        isDownloaded = false,
                        downloadProgress = 0f,
                        source = "LOCAL_GGUF",
                        description = "Microsoft's small language model with high benchmark scores in logic and science."
                    )
                )

                modelDao.insertModels(initialModels)

                // Create initial welcoming chat session
                val initialSession = ChatSession(
                    title = "Offline AI Assistant",
                    modelId = "llama-3.2-1b-instruct-q4",
                    modelName = "Llama 3.2 1B Instruct",
                    engineType = "LOCAL_GGUF",
                    systemPrompt = "You are a private, sovereign AI running 100% locally on this device via GGUF neural weights.",
                    temperature = 0.7f,
                    topP = 0.9f,
                    contextLength = 4096
                )
                val sessionId = chatDao.insertSession(initialSession)

                // Welcome message
                chatDao.insertMessage(
                    ChatMessage(
                        sessionId = sessionId,
                        role = "assistant",
                        content = "Hello! I am running 100% locally on your device via **GGUF** quantized neural weights. No internet connection is needed for inference, and your data never leaves your device.\n\nYou can also connect to your local **Ollama** server or import custom `.gguf` models at any time!",
                        tokensCount = 52,
                        tokensPerSecond = 22.4f,
                        generationDurationMs = 2320L,
                        timeToFirstTokenMs = 110L,
                        modelTag = "Llama 3.2 1B (Q4_K_M)"
                    )
                )
            }
        }
    }
}
