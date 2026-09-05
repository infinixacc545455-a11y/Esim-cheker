package com.example.esimchecker

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvSupportStatus = findViewById(R.id.tvSupportStatus)
        etLpaCode = findViewById(R.id.etLpaCode)
        btnCheck = findViewById(R.id.btnCheck)
        tvResult = findViewById(R.id.tvResult)

        esimChecker = EsimChecker(this)

        // عرض حالة دعم eSIM عند فتح التطبيق
        tvSupportStatus.text = if (esimChecker.isEsimSupported()) {
            "✅ الجهاز يدعم eSIM"
        } else {
            "❌ الجهاز لا يدعم eSIM أو الخدمة غير مفعّلة"
        }

        receiver = EsimChecker.ResultReceiver { resultCode, detailedCode ->
            runOnUiThread {
                tvResult.text = interpretResult(resultCode, detailedCode)
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

    private fun interpretResult(resultCode: Int, detailedCode: Int?): String {
        return when (resultCode) {
            EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK ->
                "✅ نجح! الملف الشخصي صالح وتم تثبيته."
            EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR ->
                "❌ فشل. الكود على الأرجح غير موجود/منتهي/مستخدم من قبل.\n" +
                        "رمز تفصيلي: ${detailedCode ?: "غير متوفر"}"
            EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR ->
                "⚠️ يحتاج تدخل إضافي من المستخدم (تأكيد في نافذة النظام)."
            else ->
                "نتيجة غير معروفة: $resultCode"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        esimChecker.unregisterReceiver(receiver)
    }
}
