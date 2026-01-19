package com.mamun.smsgateway.model

data class SmsRequest(
    val phoneNumber: String,
    val message: String
)

data class SmsResponse(
    val success: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)