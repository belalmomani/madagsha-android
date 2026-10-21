"""استيراد رسوم أوراق جاهزة (من مصمم أو مولّد صور) إلى المشروع.
الاستخدام:  python3 tools/import_art.py <مجلد_الصور> [--add-index] [--out app/src/main/res/drawable-nodpi]
أسماء الملفات: <rank>_<suit>.png|jpg  مثل  shaib_spades.png  ،  back.png للظهر.
  rank: qass shaib bint walad ten nine eight seven      suit: spades hearts clubs diamonds
--add-index : يضيف مؤشر الرتبة العربي والرمز في الزوايا (استخدمه إن كانت الرسوم بلا مؤشرات).
"""
import sys, os, argparse
from PIL import Image, ImageDraw
sys.path.insert(0, os.path.dirname(__file__))
FINAL = (420, 607); RATIO = FINAL[0] / FINAL[1]

def fit(im):
    w, h = im.size
    if w / h > RATIO:
        nw = int(h * RATIO); im = im.crop(((w - nw) // 2, 0, (w - nw) // 2 + nw, h))
    else:
        nh = int(w / RATIO); im = im.crop((0, (h - nh) // 2, w, (h - nh) // 2 + nh))
    return im

def rounded(im, r):
    m = Image.new('L', im.size, 0); ImageDraw.Draw(m).rounded_rectangle((0, 0, im.size[0] - 1, im.size[1] - 1), r, fill=255)
    o = Image.new('RGBA', im.size, (0, 0, 0, 0)); o.paste(im, (0, 0), m); return o

def main():
    ap = argparse.ArgumentParser(); ap.add_argument('src'); ap.add_argument('--add-index', action='store_true')
    ap.add_argument('--out', default='app/src/main/res/drawable-nodpi'); a = ap.parse_args()
    if a.add_index:
        import generate_cards as V, cards as C
    ranks = {'qass', 'shaib', 'bint', 'walad', 'ten', 'nine', 'eight', 'seven'}; suits = {'spades', 'hearts', 'clubs', 'diamonds'}
    done = []
    os.makedirs(a.out, exist_ok=True)
    for f in sorted(os.listdir(a.src)):
        stem, ext = os.path.splitext(f)
        if ext.lower() not in ('.png', '.jpg', '.jpeg', '.webp'): continue
        im = fit(Image.open(os.path.join(a.src, f)).convert('RGB'))
        if stem == 'back': name = 'card_back'
        else:
            try: r, s = stem.split('_')
            except ValueError: print('تجاهل (اسم غير صحيح):', f); continue
            if r not in ranks or s not in suits: print('تجاهل (اسم غير صحيح):', f); continue
            name = f'card_{r}_{s}'
            if a.add_index:
                big = im.resize((C.W, C.H), Image.LANCZOS); C.corner(big, r, s); im = big
        im = im.resize(FINAL, Image.LANCZOS)
        rounded(im, 28).save(os.path.join(a.out, name + '.png'), optimize=True); done.append(name)
    print('تم استيراد', len(done), 'ملف:', ', '.join(done))
if __name__ == '__main__': main()
