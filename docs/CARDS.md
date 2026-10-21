# أوراق اللعب
- **الأصول:** `app/src/main/res/drawable-nodpi/card_<rank>_<suit>.png` (32 وجهاً) + `card_back.png` للظهر. الحجم 420×607 بكسل (نسبة ثابتة).
  الرتب: qass, shaib, bint, walad, ten, nine, eight, seven • الأنواع: spades, hearts, clubs, diamonds.
- **الربط بالكود:** `CardAssets.kt` (جدول واضح لكل ورقة) • العرض والحركة والتوهج: `CardView` و`DealtCard` في `Screens.kt`.
- **التغيير لاحقاً:** إما استبدل أي PNG بنفس الاسم والنسبة، أو عدّل `tools/generate_cards.py` (الشخصيات في `figure()`، ألوان الأزياء في `ROBE`، الإطار الزخرفي في `corner_orn`، الظهر في `tools/cards.py: back_card`) ثم شغّل `cd tools && python3 generate_cards.py` من جذر المشروع بعد ضبط المسارات (يحتاج Pillow وخط DejaVu).
- **الحركة:** توزيع من المنتصف مع دوران وقلب (`DealtCard`)، رفع الورقة وتوهج ذهبي عند اللمس، وتوهج الأوراق الرابحة في شاشة النتيجة.

# صور اللاعبين
- 8 بورتريهات أصلية: `app/src/main/res/drawable-nodpi/avatar_1.png … avatar_8.png` (512×512، دائرية، خلفية خضراء داكنة).
- الأسماء بحسب المظهر في `GameViewModel.avatarNames` (الترتيب نفسه). العرض في `PlayerAvatar` (إطار ذهبي، توهج نابض عند الدور، شارة اسم/رصيد).
- لتغيير الصور: استبدل الملفات بنفس الاسم، أو عدّل `tools/generate_avatars.py` (قائمة `PEOPLE`).

## الطابع الجديد
ورق عاجي بملمس، إطار ذهبي بزخارف زوايا، شخصيات بحدود حبر داخل معيّن مزخرف مع رمز النوع الكبير بجانبها ورموز صغيرة على الجانبين. لرفع الجودة أكثر استبدل أي بطاقة PNG برسم فنان بنفس الاسم والنسبة (420×607).
