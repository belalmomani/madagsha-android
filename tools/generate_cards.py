import math, random, os
from PIL import Image, ImageDraw, ImageFilter, ImageChops
import cards as C
from cards import *

INK=(26,18,14)
random.seed(7)
# suit robe palettes: (robe light, robe dark, accent)
ROBE={'spades':((120,78,44),(62,36,20),(214,170,70)),
      'hearts':((170,50,52),(88,18,26),(230,184,84)),
      'clubs':((46,112,78),(18,58,40),(222,178,76)),
      'diamonds':((66,84,150),(30,40,92),(226,182,80))}

def paper(img,mask):
    n=Image.effect_noise((W,H),22).convert('L').point(lambda v:int(v*0.0+128+(v-128)*0.35))
    tex=Image.merge('RGB',(n,n,n))
    img.paste(Image.blend(img,ImageChops.multiply(img,tex.point(lambda v:min(255,int(v*1.35)))),0.28),(0,0),mask)

def corner_orn(d,x,y,sx,sy,col=GOLD2):
    # L-shaped filigree at a corner; (sx,sy) = direction signs
    L=250
    d.line((x,y,x+sx*L,y),fill=col,width=5); d.line((x,y,x,y+sy*L),fill=col,width=5)
    d.line((x+sx*14,y+sy*14,x+sx*(L-60),y+sy*14),fill=GOLD1,width=3); d.line((x+sx*14,y+sy*14,x+sx*14,y+sy*(L-60)),fill=GOLD1,width=3)
    for k,(r) in enumerate((46,30,16)):
        cx0=x+sx*(40+k*6); cy0=y+sy*(40+k*6)
        box=(cx0-r,cy0-r,cx0+r,cy0+r)
        d.arc(box,0,270,fill=col,width=4)
    d.ellipse((x+sx*L-8,y-8,x+sx*L+8,y+8),fill=GOLD1); d.ellipse((x-8,y+sy*L-8,x+8,y+sy*L+8),fill=GOLD1)

def base_card_v2():
    img,mask=C.__dict__['_orig_base']()
    paper(img,mask)
    d=ImageDraw.Draw(img)
    m=150
    corner_orn(d,m,m,1,1); corner_orn(d,W-m,m,-1,1); corner_orn(d,m,H-m,1,-1); corner_orn(d,W-m,H-m,-1,-1)
    return img,mask
C._orig_base=C.base_card
C.base_card=base_card_v2

def smooth(m,r=14):
    return m.filter(ImageFilter.GaussianBlur(r)).point(lambda v:255 if v>128 else 0).filter(ImageFilter.GaussianBlur(1.2))

def robe_pattern(lay,mask,acc_col,step=44):
    lat=lattice(lay.size,acc_col,150,step=step); 
    t=Image.new('RGBA',lay.size,(0,0,0,0)); t.paste(lat,(0,0))
    a=ImageChops.multiply(t.split()[3],mask)
    lay.paste(Image.new('RGB',lay.size,acc_col),(0,0),a)

