# Météo Flip Clock – Style HTC Diamond / Sense

تطبيق أندرويد بنفس تصميم ساعة الـ Flip Clock + الطقس المتحرك من هاتف **HTC Diamond / Sense**.

---

## المميزات / Fonctionnalités

- ساعة **Flip Clock** كلاسيكية متحركة
- طقس متحرك (شمس، مطر، غيوم، ثلج، عاصفة...)
- خلفية متحركة متزامنة مع حالة الطقس
- الواجهة بالكامل **بالفرنسية**
- إمكانية إضافة وحذف عدة مدن
- يعمل في وضع تجريبي بدون مفتاح API

---

## كيفية الحصول على ملف APK (جاهز للتثبيت)

### الطريقة الأسهل

1. نزّل المشروع من GitHub:
   - اضغط على الزر الأخضر **Code** → **Download ZIP**
   - أو استخدم الأمر:
     ```bash
     git clone https://github.com/hishemd/Meteo-Flip-Clock.git
     ```

2. افتح المجلد في **Android Studio**

3. انتظر حتى ينتهي Gradle من المزامنة

4. من القائمة:
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

5. بعد الانتهاء اضغط **locate**  
   الملف سيكون هنا:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

6. انقل ملف الـ APK إلى هاتفك وثبّته (فعّل "مصادر غير معروفة" إن لزم).

> **ملاحظة**: لا يوجد APK جاهز مرفق حاليًا لأن بناءه يحتاج Android Studio. الكود كامل وجاهز 100%.

---

## (اختياري) تفعيل الطقس الحقيقي

1. أنشئ حساب مجاني على: https://openweathermap.org/api
2. انسخ مفتاح الـ API
3. افتح الملف:
   ```
   app/src/main/java/com/htcweather/flipclock/data/remote/RetrofitClient.kt
   ```
4. استبدل:
   ```kotlin
   const val API_KEY = "YOUR_OPENWEATHERMAP_API_KEY"
   ```
   بمفتاحك الحقيقي.

بدون مفتاح يعمل التطبيق بوضع تجريبي.

---

## المتطلبات

- Android Studio (Hedgehog أو أحدث)
- JDK 17
- هاتف أندرويد 7.0 فما فوق (API 24+)

---

## روابط

- المستودع: https://github.com/hishemd/Meteo-Flip-Clock
- تحميل الكود كـ ZIP: https://github.com/hishemd/Meteo-Flip-Clock/archive/refs/heads/main.zip

---

إذا أردت APK جاهز، افتح المشروع في Android Studio واتبع الخطوات أعلاه (تستغرق أقل من دقيقتين).
