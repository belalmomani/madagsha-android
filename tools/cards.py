import math, os, random
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageChops

W, H = 1080, 1560
FINAL = (420, 607)
FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
def font(sz): return ImageFont.truetype(FONT, sz, layout_engine=ImageFont.Layout.RAQM)

IVORY1=(252,247,232); IVORY2=(236,225,198)
GOLD1=(246,213,112); GOLD2=(176,125,36); GOLDD=(120,80,20)
INK=(24,26,44); RED=(176,26,40)
SUIT_COL={'spades':INK,'clubs':INK,'hearts':RED,'diamonds':RED}
# palette per suit: (panel top, panel bottom, robe1, robe2)
PAL={'spades':((18,40,84),(8,20,48),(40,70,140),(18,36,90)),
     'hearts':((120,18,34),(64,8,20),(190,38,56),(120,16,34)),
     'clubs':((12,84,58),(6,44,32),(24,140,96),(10,84,56)),
     'diamonds':((88,40,120),(44,18,70),(140,70,180),(84,36,120))}
RANKS=['qass','shaib','bint','walad','ten','nine','eight','seven']
LABEL={'qass':'قص','shaib':'شايب','bint':'بنت','walad':'ولد','ten':'10','nine':'9','eight':'8','seven':'7'}
SUITS=['spades','hearts','clubs','diamonds']

def grad_img(size,c1,c2,vertical=True):
    w,h=size
    g=Image.linear_gradient('L')
    g=g.resize((w,h)) if vertical else g.rotate(90).resize((w,h))
    return Image.composite(Image.new('RGB',size,c2),Image.new('RGB',size,c1),g)

def paste_grad(img,mask,c1,c2,box=None):
    if box is None: box=mask.getbbox()
    if not box: return
    x0,y0,x1,y1=box
    gi=grad_img((max(1,x1-x0),max(1,y1-y0)),c1,c2)
    full=Image.new('RGB',img.size,c1); full.paste(gi,(x0,y0))
    img.paste(full,(0,0),mask)

def mk(size,fn):
    m=Image.new('L',size,0); d=ImageDraw.Draw(m); fn(d); return m

# ---------- suit shapes (polygon points) ----------
def heart_pts(cx,cy,s):
    pts=[]
    for i in range(0,361,3):
        t=math.radians(i)
        x=16*math.sin(t)**3
        y=13*math.cos(t)-5*math.cos(2*t)-2*math.cos(3*t)-math.cos(4*t)
        pts.append((cx+x*s/34,cy-y*s/34))
    return pts
def diamond_pts(cx,cy,s):
    return [(cx,cy-s*0.52),(cx+s*0.36,cy),(cx,cy+s*0.52),(cx-s*0.36,cy)]
def spade_mask(size,cx,cy,s):
    def f(d):
        pts=[(x,-(y-cy)+cy) for x,y in heart_pts(cx,cy-s*0.04,s*0.98)]
        d.polygon(pts,fill=255)
        d.polygon([(cx,cy+s*0.05),(cx-s*0.12,cy+s*0.5),(cx+s*0.12,cy+s*0.5)],fill=255)
        d.polygon([(cx-s*0.2,cy+s*0.5),(cx+s*0.2,cy+s*0.5),(cx,cy+s*0.36)],fill=255)
    return mk(size,f)
def club_mask(size,cx,cy,s):
    r=s*0.2
    def f(d):
        for dx,dy in [(0,-s*0.2),(-s*0.2,s*0.1),(s*0.2,s*0.1)]:
            d.ellipse((cx+dx-r,cy+dy-r,cx+dx+r,cy+dy+r),fill=255)
        d.polygon([(cx,cy),(cx-s*0.12,cy+s*0.5),(cx+s*0.12,cy+s*0.5)],fill=255)
        d.polygon([(cx-s*0.22,cy+s*0.5),(cx+s*0.22,cy+s*0.5),(cx,cy+s*0.38)],fill=255)
    return mk(size,f)
def suit_mask(size,suit,cx,cy,s):
    if suit=='hearts': return mk(size,lambda d:d.polygon(heart_pts(cx,cy,s),fill=255))
    if suit=='diamonds': return mk(size,lambda d:d.polygon(diamond_pts(cx,cy,s),fill=255))
    if suit=='spades': return spade_mask(size,cx,cy,s)
    return club_mask(size,cx,cy,s)