def figure(rank,suit):
    pal=ROBE[suit]; FWW,FHH=700,600
    lay=Image.new('RGB',(FWW,FHH),(0,0,0)); acc=Image.new('L',(FWW,FHH),0)
    def add(m,c1,c2=None):
        nonlocal acc
        bb=m.getbbox()
        if not bb: return
        x0,y0,x1,y1=bb; gi=grad_img((x1-x0,y1-y0),c1,c2 or c1); full=Image.new('RGB',(FWW,FHH),c1); full.paste(gi,(x0,y0))
        lay.paste(full,(0,0),m); acc=ImageChops.lighter(acc,m)
    def mkk(fn): 
        m=Image.new('L',(FWW,FHH),0); fn(ImageDraw.Draw(m)); return m
    cx=350
    # props behind (staff / sceptre)
    if rank in('shaib','bint','walad'):
        rod=mkk(lambda d:d.polygon([(cx+170,FHH),(cx+185,FHH),(cx+70,330),(cx+55,330)],fill=255)); add(rod,GOLD1,GOLD2)
        if rank=='shaib':
            j=mkk(lambda d:d.ellipse((cx+22,282,cx+98,358),fill=255)); add(j,(200,40,70),(110,10,40))
            r2=mkk(lambda d:d.ellipse((cx+38,296,cx+82,340),fill=255)); add(r2,GOLD1,GOLD2)
        elif rank=='bint':
            add(mkk(lambda d:d.polygon(heart_pts(cx+62,300,120),fill=255)).rotate(180,center=(cx+62,300)),GOLD1,GOLD2)
        else:
            add(mkk(lambda d:d.polygon([(cx+55,330),(cx+130,300),(cx+60,270)],fill=255)),(230,230,220),(190,190,180))
    # robe (rounded rhombus-like)
    robe=smooth(mkk(lambda d:d.polygon([(cx-120,350),(cx+120,350),(cx+300,470),(cx+330,FHH),(cx-330,FHH),(cx-300,470)],fill=255)),16)
    add(robe,pal[0],pal[1])
    robe_pattern(lay,robe,pal[2])
    # side bands
    for s in (-1,1):
        band=mkk(lambda d:d.polygon([(cx+s*120,350),(cx+s*160,350),(cx+s*330,FHH),(cx+s*290,FHH)],fill=255)); band=ImageChops.multiply(band,robe)
        add(band,GOLD1,GOLD2)
    d=ImageDraw.Draw(lay)
    # collar / placket
    plack=mkk(lambda d:d.polygon([(cx-46,372),(cx+46,372),(cx+36,FHH),(cx-36,FHH)],fill=255)); add(plack,(244,232,200),(204,186,150))
    for yy in range(410,FHH,52): d.ellipse((cx-8,yy,cx+8,yy+16),fill=pal[2])
    # pendant
    d.polygon([(cx,396),(cx+26,436),(cx,476),(cx-26,436)],fill=GOLD1,outline=INK); d.ellipse((cx-9,426,cx+9,444),fill=(200,30,60))
    # neck + head
    nk=mkk(lambda d:d.rectangle((cx-40,300,cx+40,380),fill=255)); add(nk,SKIN[0],SKIN[1])
    cy=262
    # beard (king)
    if rank=='shaib':
        bm=mkk(lambda d:d.pieslice((cx-84,cy-10,cx+84,cy+158),0,180,fill=255)); add(bm,(40,32,28),(18,14,12))
    hm=mkk(lambda d:(d.ellipse((cx-76,cy-96,cx+76,cy+98),fill=255),d.ellipse((cx-84,cy-18,cx-68,cy+34),fill=255),d.ellipse((cx+68,cy-18,cx+84,cy+34),fill=255)))
    add(hm,SKIN[0],SKIN[1])
    dd=ImageDraw.Draw(lay)
    # eyes
    for s in (-1,1):
        ex=cx+s*32; ey=cy-8
        dd.ellipse((ex-19,ey-9,ex+19,ey+9),fill=(252,250,246),outline=INK,width=2)
        dd.ellipse((ex-8,ey-8,ex+8,ey+8),fill=(58,38,24)); dd.ellipse((ex-3,ey-5,ex+1,ey-1),fill=(255,255,255))
        dd.arc((ex-24,ey-30,ex+24,ey+6),200,340,fill=INK,width=6 if rank=='bint' else 5)
        dd.arc((ex-22,ey-6,ex+22,ey+22),20,160,fill=(190,130,100),width=2)
    dd.line([(cx,cy+2),(cx-7,cy+32),(cx+5,cy+34)],fill=(150,96,70),width=4)
    lipc=(170,56,64) if rank=='bint' else (150,70,60)
    dd.polygon([(cx-22,cy+54),(cx,cy+46),(cx+22,cy+54),(cx,cy+66)],fill=lipc,outline=INK)
    if rank=='shaib':
        dd.polygon([(cx-30,cy+46),(cx,cy+40),(cx+30,cy+46),(cx+18,cy+52),(cx-18,cy+52)],fill=(34,26,22))
    # headwear
    if rank=='shaib':
        gm=mkk(lambda d:(d.pieslice((cx-104,cy-160,cx+104,cy+40),180,360,fill=255),d.polygon([(cx-104,cy-58),(cx-168,cy+210),(cx-92,cy+224),(cx-80,cy-8)],fill=255),d.polygon([(cx+104,cy-58),(cx+168,cy+210),(cx+92,cy+224),(cx+80,cy-8)],fill=255)))
        face=mkk(lambda d:d.ellipse((cx-76,cy-60,cx+76,cy+96),fill=255)); gm=ImageChops.subtract(gm,face)
        gm=ImageChops.subtract(gm,ImageChops.multiply(robe,Image.new('L',(FWW,FHH),0)))
        add(gm,(252,248,248),(214,210,214))
        chk=Image.new('RGB',(FWW,FHH),(0,0,0)); cd=ImageDraw.Draw(chk)
        for xx in range(0,FWW,22): cd.line((xx,0,xx,FHH),fill=(196,40,52),width=5)
        for yy in range(0,FHH,22): cd.line((0,yy,FWW,yy),fill=(196,40,52),width=5)
        lay.paste(chk,(0,0),ImageChops.multiply(gm,Image.new('L',(FWW,FHH),200)))
        ag=mkk(lambda d:d.ellipse((cx-110,cy-122,cx+110,cy-64),fill=255)); inn=mkk(lambda d:d.ellipse((cx-96,cy-110,cx+96,cy-70),fill=255))
        add(ImageChops.subtract(ag,inn),(24,22,24),(60,56,58))
    elif rank=='bint':
        sc=mkk(lambda d:(d.ellipse((cx-120,cy-150,cx+120,cy+130),fill=255),d.polygon([(cx-120,cy+20),(cx-170,FHH-120),(cx+170,FHH-120),(cx+120,cy+20)],fill=255)))
        face=mkk(lambda d:d.ellipse((cx-70,cy-62,cx+70,cy+92),fill=255)); sc=ImageChops.subtract(sc,face)
        add(sc,(60,48,40),(24,18,16))
        trim=mkk(lambda d:d.ellipse((cx-118,cy-146,cx+118,cy+126),outline=255,width=16)); trim=ImageChops.subtract(trim,face); trim=ImageChops.multiply(trim,sc)
        add(trim,GOLD1,GOLD2)
        d2=ImageDraw.Draw(lay)
        for k in range(-5,6): d2.polygon([(cx+k*20,cy-134),(cx+k*20+9,cy-122),(cx+k*20,cy-110),(cx+k*20-9,cy-122)],fill=(30,22,18))
        # forehead jewel + earrings
        d2.ellipse((cx-12,cy-100,cx+12,cy-76),fill=(210,30,70),outline=GOLDD,width=3); d2.line((cx,cy-134,cx,cy-100),fill=GOLD1,width=4)
        for s in (-1,1): d2.ellipse((cx+s*82-10,cy+30,cx+s*82+10,cy+70),fill=GOLD1,outline=INK)
        # necklace
        for r in (96,80,64): d2.arc((cx-r,330,cx+r,330+r*0.8),15,165,fill=GOLD1,width=6)
    else:
        hair=mkk(lambda d:d.ellipse((cx-86,cy-110,cx+86,cy+50),fill=255)); face=mkk(lambda d:d.ellipse((cx-70,cy-42,cx+70,cy+96),fill=255)); hair=ImageChops.subtract(hair,face)
        add(hair,(60,42,32),(22,14,12))
        tm=mkk(lambda d:(d.ellipse((cx-102,cy-176,cx+102,cy-34),fill=255),d.rectangle((cx-100,cy-92,cx+100,cy-58),fill=255)))
        add(tm,(200,52,60),(110,18,30))
        band=mkk(lambda d:d.rectangle((cx-102,cy-96,cx+102,cy-62),fill=255)); add(band,GOLD1,GOLD2)
        d3=ImageDraw.Draw(lay)
        for k in range(-4,5): d3.ellipse((cx+k*22-6,cy-87,cx+k*22+6,cy-71),fill=(190,30,52),outline=INK)
        add(mkk(lambda d:d.polygon([(cx+10,cy-172),(cx-30,cy-236),(cx+10,cy-266),(cx+46,cy-206)],fill=255)),(250,248,240),(210,200,190))
    # ink outline + soft inner shading
    ol=ImageChops.subtract(acc.filter(ImageFilter.MaxFilter(9)),acc)
    lay.paste(Image.new('RGB',(FWW,FHH),INK),(0,0),ol)
    full=ImageChops.lighter(acc,ol)
    # gentle light/shadow
    sh=Image.new('L',(FWW,FHH),0); ImageDraw.Draw(sh).ellipse((cx+10,40,cx+700,FHH+300),fill=70); sh=sh.filter(ImageFilter.GaussianBlur(60))
    lay.paste(Image.new('RGB',(FWW,FHH),(20,10,10)),(0,0),ImageChops.multiply(sh,acc))
    return lay,full

