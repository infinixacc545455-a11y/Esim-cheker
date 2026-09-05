# Esim Checker

تطبيق أندرويد بسيط لفحص/محاولة تثبيت كود eSIM بصيغة LPA عبر `EuiccManager`.

## طريقة البناء عبر GitHub Actions (من الهاتف، بدون كمبيوتر)

### الخطوات:

1. **أنشئ حساب GitHub** (لو مالكش واحد) عبر تطبيق GitHub أو المتصفح.

2. **أنشئ مستودع (Repository) جديد**:
   - افتح تطبيق GitHub أو الموقع من المتصفح.
   - اضغط "New Repository" واسمه مثلاً `EsimChecker`.
   - خليه **Public** أو **Private** (الاثنين يشتغلوا مع GitHub Actions، لكن الخاص عنده حد ساعات مجانية شهرية محدود).

3. **ارفع كل الملفات دي بنفس الهيكل**:
   ```
   EsimChecker/
   ├── .github/workflows/build-apk.yml
   ├── app/
   │   ├── build.gradle
   │   ├── proguard-rules.pro
   │   └── src/main/
   │       ├── AndroidManifest.xml
   │       ├── java/com/example/esimchecker/
   │       │   ├── EsimChecker.kt
   │       │   └── MainActivity.kt
   │       └── res/
   │           ├── layout/activity_main.xml
   │           └── values/strings.xml
   ├── build.gradle
   └── settings.gradle
   ```
   - أسهل طريقة من الهاتف: استخدم تطبيق GitHub، ادخل على المستودع، اختر "Add file > Upload files"، وارفع الملفات واحد واحد محافظاً على نفس المسارات (لازم تنشئ المجلدات بالاسم الصحيح وقت الرفع، GitHub بيسمح تكتب المسار كامل في اسم الملف مثل `app/src/main/AndroidManifest.xml`).

4. **البناء التلقائي**:
   - بمجرد ما ترفع الملفات (push) على فرع `main`، GitHub Actions هيشتغل تلقائياً وينفذ البناء.
   - تقدر تتابع التقدم من تبويب **Actions** في المستودع.

5. **تحميل الـ APK**:
   - بعد ما ينتهي البناء (يستغرق 3-5 دقائق عادة)، افتح الـ workflow run الناجح.
   - هتلاقي **Artifact** اسمه `esim-checker-debug-apk` — نزّله (هيكون ملف zip يحتوي على الـ APK).
   - فك الضغط وثبّت الـ APK على هاتفك (لازم تفعّل "تثبيت من مصادر غير معروفة" في إعدادات أندرويد).

## ملاحظات مهمة

- هذا debug build غير موقّع (unsigned/debug-signed) — مناسب للتجربة الشخصية فقط، مش للنشر على المتجر.
- بدون Carrier Privileges، التطبيق هيفتح نافذة تأكيد من نظام أندرويد عند محاولة تثبيت eSIM، مش هيشتغل بصمت بالكامل.
- بعض أكواد SM-DP+ تُستخدم مرة واحدة فقط — كن حذراً عند التجربة بأكواد حقيقية.
