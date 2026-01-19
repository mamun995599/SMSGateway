package com.mamun.smsgateway

import android.app.Application
import com.mamun.smsgateway.util.PreferenceManager

class SmsGatewayApplication : Application() {

    lateinit var preferenceManager: PreferenceManager
        private set

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
    }
}