def draw_suit(img,suit,cx,cy,s,flip=False,color=None):
    size=img.size
    m=suit_mask(size,suit,cx,cy,s)
    if flip:
        # rotate mask 180 around (cx,cy)
        m=m.rotate(180,center=(cx,cy))
    c=color or SUIT_COL[suit]
    lite=tuple(min(255,int(v*1.0+ (60 if v<100 else 30))) for v in c)
    paste_grad(img,m,lite,c)
    # subtle highlight
    hl=Image.new('L',size,0); ImageDraw.Draw(hl).ellipse((cx-s*0.18,cy-s*0.3,cx-s*0.02,cy-s*0.12),fill=70)
    hl=ImageChops.multiply(hl.filter(ImageFilter.GaussianBlur(10)),m)
    img.paste(Image.new('RGB',size,(255,255,255)),(0,0),hl)

# ---------- ornament ----------
def star8(cx,cy,r):
    pts=[]
    for i in range(16):
        a=math.radians(i*22.5-90); rr=r if i%2==0 else r*0.62
        pts.append((cx+rr*math.cos(a),cy+rr*math.sin(a)))
    return pts

def lattice(size,c,alpha,step=90):
    layer=Image.new('RGBA',size,(0,0,0,0)); d=ImageDraw.Draw(layer)
    col=(*c,alpha)
    for y in range(-step,size[1]+step,step):
        for x in range(-step,size[0]+step,step):
            d.polygon(star8(x,y,step*0.5),outline=col,width=3)
            d.ellipse((x-step*0.12,y-step*0.12,x+step*0.12,y+step*0.12),outline=col,width=2)
    return layer

def rr(d,box,r,**kw): d.rounded_rectangle(box,r,**kw)

# ---------- card base ----------
def base_card():
    img=Image.new('RGB',(W,H),(0,0,0))
    mask=mk((W,H),lambda d:rr(d,(0,0,W-1,H-1),80,fill=255))
    face=grad_img((W,H),IVORY1,IVORY2)
    # soft vignette
    vg=Image.new('L',(W,H),0); ImageDraw.Draw(vg).rounded_rectangle((20,20,W-20,H-20),70,fill=255)
    vg=vg.filter(ImageFilter.GaussianBlur(40))
    edge=Image.new('RGB',(W,H),(205,190,150))
    face=Image.composite(face,edge,vg)
    img.paste(face,(0,0),mask)
    d=ImageDraw.Draw(img)
    rr(d,(22,22,W-23,H-23),62,outline=GOLD2,width=6)
    rr(d,(34,34,W-35,H-35),52,outline=GOLD1,width=3)
    return img,mask

def finalize(img,mask):
    out=Image.new('RGBA',(W,H),(0,0,0,0)); out.paste(img,(0,0),mask)
    return out.resize(FINAL,Image.LANCZOS)

def corner(img,rank,suit):
    lay=Image.new('RGB',img.size,(0,0,0)); 
    m=Image.new('L',img.size,0)
    col=SUIT_COL[suit]
    d=ImageDraw.Draw(img)
    lab=LABEL[rank]
    sz={'ten':140,'shaib':88,'bint':96,'walad':96,'qass':104}.get(rank,170)
    f=font(sz); bb=d.textbbox((0,0),lab,font=f)
    tw=bb[2]-bb[0]; cx=122
    d.text((cx-tw/2-bb[0],62-bb[1]+8),lab,font=f,fill=col)
    draw_suit(img,suit,cx,300,100)
    # bottom-right copy
    tl=img.crop((38,40,206,360)).rotate(180)
    img.paste(tl,(W-206,H-360))

# ---------- pips ----------
def pips(img,rank,suit):
    n={'seven':7,'eight':8,'nine':9,'ten':10}[rank]
    L,R,C=370,710,540
    rows3=[310,780,1250]; rows4=[300,600,960,1260]
    pos=[]
    if n==7: pos=[(L,y) for y in rows3]+[(R,y) for y in rows3]+[(C,545)]
    if n==8: pos=[(L,y) for y in rows3]+[(R,y) for y in rows3]+[(C,545),(C,1015)]
    if n==9: pos=[(L,y) for y in rows4]+[(R,y) for y in rows4]+[(C,780)]
    if n==10: pos=[(L,y) for y in rows4]+[(R,y) for y in rows4]+[(C,450),(C,1110)]
    for x,y in pos: draw_suit(img,suit,x,y,215,flip=y>H/2+10)
    # tiny arabic motif lines (decor)
    d=ImageDraw.Draw(img)
    for yy in (190,1370):
        d.line((300,yy,780,yy),fill=GOLD2,width=3)
        d.polygon(star8(540,yy,16),fill=GOLD1,outline=GOLD2)

