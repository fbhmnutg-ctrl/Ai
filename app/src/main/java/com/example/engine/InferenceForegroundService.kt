package com.example.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity

class InferenceForegroundService : Service() {

    companion object {
        private const val TAG = "InferenceFGService"
        const val CHANNEL_ID = "pocket_ollama_inference_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "ACTION_START_INFERENCE"
        const val ACTION_STOP = "ACTION_STOP_INFERENCE"

        const val EXTRA_MODEL_NAME = "EXTRA_MODEL_NAME"

        fun startService(context: Context, modelName: String) {
            try {
                val intent = Intent(context, InferenceForegroundService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_MODEL_NAME, modelName)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to start foreground service: ${t.message}")
            }
        }

        fun updateProgress(context: Context, modelName: String, tokens: Int, speed: Float, preview: String) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (notificationManager != null) {
                    val contentText = if (tokens > 0) {
                        "$tokens tokens • ${String.format("%.1f", speed)} tok/s\n${preview.takeLast(120)}"
                    } else {
                        "Computing first token..."
                    }
                    val notification = buildNotificationStatic(
                        context = context,
                        title = "PocketOllama • $modelName",
                        content = contentText
                    )
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to update notification: ${t.message}")
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, InferenceForegroundService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to stop service: ${t.message}")
            }
        }

        fun buildNotificationStatic(
            context: Context,
            title: String,
            content: String
        ): android.app.Notification {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            return NotificationCompat.Builder(context, CHANNEL_ID)
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
    }

    private var currentModelName: String = "AI Model"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_START -> {
                    currentModelName = intent.getStringExtra(EXTRA_MODEL_NAME) ?: "AI Model"
                    val notification = buildNotificationStatic(
                        context = this,
                        title = "PocketOllama • Running Inference",
                        content = "Generating response with $currentModelName in background..."
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ServiceCompat.startForeground(
                            this,
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                }
                ACTION_STOP -> {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error in onStartCommand: ${t.message}", t)
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
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
            } catch (t: Throwable) {
                Log.w(TAG, "Error creating notification channel: ${t.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
