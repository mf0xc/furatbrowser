# 🖤 Furat Browser

متصفح Furat Browser - متصفح أندرويد سريع وخفيف مع مانع إعلانات مدمج.

## 🚀 بناء APK على Windows

### الطريقة السريعة (PowerShell)

```powershell
# 1. ادخل مجلد المشروع
cd "C:\Users\Damasco\OneDrive\Desktop\furatbrowser"

# 2. شغّل سكربت البناء
.\build-windows.ps1
```

### أو يدوياً (Command Prompt)

```cmd
:: 1. ادخل المجلد
cd /d "C:\Users\Damasco\OneDrive\Desktop\furatbrowser"

:: 2. بناء Debug APK
gradlew.bat assembleDebug --no-daemon

:: 3. APK جاهز في:
:: app\build\outputs\apk\debug\app-debug.apk
```

### أو بالنقر المزدوج

انقر مزدوجاً على `build-windows.bat` وسيبني APK تلقائياً.

---

## ⚠️ متطلبات Windows

| البرنامج | الرابط | ضروري؟ |
|----------|--------|--------|
| **JDK 17** | https://adoptium.net/ | ✅ نعم |
| **Android Studio** | https://developer.android.com/studio | ⚠️ مستحسن |
| **Git** | https://git-scm.com/download/win | ❌ اختياري |

### إعداد JAVA_HOME على Windows:

```powershell
# افتح PowerShell كـ Administrator
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17", "Machine")
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";%JAVA_HOME%\bin", "Machine")
```

أو يدوياً:
1. اضغط Win + R → اكتب `sysdm.cpl` → Enter
2. Advanced → Environment Variables
3. New → `JAVA_HOME` → مسار JDK
4. Edit Path → أضف `%JAVA_HOME%\bin`

---

## 🔧 حل مشاكل شائعة على Windows

### ❌ "gradlew.bat is not recognized"
```powershell
# الحل: شغّل من مجلد المشروع مباشرة
.\gradlew.bat assembleDebug
```

### ❌ "JAVA_HOME is not set"
```powershell
# الحل: ثبّت JDK 17 واضبط JAVA_HOME
# من Adoptium: https://adoptium.net/
```

### ❌ "gradle-wrapper.jar not found"
```powershell
# الحل: افتح المشروع بـ Android Studio مرة واحدة
# أو شغّل: .\build-windows.ps1 (رح يحمّله تلقائياً)
```

### ❌ "Build failed with daemon"
```powershell
# الحل: أضف --no-daemon
.\gradlew.bat assembleDebug --no-daemon
```

---

## 📱 تثبيت APK على الهاتف

### الطريقة 1: USB + ADB
```powershell
# فعّل Developer Options → USB Debugging على الهاتف
adb install app\build\outputs\apk\debug\app-debug.apk
```

### الطريقة 2: نقل يدوي
1. انسخ `app-debug.apk` للهاتف
2. افتحه من مدير الملفات
3. سمّح بالتثبيت من مصادر غير معروفة

### الطريقة 3: Android Studio
```
Run → Run 'app' → اختار جهاز
```

---

## 📂 هيكل المشروع

```
furat_browser/
├── .github/workflows/build.yml    # CI/CD تلقائي
├── app/
│   ├── build.gradle.kts           # إعدادات البناء
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/furat/browser/
│       │   ├── MainActivity.kt     # النشاط الرئيسي
│       │   ├── BrowserViewModel.kt # إدارة الحالة
│       │   ├── Database.kt         # Room DB
│       │   └── AdBlocker.kt       # مانع الإعلانات
│       └── res/                    # الواجهات والأيقونات
├── build-windows.ps1              # ⬅️ سكربت بناء Windows
├── build-windows.bat              # ⬅️ بديل CMD
├── gradlew.bat                    # ⬅️ Gradle Wrapper (Windows)
└── gradlew                        # Gradle Wrapper (Linux/Mac)
```

---

## 🔄 البناء التلقائي (GitHub Actions)

1. ارفع المشروع على GitHub
2. اذهب لـ Actions → Build Furat Browser APK
3. اضغط "Run workflow"
4. بعد 5 دقايق، APK جاهز للتحميل

---

## 🔐 توقيع APK للنشر

### إنشاء Keystore:
```powershell
# افتح Command Prompt كـ Administrator
keytool -genkey -v -keystore furat.keystore -alias furat -keyalg RSA -keysize 2048 -validity 10000
```

### توقيع:
```powershell
# 1. توقيع
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 `
  -keystore furat.keystore `
  app\build\outputs\apk\release\app-release-unsigned.apk furat

# 2. محاذاة
zipalign -v 4 `
  app\build\outputs\apk\release\app-release-unsigned.apk `
  FuratBrowser-v1.0.apk
```

---

## ⚙️ المتطلبات

- Windows 10/11 (64-bit)
- JDK 17
- Android SDK 34 (مع Android Studio)
- 4 GB RAM (8 GB مستحسن)
- 2 GB مساحة فارغة

## 📜 الترخيص

MIT License - حر الاستخدام 🖤