# ---------- court figures ----------
FW,FH=700,590
SKIN=((246,206,170),(214,160,120))
def court_figure(rank,suit):
    p=PAL[suit]
    lay=Image.new('RGB',(FW,FH),p[0]); m_all=Image.new('L',(FW,FH),255)
    bg=grad_img((FW,FH),p[0],p[1]); lay.paste(bg,(0,0))
    lat=lattice((FW,FH),GOLD1,38); lay.paste(lat,(0,0),lat)
    # halo
    halo=Image.new('L',(FW,FH),0); ImageDraw.Draw(halo).ellipse((150,40,550,440),fill=110)
    halo=halo.filter(ImageFilter.GaussianBlur(50)); lay.paste(Image.new('RGB',(FW,FH),(255,230,160)),(0,0),halo)
    cx=350
    # robe / shoulders
    robe=mk((FW,FH),lambda d:(d.polygon([(cx-250,FH),(cx-230,470),(cx-130,400),(cx+130,400),(cx+230,470),(cx+250,FH)],fill=255)))
    paste_grad(lay,robe,p[2],p[3])
    d=ImageDraw.Draw(lay)
    # embroidery trims
    d.line([(cx-130,400),(cx,540),(cx+130,400)],fill=GOLD1,width=9)
    d.line([(cx-120,412),(cx,548),(cx+120,412)],fill=GOLD2,width=3)
    for i in range(-3,4):
        d.polygon(star8(cx+i*60,FH-34,16),fill=GOLD1,outline=GOLD2)
    # neck
    neck=mk((FW,FH),lambda d:d.rectangle((cx-38,320,cx+38,420),fill=255)); paste_grad(lay,neck,SKIN[0],SKIN[1])
    return lay,cx,p

def head(lay,cx,cy,beard=False,young=False,lips=True,brow=(40,28,22)):
    d=ImageDraw.Draw(lay)
    hm=mk(lay.size,lambda dd:(dd.ellipse((cx-78,cy-98,cx+78,cy+100),fill=255),dd.ellipse((cx-86,cy-20,cx-70,cy+30),fill=255),dd.ellipse((cx+70,cy-20,cx+86,cy+30),fill=255)))
    paste_grad(lay,hm,SKIN[0],SKIN[1])
    d=ImageDraw.Draw(lay)
    # eyes
    for s in (-1,1):
        ex=cx+s*34; ey=cy-8
        d.ellipse((ex-17,ey-8,ex+17,ey+8),fill=(255,255,250))
        d.ellipse((ex-8,ey-8,ex+8,ey+8),fill=(46,30,20)); d.ellipse((ex-3,ey-5,ex+1,ey-1),fill=(255,255,255))
        d.arc((ex-22,ey-26,ex+22,ey+2),200,340,fill=brow,width=7)
    d.line([(cx,cy+2),(cx-7,cy+34),(cx+4,cy+36)],fill=(170,110,80),width=4)
    d.arc((cx-24,cy+44,cx+24,cy+76),20,160,fill=(150,50,50),width=6)
    return d

