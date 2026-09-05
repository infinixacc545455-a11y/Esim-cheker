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
import android.util.Log

class EsimChecker(private val context: Context) {

    companion object {
        const val ACTION_ESIM_RESULT = "com.example.esimchecker.ESIM_RESULT"
        const val REQUEST_CODE = 1001
        private const val TAG = "EsimCheckerDebug"
    }

    private val euiccManager: EuiccManager by lazy {
        context.getSystemService(Context.EUICC_SERVICE) as EuiccManager
    }

    fun isEsimSupported(): Boolean {
        val supported = euiccManager.isEnabled
        Log.d(TAG, "isEsimSupported() = $supported")
        return supported
    }

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
        Log.d(TAG, "checkAndInstall() called with code: $lpaString")

        if (!isEsimSupported()) {
            Log.e(TAG, "eSIM not supported - throwing exception")
            throw IllegalStateException("الجهاز لا يدعم eSIM أو الخدمة غير مفعّلة")
        }

        try {
            val subscription = DownloadableSubscription.forActivationCode(lpaString)
            Log.d(TAG, "DownloadableSubscription created successfully")

            val pendingIntent = buildCallbackPendingIntent()
            Log.d(TAG, "PendingIntent built, calling downloadSubscription()...")

            euiccManager.downloadSubscription(
                subscription,
                false,
                pendingIntent
            )
            Log.d(TAG, "downloadSubscription() call completed (async, waiting for broadcast)")
        } catch (e: Exception) {
            Log.e(TAG, "EXCEPTION in checkAndInstall: ${e.javaClass.simpleName} - ${e.message}", e)
            throw e
        }
    }

    fun startResolution(activity: Activity, requestCode: Int, resolutionIntent: Intent) {
        Log.d(TAG, "startResolution() called")
        try {
            val callbackPendingIntent = buildCallbackPendingIntent()
            euiccManager.startResolutionActivity(
                activity,
                requestCode,
                resolutionIntent,
                callbackPendingIntent
            )
            Log.d(TAG, "startResolutionActivity() call completed")
        } catch (e: Exception) {
            Log.e(TAG, "EXCEPTION in startResolution: ${e.javaClass.simpleName} - ${e.message}", e)
            throw e
        }
    }

    class ResultReceiver(
        private val onResult: (resultCode: Int, detailedCode: Int?, intent: Intent) -> Unit
    ) : BroadcastReceiver() {

        companion object {
            private const val LOG_TAG = "EsimCheckerDebug"
        }

        override fun onReceive(context: Context, intent: Intent) {
            Log.d(LOG_TAG, "ResultReceiver.onReceive() called, action=${intent.action}")
            if (intent.action != ACTION_ESIM_RESULT) {
                Log.d(LOG_TAG, "Ignoring - action doesn't match")
                return
            }

            val resultCode = this.resultCode
            val detailedCode = intent.getIntExtra(
                EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE, -1
            ).takeIf { it != -1 }

            Log.d(LOG_TAG, "Result received: resultCode=$resultCode, detailedCode=$detailedCode")

            intent.extras?.keySet()?.forEach { key ->
                Log.d(LOG_TAG, "Extra key: $key = ${intent.extras?.get(key)}")
            }

            onResult(resultCode, detailedCode, intent)
        }
    }

    fun registerReceiver(receiver: ResultReceiver) {
        Log.d(TAG, "registerReceiver() called")
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
            Log.w(TAG, "unregisterReceiver: receiver was not registered")
        }
    }
}
