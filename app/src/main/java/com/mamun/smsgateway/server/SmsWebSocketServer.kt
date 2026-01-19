package com.mamun.smsgateway.server

import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.mamun.smsgateway.model.ConnectedDevice
import com.mamun.smsgateway.model.SmsResponse
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SmsWebSocketServer(
    private val context: Context,
    port: Int
) : WebSocketServer(InetSocketAddress("0.0.0.0", port)) {

    companion object {
        private const val TAG = "SmsWebSocketServer"
        const val ACTION_DEVICE_CONNECTED = "com.smsgateway.DEVICE_CONNECTED"
        const val ACTION_DEVICE_DISCONNECTED = "com.smsgateway.DEVICE_DISCONNECTED"
        const val ACTION_DEVICES_UPDATE = "com.smsgateway.DEVICES_UPDATE"
        const val EXTRA_DEVICE = "device"
        const val EXTRA_DEVICES = "devices"
    }

    private val gson = Gson()
    private val connectedDevices = ConcurrentHashMap<WebSocket, ConnectedDevice>()

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        val clientIp = conn.remoteSocketAddress?.address?.hostAddress ?: "unknown"
        val device = ConnectedDevice(
            id = UUID.randomUUID().toString(),
            ipAddress = clientIp
        )
        connectedDevices[conn] = device

        Log.d(TAG, "New connection from: $clientIp")

        // Send welcome message
        val welcomeMsg = mapOf(
            "type" to "connected",
            "message" to "Connected to SMS Gateway",
            "timestamp" to System.currentTimeMillis()
        )
        conn.send(gson.toJson(welcomeMsg))

        broadcastDevicesUpdate()
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        val device = connectedDevices.remove(conn)
        Log.d(TAG, "Connection closed: ${device?.ipAddress}")
        broadcastDevicesUpdate()
    }

    override fun onMessage(conn: WebSocket, message: String) {
        Log.d(TAG, "Received message: $message")

        try {
            val request = gson.fromJson(message, Map::class.java)
            val action = request["action"] as? String

            when (action) {
                "send_sms" -> {
                    val phoneNumber = request["phone_number"] as? String
                    val smsMessage = request["message"] as? String

                    if (phoneNumber != null && smsMessage != null) {
                        val result = sendSms(phoneNumber, smsMessage)
                        conn.send(gson.toJson(result))
                    } else {
                        val error = SmsResponse(false, "Missing phone_number or message")
                        conn.send(gson.toJson(error))
                    }
                }
                "ping" -> {
                    val pong = mapOf("type" to "pong", "timestamp" to System.currentTimeMillis())
                    conn.send(gson.toJson(pong))
                }
                else -> {
                    val error = mapOf("error" to "Unknown action: $action")
                    conn.send(gson.toJson(error))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing message", e)
            val error = SmsResponse(false, "Error: ${e.message}")
            conn.send(gson.toJson(error))
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        Log.e(TAG, "WebSocket error", ex)
        conn?.let {
            connectedDevices.remove(it)
            broadcastDevicesUpdate()
        }
    }

    override fun onStart() {
        Log.d(TAG, "WebSocket server started on port $port")
        connectionLostTimeout = 100
    }

    private fun sendSms(phoneNumber: String, message: String): SmsResponse {
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)

            if (message.length > 160) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }

            SmsResponse(true, "SMS sent successfully to $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            SmsResponse(false, "Failed to send SMS: ${e.message}")
        }
    }

    private fun broadcastDevicesUpdate() {
        val intent = Intent(ACTION_DEVICES_UPDATE).apply {
            putParcelableArrayListExtra(EXTRA_DEVICES, ArrayList(connectedDevices.values.map {
                android.os.Bundle().apply {
                    putString("id", it.id)
                    putString("ip", it.ipAddress)
                    putLong("connectedAt", it.connectedAt)
                }
            }))
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun getConnectedDevices(): List<ConnectedDevice> = connectedDevices.values.toList()

    fun broadcastMessage(message: String) {
        broadcast(message)
    }

    fun getConnectionCount(): Int = connections.size
}