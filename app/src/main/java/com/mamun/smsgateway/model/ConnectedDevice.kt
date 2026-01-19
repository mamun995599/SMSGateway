package com.mamun.smsgateway.model

data class ConnectedDevice(
    val id: String,
    val ipAddress: String,
    val connectedAt: Long = System.currentTimeMillis()
)