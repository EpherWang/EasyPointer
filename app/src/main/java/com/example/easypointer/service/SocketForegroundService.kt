package com.example.easypointer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.easypointer.MainActivity
import com.example.easypointer.R
import com.example.easypointer.app.AppConstants
import com.example.easypointer.app.PointerController
import com.example.easypointer.model.PointerCommand
import com.example.easypointer.socket.SocketServer

/**
 * Foreground service hosting TCP socket server for better process survival.
 */
class SocketForegroundService : Service(), SocketServer.Listener {

    private var socketServer: SocketServer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(AppConstants.NOTIFICATION_ID, buildNotification("Socket server idle"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AppConstants.ACTION_START_SOCKET -> startServerIfNeeded()
            AppConstants.ACTION_STOP_SOCKET -> stopSelfSafely()
            else -> startServerIfNeeded()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopServerIfNeeded()
        super.onDestroy()
    }

    override fun onServerStarted(port: Int) {
        PointerController.updateSocketRunning(true)
        PointerController.updateStatus("Socket listening on $port")
        updateNotification("Socket listening on $port")
    }

    override fun onServerStopped() {
        PointerController.updateSocketRunning(false)
        PointerController.updateStatus("Socket stopped")
        updateNotification("Socket stopped")
    }

    override fun onLineReceived(line: String) {
        PointerController.updateLastCommand(line)
    }

    override fun onCommand(command: PointerCommand): String {
        val dispatched = PointerController.dispatchCommand(command)
        return if (dispatched || command == PointerCommand.Ping) {
            PointerController.publishOverlayState()
            "OK"
        } else {
            "ERROR ${AppConstants.ERROR_SERVICE_UNAVAILABLE}"
        }
    }

    override fun onError(message: String, t: Throwable?) {
        Log.e(AppConstants.TAG_APP, message, t)
        PointerController.updateStatus("Socket error: $message")
    }

    private fun startServerIfNeeded() {
        if (socketServer?.isRunning() == true) return
        socketServer = SocketServer(AppConstants.SOCKET_PORT, this).also { it.start() }
    }

    private fun stopServerIfNeeded() {
        socketServer?.stop()
        socketServer = null
        PointerController.updateSocketRunning(false)
    }

    private fun stopSelfSafely() {
        stopServerIfNeeded()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            AppConstants.NOTIFICATION_CHANNEL_ID,
            AppConstants.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(content: String): Notification {
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            pendingIntentFlags
        )

        return NotificationCompat.Builder(this, AppConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.presence_online)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(content)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(AppConstants.NOTIFICATION_ID, buildNotification(content))
    }
}
