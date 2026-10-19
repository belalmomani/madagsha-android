# إعداد Firebase
1. console.firebase.google.com ▸ مشروع جديد ▸ أضف تطبيق Android بالحزمة `discussion.com.paper`.
2. فعّل Authentication (Anonymous أو Google) وCloud Firestore، وانشر `firestore.rules` من هذا المستودع.
3. نزّل `google-services.json` وضعه في `app/` (مُتجاهَل في git) أو خزّنه في Secret باسم `GOOGLE_SERVICES_JSON`.
4. بدونه تعمل اللعبة محليًا بالكامل. ملاحظة: تسجيل الدخول (واجهة Auth) غير مُنفَّذ بعد في الواجهة؛ الأساس جاهز في FirebaseRemote.
