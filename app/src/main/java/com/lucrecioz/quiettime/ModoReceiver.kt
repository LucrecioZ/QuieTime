package com.lucrecioz.quiettime

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ModoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val modo = intent.getStringExtra("modo") ?: return

        val nm =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        if (!nm.isNotificationPolicyAccessGranted) return

        when (modo) {
            "silencioso" -> {

                nm.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                )
            }

            "som" -> {
                nm.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_ALL
                )
            }
        }
    }
}