def fig_king(suit):
    lay,cx,p=court_figure('shaib',suit); d=ImageDraw.Draw(lay)
    cy=285
    # beard
    bm=mk(lay.size,lambda dd:dd.pieslice((cx-86,cy-30,cx+86,cy+150),0,180,fill=255)); paste_grad(lay,bm,(210,210,210),(150,150,155))
    head(lay,cx,cy,brow=(70,70,70))
    d=ImageDraw.Draw(lay)
    d.arc((cx-40,cy+36,cx+40,cy+70),10,170,fill=(235,235,235),width=14) # mustache
    # ghutra
    gm=mk(lay.size,lambda dd:(dd.pieslice((cx-100,cy-150,cx+100,cy+40),180,360,fill=255),dd.polygon([(cx-100,cy-55),(cx-170,cy+190),(cx-85,cy+200),(cx-78,cy-10)],fill=255),dd.polygon([(cx+100,cy-55),(cx+170,cy+190),(cx+85,cy+200),(cx+78,cy-10)],fill=255)))
    pat=suit in('hearts','diamonds')
    paste_grad(lay,gm,(255,255,255),(214,214,222))
    if pat:
        chk=Image.new('RGB',lay.size,(0,0,0)); cd=ImageDraw.Draw(chk)
        for xx in range(0,FW,26):
            cd.line((xx,0,xx,FH),fill=(200,40,50),width=4)
        for yy in range(0,FH,26): cd.line((0,yy,FW,yy),fill=(200,40,50),width=4)
        lay.paste(chk,(0,0),ImageChops.multiply(gm,Image.new('L',lay.size,150)))
    d=ImageDraw.Draw(lay)
    d.arc((cx-100,cy-150,cx+100,cy+10),180,360,fill=(190,190,200),width=3)
    # agal
    d.ellipse((cx-104,cy-118,cx+104,cy-66),outline=(20,20,24),width=16)
    d.ellipse((cx-104,cy-118,cx+104,cy-66),outline=GOLD2,width=3)
    return lay

def fig_queen(suit):
    lay,cx,p=court_figure('bint',suit); d=ImageDraw.Draw(lay); cy=285
    hair=mk(lay.size,lambda dd:(dd.ellipse((cx-120,cy-130,cx+120,cy+120),fill=255),dd.polygon([(cx-120,cy),(cx-165,420),(cx-60,440),(cx-60,150)],fill=255),dd.polygon([(cx+120,cy),(cx+165,420),(cx+60,440),(cx+60,150)],fill=255)))
    paste_grad(lay,hair,(52,34,34),(14,8,10))
    head(lay,cx,cy,brow=(30,20,18))
    d=ImageDraw.Draw(lay)
    # fringe
    d.pieslice((cx-84,cy-108,cx+84,cy+10),180,360,fill=(30,18,20))
    # crown
    cm=mk(lay.size,lambda dd:dd.polygon([(cx-100,cy-100),(cx-110,cy-190),(cx-55,cy-150),(cx,cy-215),(cx+55,cy-150),(cx+110,cy-190),(cx+100,cy-100)],fill=255))
    paste_grad(lay,cm,GOLD1,GOLD2); d=ImageDraw.Draw(lay)
    d.line([(cx-100,cy-100),(cx+100,cy-100)],fill=GOLDD,width=6)
    for gx,gy,col in [(cx,cy-150,(200,30,50)),(cx-52,cy-125,(40,150,220)),(cx+52,cy-125,(40,150,220))]:
        d.ellipse((gx-11,gy-11,gx+11,gy+11),fill=col,outline=GOLDD,width=2)
    d.ellipse((cx-9,cy-232,cx+9,cy-214),fill=(255,250,230),outline=GOLDD)
    # earrings + necklace
    for s in (-1,1): d.ellipse((cx+s*88-8,cy+40,cx+s*88+8,cy+66),fill=GOLD1,outline=GOLD2)
    for i,r in enumerate((110,90)):
        d.arc((cx-r,330,cx+r,330+r*1.1),20,160,fill=GOLD1,width=8)
    d.ellipse((cx-12,418,cx+12,446),fill=(200,30,50),outline=GOLDD,width=3)
    return lay

