package com.mamun.smsgateway.server

import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.mamun.smsgateway.model.LogEntry
import com.mamun.smsgateway.model.SmsResponse
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

class SmsHttpServer(
    private val context: Context,
    port: Int
) : NanoHTTPD("0.0.0.0", port) {

    companion object {
        private const val TAG = "SmsHttpServer"
        const val ACTION_LOG_ENTRY = "com.smsgateway.LOG_ENTRY"
        const val EXTRA_LOG_ENTRY = "log_entry"
    }

    private val gson = Gson()

    override fun serve(session: IHTTPSession): Response {
        val method = session.method.name
        val uri = session.uri
        val clientIp = session.remoteIpAddress

        Log.d(TAG, "$method $uri from $clientIp")

        val response = try {
            when {
                uri == "/" || uri == "/status" -> handleStatus(session)
                uri == "/send" || uri == "/send-sms" -> handleSendSms(session)
                uri == "/health" -> handleHealth()
                uri.startsWith("/api/") -> handleApiRequest(session)
                else -> handleNotFound(uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling request", e)
            createErrorResponse(500, "Internal Server Error: ${e.message}")
        }

        // Log the request
        val logEntry = LogEntry(
            method = method,
            path = uri,
            clientIp = clientIp,
            statusCode = response.status.requestStatus
        )
        broadcastLogEntry(logEntry)

        // Add CORS headers
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")

        return response
    }

    private fun handleStatus(session: IHTTPSession): Response {
        val status = mapOf(
            "status" to "running",
            "server" to "SMS Gateway",
            "version" to "1.0",
            "timestamp" to System.currentTimeMillis(),
            "endpoints" to listOf(
                mapOf("method" to "GET", "path" to "/status", "description" to "Server status"),
                mapOf("method" to "GET", "path" to "/health", "description" to "Health check"),
                mapOf("method" to "GET/POST", "path" to "/send", "description" to "Send SMS"),
                mapOf("method" to "POST", "path" to "/api/sms/send", "description" to "Send SMS (API)")
            )
        )
        return createJsonResponse(status)
    }

    private fun handleHealth(): Response {
        val health = mapOf(
            "status" to "healthy",
            "timestamp" to System.currentTimeMillis()
        )
        return createJsonResponse(health)
    }

    private fun handleSendSms(session: IHTTPSession): Response {
        val phoneNumber: String?
        val message: String?

        when (session.method) {
            Method.GET -> {
                phoneNumber = session.parms["phone"] ?: session.parms["to"] ?: session.parms["number"]
                message = session.parms["message"] ?: session.parms["text"] ?: session.parms["msg"]
            }
            Method.POST -> {
                val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
                val body = mutableMapOf<String, String>()

                try {
                    session.parseBody(body)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing body", e)
                }

                val postData = body["postData"]

                if (postData != null && postData.startsWith("{")) {
                    // JSON body
                    try {
                        val jsonBody = gson.fromJson(postData, Map::class.java)
                        phoneNumber = jsonBody["phone"] as? String
                            ?: jsonBody["phone_number"] as? String
                                    ?: jsonBody["to"] as? String
                        message = jsonBody["message"] as? String
                            ?: jsonBody["text"] as? String
                    } catch (e: Exception) {
                        return createErrorResponse(400, "Invalid JSON body")
                    }
                } else {
                    // Form data
                    phoneNumber = session.parms["phone"] ?: session.parms["to"] ?: session.parms["number"]
                    message = session.parms["message"] ?: session.parms["text"] ?: session.parms["msg"]
                }
            }
            Method.OPTIONS -> {
                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "")
            }
            else -> {
                return createErrorResponse(405, "Method not allowed")
            }
        }

        if (phoneNumber.isNullOrBlank()) {
            return createErrorResponse(400, "Missing phone number. Use 'phone', 'to', or 'number' parameter")
        }

        if (message.isNullOrBlank()) {
            return createErrorResponse(400, "Missing message. Use 'message', 'text', or 'msg' parameter")
        }

        val result = sendSms(phoneNumber, message)
        val status = if (result.success) Response.Status.OK else Response.Status.INTERNAL_ERROR
        return newFixedLengthResponse(status, "application/json", gson.toJson(result))
    }

    private fun handleApiRequest(session: IHTTPSession): Response {
        return when (session.uri) {
            "/api/sms/send" -> handleSendSms(session)
            "/api/status" -> handleStatus(session)
            else -> handleNotFound(session.uri)
        }
    }

    private fun handleNotFound(uri: String): Response {
        val error = mapOf(
            "error" to "Not Found",
            "path" to uri,
            "message" to "The requested endpoint does not exist"
        )
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", gson.toJson(error))
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

    private fun createJsonResponse(data: Any): Response {
        return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(data))
    }

    private fun createErrorResponse(code: Int, message: String): Response {
        val error = mapOf("error" to message, "code" to code)
        val status = when (code) {
            400 -> Response.Status.BAD_REQUEST
            404 -> Response.Status.NOT_FOUND
            405 -> Response.Status.METHOD_NOT_ALLOWED
            else -> Response.Status.INTERNAL_ERROR
        }
        return newFixedLengthResponse(status, "application/json", gson.toJson(error))
    }

    private fun broadcastLogEntry(logEntry: LogEntry) {
        val intent = Intent(ACTION_LOG_ENTRY).apply {
            putExtra("timestamp", logEntry.timestamp)
            putExtra("method", logEntry.method)
            putExtra("path", logEntry.path)
            putExtra("clientIp", logEntry.clientIp)
            putExtra("statusCode", logEntry.statusCode)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}