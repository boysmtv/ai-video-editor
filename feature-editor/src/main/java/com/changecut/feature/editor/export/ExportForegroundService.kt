package com.changecut.feature.editor.export

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.changecut.feature.editor.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ExportForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "export_channel"
        private const val NOTIFICATION_ID = 101
        const val ACTION_START_EXPORT = "com.changecut.action.START_EXPORT"
    }

    private val binder = LocalBinder()

    @Inject
    lateinit var exportEngine: ExportEngine

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_EXPORT) {
            startForeground(NOTIFICATION_ID, createNotification("Preparing export..."))
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Export Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ChangeCut Export")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }

    inner class LocalBinder : Binder() {
        fun getService(): ExportForegroundService = this@ExportForegroundService
    }
}