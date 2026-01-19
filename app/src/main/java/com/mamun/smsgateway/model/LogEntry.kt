package com.mamun.smsgateway.model

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val path: String,
    val clientIp: String,
    val statusCode: Int = 200,
    val message: String = ""
) {
    enum class LogType {
        INFO, SUCCESS, ERROR, WARNING
    }

    val type: LogType
        get() = when {
            statusCode in 200..299 -> LogType.SUCCESS
            statusCode in 400..499 -> LogType.WARNING
            statusCode >= 500 -> LogType.ERROR
            else -> LogType.INFO
        }
}