def fig_jack(suit):
    lay,cx,p=court_figure('walad',suit); d=ImageDraw.Draw(lay); cy=290
    hair=mk(lay.size,lambda dd:dd.ellipse((cx-90,cy-110,cx+90,cy+60),fill=255)); paste_grad(lay,hair,(50,34,26),(18,10,8))
    head(lay,cx,cy,brow=(40,26,20))
    d=ImageDraw.Draw(lay); d.pieslice((cx-80,cy-100,cx+80,cy-10),180,360,fill=(40,26,20))
    d.arc((cx-34,cy+42,cx+34,cy+66),15,165,fill=(60,40,30),width=8)
    # turban/cap
    tm=mk(lay.size,lambda dd:(dd.ellipse((cx-100,cy-170,cx+100,cy-30),fill=255),dd.rectangle((cx-98,cy-86,cx+98,cy-56),fill=255)))
    paste_grad(lay,tm,(220,50,60),(130,16,30)); d=ImageDraw.Draw(lay)
    d.rectangle((cx-100,cy-90,cx+100,cy-62),fill=GOLD1,outline=GOLDD,width=3)
    for i in range(-4,5): d.ellipse((cx+i*22-5,cy-82,cx+i*22+5,cy-72),fill=(200,30,50))
    for k in range(-2,3): d.arc((cx-100+k*18,cy-170,cx+100-k*18,cy-60),200,340,fill=(255,120,120),width=3)
    d.polygon([(cx+90,cy-60),(cx+135,cy+20),(cx+100,cy+30),(cx+70,cy-40)],fill=(190,36,50))
    # feather
    d.polygon([(cx,cy-170),(cx-14,cy-215),(cx+6,cy-250),(cx+22,cy-205)],fill=(250,250,245),outline=GOLD2)
    d.ellipse((cx-9,cy-172,cx+9,cy-154),fill=(40,150,220),outline=GOLDD,width=2)
    return lay

