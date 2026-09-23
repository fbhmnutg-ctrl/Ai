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

                // Curated mobile-friendly GGUF models across diverse quantization formats
                val initialModels = listOf(
                    LocalModelEntity(
                        id = "qwen-2.5-0.5b-q6-k-p",
                        name = "Qwen 2.5 0.5B (Q6_K_P Precision)",
                        filename = "Qwen2.5-0.5B-Instruct-Q6_K_P.gguf",
                        architecture = "qwen2",
                        quantization = "Q6_K_P",
                        parameterCount = "0.49B",
                        sizeBytes = 492830720L, // ~470 MB
                        requiredRamMb = 750,
                        contextLength = 4096,
                        isDownloaded = false,
                        downloadProgress = 0f,
                        filePath = null,
                        source = "LOCAL_GGUF",
                        description = "High-precision 6-bit K-quant (Q6_K_P). Delivers enhanced mathematical fidelity and code generation with virtually zero loss.",
                        isFavorite = true,
                        downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q6_k.gguf"
                    ),
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
                        id = "llama-3.2-1b-instruct-q5-k-m",
                        name = "Llama 3.2 1B (Q5_K_M Balanced)",
                        filename = "Llama-3.2-1B-Instruct-Q5_K_M.gguf",
                        architecture = "llama",
                        quantization = "Q5_K_M",
                        parameterCount = "1.23B",
                        sizeBytes = 891289600L, // ~850 MB
                        requiredRamMb = 1350,
                        contextLength = 4096,
                        isDownloaded = false,
                        downloadProgress = 0f,
                        source = "LOCAL_GGUF",
                        description = "5-bit medium K-quant (Q5_K_M). Offers higher reasoning coherence and vocabulary nuance than standard 4-bit weights.",
                        isFavorite = true,
                        downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q5_K_M.gguf"
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
                        id = "gemma-2-2b-instruct-q4-k-m",
                        name = "Gemma 2 2B (Google DeepMind)",
                        filename = "gemma-2-2b-it-Q4_K_M.gguf",
                        architecture = "gemma",
                        quantization = "Q4_K_M",
                        parameterCount = "2.6B",
                        sizeBytes = 1630000000L, // ~1.55 GB
                        requiredRamMb = 2300,
                        contextLength = 4096,
                        isDownloaded = false,
                        downloadProgress = 0f,
                        filePath = null,
                        source = "LOCAL_GGUF",
                        description = "Google DeepMind's Gemma 2. Advanced multi-head attention and alternating sliding-window attention for state-of-the-art 2B reasoning.",
                        isFavorite = true,
                        lastUsedTimestamp = System.currentTimeMillis() - 50000,
                        downloadUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf"
                    ),
                    LocalModelEntity(
                        id = "deepseek-r1-1.5b-iq4-nl",
                        name = "DeepSeek-R1 1.5B (IQ4_NL I-Matrix)",
                        filename = "DeepSeek-R1-Distill-Qwen-1.5B-IQ4_NL.gguf",
                        architecture = "qwen2",
                        quantization = "IQ4_NL",
                        parameterCount = "1.54B",
                        sizeBytes = 1120000000L, // ~1.05 GB
                        requiredRamMb = 1750,
                        contextLength = 4096,
                        isDownloaded = false,
                        downloadProgress = 0f,
                        filePath = null,
                        source = "LOCAL_GGUF",
                        description = "Importance Matrix Non-Linear 4-bit (IQ4_NL). Outperforms traditional Q4_K_M on complex reasoning and step-by-step logic chains.",
                        isFavorite = true,
                        lastUsedTimestamp = System.currentTimeMillis() - 100000,
                        downloadUrl = "https://huggingface.co/bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-IQ4_NL.gguf"
                    ),
                    LocalModelEntity(
                        id = "smollm2-360m-instruct-q8",
                        name = "SmolLM2 360M (Q8_0 Studio)",
                        filename = "SmolLM2-360M-Instruct-Q8_0.gguf",
                        architecture = "llama",
                        quantization = "Q8_0",
                        parameterCount = "360M",
                        sizeBytes = 388000000L, // ~370 MB
                        requiredRamMb = 600,
                        contextLength = 2048,
                        isDownloaded = false,
                        downloadProgress = 0f,
                        source = "LOCAL_GGUF",
                        description = "Full 8-bit studio-grade quantization (Q8_0). Near 100% full floating-point accuracy with ultra-compact RAM footprint.",
                        isFavorite = false,
                        downloadUrl = "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q8_0.gguf"
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
                    )
                )

                modelDao.insertModels(initialModels)
            }
        }
    }
}
