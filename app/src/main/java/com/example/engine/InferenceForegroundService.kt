package com.example.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class InferenceForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "pocket_ollama_inference_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "ACTION_START_INFERENCE"
        const val ACTION_UPDATE = "ACTION_UPDATE_INFERENCE"
        const val ACTION_STOP = "ACTION_STOP_INFERENCE"

        const val EXTRA_MODEL_NAME = "EXTRA_MODEL_NAME"
        const val EXTRA_TOKENS = "EXTRA_TOKENS"
        const val EXTRA_SPEED = "EXTRA_SPEED"
        const val EXTRA_PREVIEW = "EXTRA_PREVIEW"

        fun startService(context: Context, modelName: String) {
            val intent = Intent(context, InferenceForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MODEL_NAME, modelName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateProgress(context: Context, modelName: String, tokens: Int, speed: Float, preview: String) {
            val intent = Intent(context, InferenceForegroundService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_MODEL_NAME, modelName)
                putExtra(EXTRA_TOKENS, tokens)
                putExtra(EXTRA_SPEED, speed)
                putExtra(EXTRA_PREVIEW, preview.takeLast(100))
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, InferenceForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var currentModelName: String = "AI Model"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentModelName = intent.getStringExtra(EXTRA_MODEL_NAME) ?: "AI Model"
                val notification = buildNotification(
                    title = "PocketOllama • Running Inference",
                    content = "Generating response with $currentModelName in background...",
                    tokens = 0,
                    speed = 0f
                )
                startForeground(NOTIFICATION_ID, notification)
            }
            ACTION_UPDATE -> {
                val modelName = intent.getStringExtra(EXTRA_MODEL_NAME) ?: currentModelName
                val tokens = intent.getIntExtra(EXTRA_TOKENS, 0)
                val speed = intent.getFloatExtra(EXTRA_SPEED, 0f)
                val preview = intent.getStringExtra(EXTRA_PREVIEW) ?: ""

                val notification = buildNotification(
                    title = "Generating Response • $modelName",
                    content = if (tokens > 0) "$tokens tokens • ${String.format("%.1f", speed)} tok/s\n$preview" else "Computing first token...",
                    tokens = tokens,
                    speed = speed
                )
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
            ACTION_STOP -> {
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "On-Device AI Inference",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of background local LLM inference"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        title: String,
        content: String,
        tokens: Int,
        speed: Float
    ): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