def court_card(rank,suit):
    img,mask=base_card(); d=ImageDraw.Draw(img)
    # panel
    px0,py0,px1,py1=196,196,884,1364
    pm=mk((W,H),lambda dd:rr(dd,(px0,py0,px1,py1),34,fill=255))
    fig={'shaib':fig_king,'bint':fig_queen,'walad':fig_jack}[rank](suit)
    half=(px1-px0, (py1-py0)//2)
    top=fig.resize(half,Image.LANCZOS)
    pan=Image.new('RGB',(px1-px0,py1-py0),(0,0,0))
    pan.paste(top,(0,0)); pan.paste(top.rotate(180),(0,half[1]))
    # centre band
    pd=ImageDraw.Draw(pan); my=half[1]
    pd.rectangle((0,my-10,pan.size[0],my+10),fill=GOLD2); pd.rectangle((0,my-5,pan.size[0],my+5),fill=GOLD1)
    pd.polygon(star8(pan.size[0]//2,my,34),fill=GOLD1,outline=GOLDD)
    sx=pan.size[0]//2
    sm=mk(pan.size,lambda dd:None)
    pm_crop=pm.crop((px0,py0,px1,py1)); img.paste(pan,(px0,py0),pm_crop)
    d=ImageDraw.Draw(img)
    rr(d,(px0,py0,px1,py1),34,outline=GOLD2,width=7); rr(d,(px0+10,py0+10,px1-10,py1-10),26,outline=GOLD1,width=3)
    # suit emblem on midline
    draw_suit(img,suit,(px0+px1)//2,(py0+py1)//2,90,color=SUIT_COL[suit] if False else None) if False else None
    corner(img,rank,suit)
    return finalize(img,mask)

def ace_card(suit):
    img,mask=base_card(); d=ImageDraw.Draw(img)
    cx,cy=540,H//2
    p=PAL[suit]
    # medallion
    mm=mk((W,H),lambda dd:dd.polygon(star8(cx,cy,470),fill=255)); paste_grad(img,mm,p[0],p[1])
    lat=lattice((W,H),GOLD1,50); img.paste(lat,(0,0),ImageChops.multiply(mm,Image.new('L',(W,H),255)))
    d=ImageDraw.Draw(img); d.polygon(star8(cx,cy,470),outline=GOLD2,width=8); d.polygon(star8(cx,cy,452),outline=GOLD1,width=3)
    cm=mk((W,H),lambda dd:dd.ellipse((cx-300,cy-300,cx+300,cy+300),fill=255)); paste_grad(img,cm,IVORY1,IVORY2)
    d=ImageDraw.Draw(img); d.ellipse((cx-300,cy-300,cx+300,cy+300),outline=GOLD2,width=8); d.ellipse((cx-284,cy-284,cx+284,cy+284),outline=GOLD1,width=3)
    draw_suit(img,suit,cx,cy+8,420)
    # crowns top/bottom
    def crown(x,y,flip=False):
        c=Image.new('L',(W,H),0); cd=ImageDraw.Draw(c)
        cd.polygon([(x-90,y),(x-100,y-80),(x-45,y-40),(x,y-100),(x+45,y-40),(x+100,y-80),(x+90,y)],fill=255)
        if flip: c=c.rotate(180,center=(x,y-40))
        paste_grad(img,c,GOLD1,GOLD2)
    crown(cx,cy-310); crown(cx,cy+360,flip=True)
    corner(img,'qass',suit)
    return finalize(img,mask)

def number_card(rank,suit):
    img,mask=base_card(); pips(img,rank,suit); corner(img,rank,suit); return finalize(img,mask)

# ---------- back ----------
def back_card():
    img=Image.new('RGB',(W,H),(0,0,0)); mask=mk((W,H),lambda d:rr(d,(0,0,W-1,H-1),80,fill=255))
    bg=grad_img((W,H),(14,92,60),(5,42,28)); img.paste(bg,(0,0),mask)
    lat=lattice((W,H),GOLD1,120,step=120); img.paste(lat,(0,0),lat)
    lat2=lattice((W,H),(255,255,255),20,step=60); img.paste(lat2,(0,0),lat2)
    d=ImageDraw.Draw(img)
    # inner field masks the pattern edges
    for inset,wd,col in [(22,6,GOLD2),(34,3,GOLD1),(70,4,GOLD2),(82,2,GOLD1)]:
        rr(d,(inset,inset,W-inset,H-inset),max(20,70-inset//2),outline=col,width=wd)
    cx,cy=540,H//2
    mm=mk((W,H),lambda dd:dd.polygon(star8(cx,cy,400),fill=255)); paste_grad(img,mm,(8,56,36),(4,28,20))
    d=ImageDraw.Draw(img); d.polygon(star8(cx,cy,400),outline=GOLD1,width=7); d.polygon(star8(cx,cy,380),outline=GOLD2,width=3)
    d.ellipse((cx-250,cy-250,cx+250,cy+250),outline=GOLD1,width=6); d.ellipse((cx-232,cy-232,cx+232,cy+232),outline=GOLD2,width=3)
    f=font(150); t='مداقشة'; bb=d.textbbox((0,0),t,font=f); tw=bb[2]-bb[0]
    # gold text with shadow
    d.text((cx-tw/2-bb[0]+5,cy+30+5),t,font=f,fill=(0,0,0))
    tm=Image.new('L',(W,H),0); ImageDraw.Draw(tm).text((cx-tw/2-bb[0],cy+30),t,font=f,fill=255)
    paste_grad(img,tm,GOLD1,GOLD2)
    # crown
    c=Image.new('L',(W,H),0); cd=ImageDraw.Draw(c); x,y=cx,cy-30
    cd.polygon([(x-110,y),(x-125,y-100),(x-55,y-50),(x,y-125),(x+55,y-50),(x+125,y-100),(x+110,y)],fill=255)
    cd.rectangle((x-110,y+6,x+110,y+30),fill=255)
    paste_grad(img,c,GOLD1,GOLD2)
    d=ImageDraw.Draw(img)
    for gx in (x-125,x,x+125): d.ellipse((gx-12,y-135 if gx==x else y-110,gx+12,y-111 if gx==x else y-86),fill=(255,250,230))
    # four suits at corners of medallion
    for (sx,sy),su in zip([(cx-300,cy-420),(cx+300,cy-420),(cx-300,cy+420),(cx+300,cy+420)],SUITS): pass
    return finalize(img,mask)

if __name__=='__main__':
    out='app/src/main/res/drawable-nodpi/'
    os.makedirs(out,exist_ok=True)
    allc=[]
    for s in SUITS:
        for r in RANKS:
            c=ace_card(s) if r=='qass' else court_card(r,s) if r in('shaib','bint','walad') else number_card(r,s)
            c.save(out+f'card_{r}_{s}.png',optimize=True); allc.append(c)
    b=back_card(); b.save(out+'card_back.png',optimize=True)
    sheet=Image.new('RGB',(8*430,4*617+0),(11,61,36))
    for i,c in enumerate(allc): sheet.paste(c,((i%8)*430+5,(i//8)*617+5),c)
    sheet.save('/tmp/cards_sheet.png'); 
    bs=Image.new('RGB',(900,640),(11,61,36)); bs.paste(b,(10,10),b); bs.save('/tmp/back.png')
    print('done',len(allc))
