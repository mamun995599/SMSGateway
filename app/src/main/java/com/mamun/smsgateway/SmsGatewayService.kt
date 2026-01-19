package com.mamun.smsgateway

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mamun.smsgateway.server.SmsHttpServer
import com.mamun.smsgateway.server.SmsWebSocketServer
import com.mamun.smsgateway.util.NetworkUtils
import com.mamun.smsgateway.util.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException

class SmsGatewayService : Service() {

    companion object {
        private const val TAG = "SmsGatewayService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sms_gateway_channel"

        const val ACTION_START = "com.smsgateway.START"
        const val ACTION_STOP = "com.smsgateway.STOP"
        const val EXTRA_WS_PORT = "ws_port"
        const val EXTRA_HTTP_PORT = "http_port"

        fun start(context: Context, wsPort: Int, httpPort: Int) {
            val intent = Intent(context, SmsGatewayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_WS_PORT, wsPort)
                putExtra(EXTRA_HTTP_PORT, httpPort)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SmsGatewayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var webSocketServer: SmsWebSocketServer? = null
    private var httpServer: SmsHttpServer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var wsPort = PreferenceManager.DEFAULT_WS_PORT
    private var httpPort = PreferenceManager.DEFAULT_HTTP_PORT

    var isRunning = false
        private set

    inner class LocalBinder : Binder() {
        fun getService(): SmsGatewayService = this@SmsGatewayService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                wsPort = intent.getIntExtra(EXTRA_WS_PORT, PreferenceManager.DEFAULT_WS_PORT)
                httpPort = intent.getIntExtra(EXTRA_HTTP_PORT, PreferenceManager.DEFAULT_HTTP_PORT)
                startServers()
            }
            ACTION_STOP -> {
                stopServers()
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startServers() {
        if (isRunning) return

        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch {
            try {
                // Start WebSocket Server
                webSocketServer = SmsWebSocketServer(applicationContext, wsPort).apply {
                    isReuseAddr = true
                    start()
                }
                Log.d(TAG, "WebSocket server started on port $wsPort")

                // Start HTTP Server
                httpServer = SmsHttpServer(applicationContext, httpPort).apply {
                    start()
                }
                Log.d(TAG, "HTTP server started on port $httpPort")

                isRunning = true
                updateNotification()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start servers", e)
                stopServers()
            }
        }
    }

    private fun stopServers() {
        isRunning = false

        try {
            webSocketServer?.stop(1000)
            webSocketServer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping WebSocket server", e)
        }

        try {
            httpServer?.stop()
            httpServer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping HTTP server", e)
        }

        releaseWakeLock()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SmsGatewayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ips = NetworkUtils.getAllIpAddresses().joinToString(", ")

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Gateway Running")
            .setContentText("WS: $wsPort | HTTP: $httpPort")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("WebSocket Port: $wsPort\nHTTP Port: $httpPort\nIPs: $ips"))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SmsGateway::WakeLock"
        ).apply {
            acquire(10*60*1000L /*10 minutes*/)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    fun getWebSocketServer(): SmsWebSocketServer? = webSocketServer
    fun getHttpServer(): SmsHttpServer? = httpServer
    fun getWsPort(): Int = wsPort
    fun getHttpPort(): Int = httpPort

    override fun onDestroy() {
        stopServers()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Service continues running even when app is removed from recents
        super.onTaskRemoved(rootIntent)
    }
}