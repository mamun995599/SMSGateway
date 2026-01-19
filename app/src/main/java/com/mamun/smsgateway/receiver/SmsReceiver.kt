package com.mamun.smsgateway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (sms in messages) {
                val sender = sms.displayOriginatingAddress
                val body = sms.messageBody

                Log.d(TAG, "SMS received from $sender: $body")

                // You can broadcast this to connected WebSocket clients
                // or store it for retrieval via HTTP
            }
        }
    }
}