package com.example.esimchecker

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.euicc.DownloadableSubscription
import android.telephony.euicc.EuiccManager

class EsimChecker(private val context: Context) {

    companion object {
        const val ACTION_ESIM_RESULT = "com.example.esimchecker.ESIM_RESULT"
        const val REQUEST_CODE = 1001
    }

    private val euiccManager: EuiccManager by lazy {
        context.getSystemService(Context.EUICC_SERVICE) as EuiccManager
    }

    fun isEsimSupported(): Boolean = euiccManager.isEnabled

    private fun buildCallbackPendingIntent(): PendingIntent {
        val intent = Intent(ACTION_ESIM_RESULT).apply {
            setPackage(context.packageName)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    fun checkAndInstall(lpaString: String) {
        if (!isEsimSupported()) {
            throw IllegalStateException("الجهاز لا يدعم eSIM أو الخدمة غير مفعّلة")
        }

        val subscription = DownloadableSubscription.forActivationCode(lpaString)
        val pendingIntent = buildCallbackPendingIntent()

        euiccManager.downloadSubscription(
            subscription,
            false,
            pendingIntent
        )
    }

    fun startResolution(activity: Activity, requestCode: Int, resolutionIntent: Intent) {
        val callbackPendingIntent = buildCallbackPendingIntent()
        euiccManager.startResolutionActivity(
            activity,
            requestCode,
            resolutionIntent,
            callbackPendingIntent
        )
    }

    class ResultReceiver(
        private val onResult: (resultCode: Int, detailedCode: Int?, intent: Intent) -> Unit
    ) : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_ESIM_RESULT) return

            val resultCode = this.resultCode
            val detailedCode = intent.getIntExtra(
                EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE, -1
            ).takeIf { it != -1 }

            onResult(resultCode, detailedCode, intent)
        }
    }

    fun registerReceiver(receiver: ResultReceiver) {
        val filter = IntentFilter(ACTION_ESIM_RESULT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    fun unregisterReceiver(receiver: ResultReceiver) {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
        }
    }
}
