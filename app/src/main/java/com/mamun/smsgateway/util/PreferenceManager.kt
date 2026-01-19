package com.mamun.smsgateway.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {

    companion object {
        private const val PREF_NAME = "sms_gateway_prefs"
        private const val KEY_WS_PORT = "websocket_port"
        private const val KEY_HTTP_PORT = "http_port"
        private const val KEY_SERVER_ENABLED = "server_enabled"

        const val DEFAULT_WS_PORT = 8080
        const val DEFAULT_HTTP_PORT = 8070
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var webSocketPort: Int
        get() = prefs.getInt(KEY_WS_PORT, DEFAULT_WS_PORT)
        set(value) = prefs.edit().putInt(KEY_WS_PORT, value).apply()

    var httpPort: Int
        get() = prefs.getInt(KEY_HTTP_PORT, DEFAULT_HTTP_PORT)
        set(value) = prefs.edit().putInt(KEY_HTTP_PORT, value).apply()

    var serverEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SERVER_ENABLED, value).apply()
}