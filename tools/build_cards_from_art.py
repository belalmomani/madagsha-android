"""يبني الأوراق الـ32 + الظهر.
- أوراق الشايب/البنت/الولد: من رسومك الأصلية (art/court_*_clean.png) بعد إزالة المؤشرات القديمة، ثم يرسم مؤشرات ورموز كل نوع بوضوح.
- الأرقام والقص والظهر: بنفس الورق والإطار.
التشغيل من جذر المشروع:  python3 tools/build_cards_from_art.py
"""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image, ImageDraw, ImageFilter, ImageChops
import cards as C
import generate_cards as V           # يضبط أدوات الزخرفة ويثبّت C.corner
from cards import *

OUT = 'app/src/main/res/drawable-nodpi/'
C.FINAL = (504, 728)
C.IVORY1 = (249, 244, 230); C.IVORY2 = (238, 228, 206)

def base_final():
    """ورق بلا حدود خارجية (كورقك المرجعية) مع إطار ذهبي رفيع وزخارف زوايا."""
    img = Image.new('RGB', (W, H), (0, 0, 0))
    mask = mk((W, H), lambda d: rr(d, (0, 0, W - 1, H - 1), 80, fill=255))
    face = grad_img((W, H), C.IVORY1, C.IVORY2)
    img.paste(face, (0, 0), mask)
    n = Image.effect_noise((W, H), 18).convert('L').point(lambda v: 255 - abs(v - 128) // 3)
    img.paste(Image.blend(img, ImageChops.multiply(img, Image.merge('RGB', (n, n, n))), 0.10), (0, 0), mask)
    d = ImageDraw.Draw(img)
    m = 150
    V.corner_orn(d, m, m, 1, 1); V.corner_orn(d, W - m, m, -1, 1); V.corner_orn(d, m, H - m, 1, -1); V.corner_orn(d, W - m, H - m, -1, -1)
    return img, mask
C.base_card = base_final

def court_from_art(rank, suit):
    art = Image.open(f'art/court_{rank}_clean.png').convert('RGB').resize((W, H), Image.LANCZOS)
    mask = mk((W, H), lambda d: rr(d, (0, 0, W - 1, H - 1), 80, fill=255))
    img = art.copy()
    draw_suit(img, suit, 292, 302, 220)
    draw_suit(img, suit, W - 300, H - 302, 210, flip=True)
    if rank != 'walad':
        draw_suit(img, suit, 68, H // 2, 70); draw_suit(img, suit, W - 68, H // 2, 70, flip=True)
    V.corner_v2(img, rank, suit)
    return finalize(img, mask)

if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    allc = []
    for s in SUITS:
        for r in RANKS:
            if r in ('shaib', 'bint', 'walad'): c = court_from_art(r, s)
            elif r == 'qass': c = C.ace_card(s)
            else: c = C.number_card(r, s)
            c.save(OUT + f'card_{r}_{s}.png', optimize=True); allc.append(c)
    C.back_card().save(OUT + 'card_back.png', optimize=True)
    sheet = Image.new('RGB', (8 * 520, 4 * 744), (11, 61, 36))
    for i, c in enumerate(allc): sheet.paste(c, ((i % 8) * 520 + 8, (i // 8) * 744 + 8), c)
    sheet.save('/tmp/cards_final.png'); print('ok', len(allc))
