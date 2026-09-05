package com.example.esimchecker

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.euicc.DownloadableSubscription
import android.telephony.euicc.EuiccManager

/**
 * مثال أساسي لاستخدام EuiccManager لمحاولة تنزيل/تثبيت ملف eSIM
 * انطلاقاً من كود LPA بصيغة:
 *   LPA:1$smdp.example.com$ACTIVATION_CODE$
 *
 * ملاحظات مهمة:
 * 1) يتطلب جهاز أندرويد حقيقي يدعم eSIM (EuiccManager.isEnabled() == true).
 * 2) بدون Carrier Privileges، النظام سيعرض واجهة تأكيد رسمية للمستخدم.
 * 3) بعض الأكواد "استخدام واحد" (one-time) — محاولة التثبيت قد تحرق الكود
 *    حتى إن أُلغيت العملية لاحقاً، حسب سياسة SM-DP+.
 */
class EsimChecker(private val context: Context) {

    companion object {
        const val ACTION_ESIM_RESULT = "com.example.esimchecker.ESIM_RESULT"
        const val REQUEST_CODE = 1001
        const val EXTRA_RESULT_CODE = "android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_RESULT"
    }

    private val euiccManager: EuiccManager by lazy {
        context.getSystemService(Context.EUICC_SERVICE) as EuiccManager
    }

    fun isEsimSupported(): Boolean = euiccManager.isEnabled

    fun checkAndInstall(lpaString: String) {
        if (!isEsimSupported()) {
            throw IllegalStateException("الجهاز لا يدعم eSIM أو الخدمة غير مفعّلة")
        }

        val subscription = DownloadableSubscription.forActivationCode(lpaString)

        val intent = Intent(ACTION_ESIM_RESULT).apply {
            setPackage(context.packageName)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }

        val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)

        euiccManager.downloadSubscription(
            subscription,
            /* switchAfterDownload = */ false,
            pendingIntent
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
            // الـ receiver غير مسجل، تجاهل
        }
    }
}

/**
 * دليل تفسير أهم رموز النتائج (EuiccManager):
 *
 * EMBEDDED_SUBSCRIPTION_RESULT_OK (0)
 *   -> نجحت العملية، الملف الشخصي صالح وتم تنزيله فعلياً.
 *
 * EMBEDDED_SUBSCRIPTION_RESULT_ERROR (1)
 *   -> خطأ عام. راجع detailedCode لمعرفة السبب:
 *      - عنوان SM-DP+ غير صالح
 *      - كود التفعيل غير صحيح الصيغة
 *      - eUICC ممتلئة
 *      - فشل الاتصال بالخادم، أو الكود منتهي/مستخدم مسبقاً
 *        (أقرب مؤشر على "eSIM غير موجودة")
 *
 * EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR (2)
 *   -> يحتاج تدخل المستخدم، وليس بالضرورة فشلاً نهائياً.
 */
