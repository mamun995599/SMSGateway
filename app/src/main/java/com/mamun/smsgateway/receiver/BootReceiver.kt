package com.mamaun.smsgateway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mamun.smsgateway.SmsGatewayService
import com.mamun.smsgateway.util.PreferenceManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            val prefs = PreferenceManager(context)

            if (prefs.serverEnabled) {
                SmsGatewayService.start(
                    context,
                    prefs.webSocketPort,
                    prefs.httpPort
                )
            }
        }
    }
}