def court_v2(rank,suit):
    img,mask=C.base_card(); d=ImageDraw.Draw(img)
    lay,full=figure(rank,suit)
    FWW,FHH=lay.size
    fig=Image.new('RGBA',lay.size,(0,0,0,0)); fig.paste(lay,(0,0),full)
    sc=0.92; fw,fh=int(FWW*sc),int(FHH*sc)
    top=fig.resize((fw,fh),Image.LANCZOS)
    cxp=W//2; midy=H//2
    # big diamond-rounded glow behind figure
    glow=Image.new('L',(W,H),0); ImageDraw.Draw(glow).polygon([(cxp,midy-520),(cxp+330,midy),(cxp,midy+520),(cxp-330,midy)],fill=255)
    glow=smooth(glow,40)
    img.paste(Image.new('RGB',(W,H),(246,236,208)),(0,0),glow.point(lambda v:int(v*0.6)))
    img.paste(top,(cxp-fw//2,midy-fh),top)
    bot=top.rotate(180)
    img.paste(bot,(cxp-fw//2,midy),bot)
    d=ImageDraw.Draw(img)
    # centre ornament band
    d.polygon(star8(cxp,midy,26),fill=GOLD1,outline=INK)
    # large suit pip (top-left inside frame) + rotated bottom-right, small side pips
    draw_suit(img,suit,318,300,150)
    pip=Image.new('RGBA',(W,H),(0,0,0,0)); 
    tmp=img.copy(); draw_suit(tmp,suit,W-318,H-300,150,flip=True); img.paste(tmp,(0,0))
    draw_suit(img,suit,200,H//2+4,64); draw_suit(img,suit,W-200,H//2-4,64,flip=True)
    corner_idx(img,rank,suit)
    return finalize(img,mask)

def corner_v2(img,rank,suit):
    bg=img.copy(); d=ImageDraw.Draw(img); col=SUIT_COL[suit]
    sz={'ten':140,'shaib':74,'bint':92,'walad':92,'qass':100}.get(rank,170)
    f=font(sz); lab=LABEL[rank]; bb=d.textbbox((0,0),lab,font=f); tw=bb[2]-bb[0]; cx=126
    d.text((cx-tw/2-bb[0],62-bb[1]+8),lab,font=f,fill=col)
    draw_suit(img,suit,cx,300,100)
    box=(38,40,214,360); a=img.crop(box); b=bg.crop(box)
    m=ImageChops.difference(a,b).convert('L').point(lambda v:255 if v>6 else 0).filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.GaussianBlur(1))
    img.paste(a.rotate(180),(W-214,H-360),m.rotate(180))
C.corner=corner_v2
def corner_idx(img,rank,suit):
    C.corner(img,rank,suit)

def ace_v2(suit):
    return C.ace_card(suit)

def number_v2(rank,suit): return C.number_card(rank,suit)

if __name__=='__main__':
    out='app/src/main/res/drawable-nodpi/'
    allc=[]
    for s in SUITS:
        for r in RANKS:
            c=ace_v2(s) if r=='qass' else court_v2(r,s) if r in('shaib','bint','walad') else number_v2(r,s)
            c.save(out+f'card_{r}_{s}.png',optimize=True); allc.append(c)
    sheet=Image.new('RGB',(8*430,4*617),(11,61,36))
    for i,c in enumerate(allc): sheet.paste(c,((i%8)*430+5,(i//8)*617+5),c)
    sheet.save('/tmp/cards_v2.png'); print('ok')
