package com.example.esimchecker

import android.content.Intent
import android.os.Bundle
import android.telephony.euicc.EuiccManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var esimChecker: EsimChecker
    private lateinit var receiver: EsimChecker.ResultReceiver

    private lateinit var tvSupportStatus: TextView
    private lateinit var etLpaCode: EditText
    private lateinit var btnCheck: Button
    private lateinit var tvResult: TextView

    companion object {
        private const val REQ_RESOLVE = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvSupportStatus = findViewById(R.id.tvSupportStatus)
        etLpaCode = findViewById(R.id.etLpaCode)
        btnCheck = findViewById(R.id.btnCheck)
        tvResult = findViewById(R.id.tvResult)

        esimChecker = EsimChecker(this)

        tvSupportStatus.text = if (esimChecker.isEsimSupported()) {
            "✅ الجهاز يدعم eSIM"
        } else {
            "❌ الجهاز لا يدعم eSIM أو الخدمة غير مفعّلة"
        }

        receiver = EsimChecker.ResultReceiver { resultCode, detailedCode, intent ->
            runOnUiThread {
                handleResult(resultCode, detailedCode, intent)
            }
        }
        esimChecker.registerReceiver(receiver)

        btnCheck.setOnClickListener {
            val code = etLpaCode.text.toString().trim()
            if (code.isEmpty()) {
                tvResult.text = "من فضلك أدخل كود LPA أولاً"
                return@setOnClickListener
            }
            try {
                tvResult.text = "جارِ الفحص... (قد تظهر نافذة تأكيد من النظام)"
                esimChecker.checkAndInstall(code)
            } catch (e: Exception) {
                tvResult.text = "خطأ: ${e.message}"
            }
        }
    }

    private fun handleResult(resultCode: Int, detailedCode: Int?, intent: Intent) {
        when (resultCode) {
            EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK -> {
                tvResult.text = "✅ نجح! الملف الشخصي صالح وتم تثبيته."
            }
            EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR -> {
                val detailText = detailedCode?.let { code ->
                    "$code${interpretDetailedCode(code)}"
                } ?: "غير متوفر"
                tvResult.text = "❌ فشل التثبيت.\n" +
                        "رمز تفصيلي: $detailText\n\n" +
                        "ملاحظة: على بعض الأجهزة (مثل OnePlus/Oppo/Vivo)، قد يظهر " +
                        "هذا الخطأ حتى مع كود صحيح، بسبب قيود الشركة المصنّعة على " +
                        "تطبيقات الطرف الثالث، وليس بالضرورة عيباً في الكود نفسه."
            }
            EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR -> {
                tvResult.text = "⚠️ جارِ فتح نافذة تأكيد النظام..."
                try {
                    esimChecker.startResolution(this, REQ_RESOLVE, intent)
                } catch (e: Exception) {
                    tvResult.text = "خطأ في فتح نافذة التأكيد: ${e.message}"
                }
            }
            else -> {
                tvResult.text = "نتيجة غير معروفة: $resultCode"
            }
        }
    }

    private fun interpretDetailedCode(code: Int): String {
        return when {
            code == 10000 || code == 10001 -> " (احتمال: مشكلة في عنوان SM-DP+ أو الاتصال بالخادم)"
            code in 10002..10010 -> " (احتمال: الكود غير صالح أو تنسيقه خاطئ)"
            else -> " (راجع توثيق EuiccManager أو ابحث عن الرمز تحديداً لمعرفة السبب الدقيق)"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_RESOLVE) {
            tvResult.text = "تمت معالجة نافذة التأكيد. جارِ انتظار النتيجة النهائية..."
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        esimChecker.unregisterReceiver(receiver)
    }
}
