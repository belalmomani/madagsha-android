# مفتاح التوقيع الثابت (مرة واحدة فقط، من الهاتف)
1. في Termux: `pkg install openjdk-17`
2. `keytool -genkeypair -v -keystore madagsha-release.jks -alias madagsha -keyalg RSA -keysize 2048 -validity 10000`
3. **انسخ الملف احتياطيًا** (Drive خاص/ذاكرة خارجية) واحفظ كلمات المرور. ضياعه = عدم القدرة على تحديث التطبيق.
4. `base64 -w0 madagsha-release.jks` ثم أنشئ Secrets في GitHub (Settings ▸ Secrets ▸ Actions): `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
5. بعد البناء: `apksigner verify --print-certs app-release.apk` (تُنفَّذ تلقائيًا في Actions). لا ترفع .jks إلى GitHub أبدًا (موجود في .gitignore).
