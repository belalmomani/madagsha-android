import math, os
from PIL import Image, ImageDraw, ImageFilter, ImageChops
S=1024  # canvas; final 512
def grad(size,c1,c2):
    g=Image.linear_gradient('L').resize(size)
    return Image.composite(Image.new('RGB',size,c2),Image.new('RGB',size,c1),g)
def mk(fn,blur=0):
    m=Image.new('L',(S,S),0); fn(ImageDraw.Draw(m))
    return m.filter(ImageFilter.GaussianBlur(blur)) if blur else m
def fill(layer,mask,c1,c2=None):
    c2=c2 or c1; bb=mask.getbbox()
    if not bb: return
    x0,y0,x1,y1=bb; gi=grad((x1-x0,y1-y0),c1,c2); full=Image.new('RGB',(S,S),c1); full.paste(gi,(x0,y0))
    layer.paste(full,(0,0),mask)
def shade(layer,mask,region,col,alpha):
    m=ImageChops.multiply(mask,region.point(lambda v:int(v*alpha/255)))
    layer.paste(Image.new('RGB',(S,S),col),(0,0),m)

def draw_person(P):
    bg=Image.new('RGB',(S,S)); 
    # background: dark green radial with soft light
    g=grad((S,S),(22,86,58),(6,36,26)); bg.paste(g)
    glow=Image.new('L',(S,S),0); ImageDraw.Draw(glow).ellipse((150,60,874,800),fill=120); glow=glow.filter(ImageFilter.GaussianBlur(120))
    bg.paste(Image.new('RGB',(S,S),(70,150,100)),(0,0),glow)
    fig=Image.new('RGB',(S,S),(0,0,0)); figmask=Image.new('L',(S,S),0)
    def add(m,c1,c2=None):
        nonlocal figmask
        fill(fig,m,c1,c2); figmask=ImageChops.lighter(figmask,m)
    cx=512; skin=P['skin']; sk2=tuple(int(v*0.78) for v in skin); sk3=tuple(int(v*0.62) for v in skin)
    hw=P.get('hw',185); hh=P.get('hh',235); cy=P.get('cy',455); jaw=P.get('jaw',0.82)
    # --- clothing (shoulders)
    cl=P['cloth']
    add(mk(lambda d:d.ellipse((cx-400,760,cx+400,1300),fill=255)),cl[0],cl[1])
    if P.get('collar')=='thobe':
        add(mk(lambda d:d.polygon([(cx-60,740),(cx+60,740),(cx+40,900),(cx-40,900)],fill=255)),(245,245,245),(215,215,220))
    # --- neck
    nk=mk(lambda d:d.rounded_rectangle((cx-85,cy+120,cx+85,cy+330),40,fill=255))
    add(nk,skin,sk2)
    # neck shadow under chin
    ns=mk(lambda d:d.ellipse((cx-140,cy+160,cx+140,cy+300),fill=255),30)
    shade(fig,ImageChops.multiply(ns,nk),Image.new('L',(S,S),255),sk3,140)
    if P.get('collar')=='crew': add(mk(lambda d:d.arc((cx-130,cy+250,cx+130,cy+400),0,180,fill=255,width=36)),(240,240,240))
    # --- back hair / hijab back
    if P.get('hair_back'): P['hair_back'](add,cx,cy)
    # --- head
    head=mk(lambda d:(d.ellipse((cx-hw,cy-hh,cx+hw,cy+hh*0.75),fill=255),d.ellipse((cx-hw*jaw,cy+hh*0.1,cx+hw*jaw,cy+hh*1.0),fill=255),
                      d.ellipse((cx-hw-16,cy-30,cx-hw+18,cy+60),fill=255),d.ellipse((cx+hw-18,cy-30,cx+hw+16,cy+60),fill=255)))
    add(head,skin,sk2)
    # --- face lighting: light from upper-left
    lit=mk(lambda d:d.ellipse((cx-hw*0.9,cy-hh*0.8,cx+hw*0.1,cy+hh*0.2),fill=255),60)
    shade(fig,head,lit,(255,235,210),70)
    sh=mk(lambda d:d.ellipse((cx+hw*0.25,cy-hh*0.6,cx+hw*1.5,cy+hh*1.3),fill=255),55)
    shade(fig,head,sh,(70,30,20),95)
    # cheeks blush
    for s in (-1,1):
        bm=mk(lambda d:d.ellipse((cx+s*100-45,cy+60,cx+s*100+45,cy+130),fill=255),28)
        shade(fig,head,bm,P.get('blush',(220,110,100)),55)
    d=ImageDraw.Draw(fig)
    # --- beard
    if P.get('beard'):
        bc=P['beard']
        bm=mk(lambda dd:(dd.ellipse((cx-hw*0.99,cy+50,cx+hw*0.99,cy+hh*1.14),fill=255),dd.ellipse((cx-hw*0.62,cy+30,cx+hw*0.62,cy+138),fill=0)),4)
        bm=ImageChops.multiply(bm,head.filter(ImageFilter.MaxFilter(3)))
        # soften
        add(bm.filter(ImageFilter.GaussianBlur(2)),bc,tuple(int(v*0.6) for v in bc)); d=ImageDraw.Draw(fig)
    # --- eyes
    ey=cy-10; er=P.get('eye',(60,38,24)); fem=P.get('fem',False)
    for s in (-1,1):
        ex=cx+s*72
        d.ellipse((ex-38,ey-17,ex+38,ey+17),fill=(250,248,244))
        d.ellipse((ex-17,ey-17,ex+17,ey+17),fill=er); d.ellipse((ex-8,ey-8,ex+8,ey+8),fill=(10,8,8)); d.ellipse((ex-9,ey-12,ex-1,ey-4),fill=(255,255,255))
        d.arc((ex-42,ey-34,ex+42,ey+14),198,342,fill=(30,18,14),width=9 if fem else 6)
        d.arc((ex-38,ey-12,ex+38,ey+30),15,165,fill=sk3,width=3)
        bw=P.get('brow',(40,28,22)); th=P.get('brow_th',13)
        d.arc((ex-50,ey-72+(6 if fem else 0),ex+50,ey-14),205,335,fill=bw,width=th)
        if fem: 
            for k in range(5): 
                a=math.radians(200+k*34); d.line((ex+40*math.cos(a)*s*-1*-1,ey-16+0,ex+52*math.cos(a)*s,ey-30),fill=(30,18,14),width=3)
    # nose
    nm=mk(lambda dd:dd.polygon([(cx-10,ey+10),(cx-34,ey+118),(cx+34,ey+118),(cx+10,ey+10)],fill=255),12)
    shade(fig,head,nm,sk3,60)
    d=ImageDraw.Draw(fig)
    d.ellipse((cx-26,ey+100,cx-8,ey+118),fill=sk3); d.ellipse((cx+8,ey+100,cx+26,ey+118),fill=sk3)
    d.ellipse((cx-8,ey+78,cx+8,ey+96),fill=tuple(min(255,v+25) for v in skin))
    # mouth
    my=ey+176; lip=P.get('lip',(168,70,70))
    d.polygon([(cx-52,my),(cx-20,my-17),(cx,my-10),(cx+20,my-17),(cx+52,my),(cx,my+6)],fill=lip)
    d.polygon([(cx-50,my+2),(cx,my+8),(cx+50,my+2),(cx+30,my+32),(cx,my+38),(cx-30,my+32)],fill=tuple(min(255,int(v*1.12)) for v in lip))
    d.line((cx-52,my,cx,my+7,cx+52,my),fill=(80,30,30),width=4)
    if P.get('smile'): d.arc((cx-62,my-30,cx+62,my+40),20,160,fill=(80,30,30),width=3)
    if P.get('stache'):
        sc=P['stache']; d.pieslice((cx-70,my-40,cx+70,my+6),0,180,fill=sc) if False else d.polygon([(cx-70,my-6),(cx-36,my-34),(cx,my-26),(cx+36,my-34),(cx+70,my-6),(cx+30,my-14),(cx,my-12),(cx-30,my-14)],fill=sc)
        d=ImageDraw.Draw(fig)
    # hair/headwear
    globals()['return_layer']=fig
    if P.get('front'): P['front'](add,cx,cy,hw,hh)
    d=ImageDraw.Draw(fig)
    if P.get('glasses'):
        for s in (-1,1): d.ellipse((cx+s*72-62,ey-46,cx+s*72+62,ey+46),outline=(30,30,36),width=9)
        d.line((cx-10,ey-4,cx+10,ey-4),fill=(30,30,36),width=8)
    if P.get('earring'):
        for s in (-1,1): d.ellipse((cx+s*(hw+4)-12,cy+68,cx+s*(hw+4)+12,cy+92),fill=(240,200,80))
    # global light/vignette on figure
    light=Image.new('L',(S,S),0); ImageDraw.Draw(light).ellipse((-200,-200,700,700),fill=60); light=light.filter(ImageFilter.GaussianBlur(150))
    fig.paste(Image.new('RGB',(S,S),(255,240,215)),(0,0),ImageChops.multiply(light,figmask))
    dark=Image.new('L',(S,S),0); ImageDraw.Draw(dark).ellipse((420,380,1500,1500),fill=90); dark=dark.filter(ImageFilter.GaussianBlur(140))
    fig.paste(Image.new('RGB',(S,S),(10,10,20)),(0,0),ImageChops.multiply(dark,figmask))
    # rim light (gold) on right edge
    rim=ImageChops.subtract(figmask.filter(ImageFilter.GaussianBlur(5)),figmask.transform((S,S),Image.AFFINE,(1,0,18,0,1,0)).filter(ImageFilter.GaussianBlur(5)))
    fig.paste(Image.new('RGB',(S,S),(255,215,120)),(0,0),rim.point(lambda v:int(min(255,v*1.0)*0.55)))
    # soft drop shadow behind figure
    shadow=figmask.filter(ImageFilter.GaussianBlur(24)).point(lambda v:int(v*0.5))
    bg.paste(Image.new('RGB',(S,S),(0,12,6)),(18,10),shadow)
    bg.paste(fig,(0,0),figmask)
    return bg

# ---------- hair / headwear builders ----------
def ghutra(white=True,check=(190,30,40)):
    def front(add,cx,cy,hw,hh):
        top=mk(lambda d:(d.ellipse((cx-hw-30,cy-hh-70,cx+hw+30,cy+20),fill=255),d.rectangle((cx-hw-30,cy-hh*0.35,cx+hw+30,cy-hh*0.1),fill=0)))
        drape=mk(lambda d:(d.polygon([(cx-hw-25,cy-120),(cx-hw-120,cy+420),(cx-hw+60,cy+450),(cx-hw+20,cy-20)],fill=255),d.polygon([(cx+hw+25,cy-120),(cx+hw+120,cy+420),(cx+hw-60,cy+450),(cx+hw-20,cy-20)],fill=255)))
        m=ImageChops.lighter(top,drape)
        # carve face opening
        face=mk(lambda d:d.ellipse((cx-hw*0.98,cy-hh*0.55,cx+hw*0.98,cy+hh*1.05),fill=255))
        m=ImageChops.subtract(m,face)
        base=(250,250,250) if white else (252,246,246); b2=(206,206,216)
        add(m,base,b2)
        if not white:
            pat=Image.new('RGB',(S,S),(0,0,0)); pd=ImageDraw.Draw(pat)
            for x in range(0,S,34): pd.line((x,0,x,S),fill=check,width=7)
            for y in range(0,S,34): pd.line((0,y,S,y),fill=check,width=7)
            return_layer.paste(pat,(0,0),ImageChops.multiply(m,Image.new('L',(S,S),170)))
        # agal
        ag=mk(lambda d:d.ellipse((cx-hw-26,cy-hh-6,cx+hw+26,cy-hh*0.5),fill=255)); inner=mk(lambda d:d.ellipse((cx-hw-6,cy-hh+22,cx+hw+6,cy-hh*0.5+8),fill=255))
        add(ImageChops.subtract(ag,inner),(20,20,24),(60,60,66))
    return front
def hijab(col1,col2):
    def back(add,cx,cy):
        add(mk(lambda d:(d.ellipse((cx-250,cy-290,cx+250,cy+260),fill=255),d.polygon([(cx-250,cy),(cx-330,860),(cx+330,860),(cx+250,cy)],fill=255))),col1,col2)
    def front(add,cx,cy,hw,hh):
        face=mk(lambda d:d.ellipse((cx-hw*0.88,cy-hh*0.62,cx+hw*0.88,cy+hh*1.02),fill=255))
        wrap=mk(lambda d:(d.ellipse((cx-hw-60,cy-hh-30,cx+hw+60,cy+hh*0.95),fill=255),d.polygon([(cx-hw-60,cy+60),(cx-hw-190,880),(cx+hw+190,880),(cx+hw+60,cy+60)],fill=255)))
        under=mk(lambda d:d.polygon([(cx-hw*0.8,cy+hh*0.7),(cx-hw-120,900),(cx+hw+120,900),(cx+hw*0.8,cy+hh*0.7)],fill=255))
        m=ImageChops.subtract(ImageChops.lighter(wrap,under),face)
        add(m,col1,col2)
        # fold highlights
        hl=mk(lambda d:(d.arc((cx-hw-50,cy-hh,cx+hw+50,cy+hh*0.9),200,260,fill=255,width=26)),10)
        return_layer_light(add,m,hl)
    return back,front
def return_layer_light(add,m,hl): pass
return_layer=None

def hair_short(col,fringe=True,curly=False):
    def front(add,cx,cy,hw,hh):
        if curly:
            m=mk(lambda d:[d.ellipse((cx+dx-60,cy-hh-30+dy,cx+dx+60,cy-hh+90+dy),fill=255) for dx,dy in [(-150,40),(-80,-10),(0,-30),(80,-10),(150,40),(-175,110),(175,110),(-120,60),(120,60)]]+[d.ellipse((cx-hw-10,cy-hh+10,cx+hw+10,cy-40),fill=255)])
        else:
            m=mk(lambda d:(d.ellipse((cx-hw-12,cy-hh-34,cx+hw+12,cy+10),fill=255)))
        face=mk(lambda d:d.ellipse((cx-hw*0.9,cy-hh*0.4,cx+hw*0.9,cy+hh*1.0),fill=255)) if fringe else mk(lambda d:d.ellipse((cx-hw*0.9,cy-hh*0.55,cx+hw*0.9,cy+hh*1.0),fill=255))
        m=ImageChops.subtract(m,face)
        add(m,tuple(min(255,v+40) for v in col),col)
    return front
def hair_long(col,wavy=True):
    def back(add,cx,cy):
        add(mk(lambda d:(d.ellipse((cx-230,cy-280,cx+230,cy+240),fill=255),d.polygon([(cx-230,cy-20),(cx-300,900),(cx-60,880),(cx-80,cy+100)],fill=255),d.polygon([(cx+230,cy-20),(cx+300,900),(cx+60,880),(cx+80,cy+100)],fill=255))),tuple(min(255,v+30) for v in col),col)
    def front(add,cx,cy,hw,hh):
        m=mk(lambda d:(d.ellipse((cx-hw-14,cy-hh-30,cx+hw+14,cy+20),fill=255)))
        face=mk(lambda d:d.ellipse((cx-hw*0.9,cy-hh*0.55,cx+hw*0.9,cy+hh*1.0),fill=255))
        # side-parted fringe
        m=ImageChops.subtract(m,face)
        add(m,tuple(min(255,v+40) for v in col),col)
    return back,front
def cap(col):
    def front(add,cx,cy,hw,hh):
        crown=mk(lambda d:d.pieslice((cx-hw-14,cy-hh-60,cx+hw+14,cy+hh*0.1),180,360,fill=255))
        brim=mk(lambda d:d.ellipse((cx-hw-70,cy-hh*0.5,cx+hw+70,cy-hh*0.28),fill=255))
        add(crown,col,tuple(int(v*0.6) for v in col)); add(brim,tuple(int(v*0.85) for v in col),tuple(int(v*0.5) for v in col))
    return front

def bake(i,P):
    img=draw_person(P).crop((120,100,904,884)).resize((512,512),Image.LANCZOS)
    m=Image.new('L',(512,512),0); ImageDraw.Draw(m).ellipse((0,0,511,511),fill=255)
    o=Image.new('RGBA',(512,512),(0,0,0,0)); o.paste(img,(0,0),m)
    o.save(f'app/src/main/res/drawable-nodpi/avatar_{i}.png',optimize=True); return o

hb1,hf1=hijab((18,18,22),(50,50,58)); hb2,hf2=hijab((130,26,56),(80,12,36))
hlb,hlf=hair_long((92,52,30))
PEOPLE=[
 dict(skin=(226,176,138),cloth=((250,250,250),(214,214,222)),collar='thobe',front=ghutra(False),beard=(36,26,22),stache=(30,22,18),smile=True,jaw=0.8),
 dict(skin=(200,150,112),cloth=((246,246,246),(210,210,216)),collar='thobe',front=ghutra(True),beard=(214,214,216),stache=(226,226,228),brow=(190,190,192),hh=240,hw=190,jaw=0.78),
 dict(skin=(238,196,160),cloth=((20,20,26),(48,48,56)),front=hf1,hair_back=hb1,fem=True,lip=(182,66,82),eye=(70,44,26),earring=True,smile=True,jaw=0.74,hw=170),
 dict(skin=(232,184,148),cloth=((60,100,150),(30,60,100)),collar='crew',front=hlf,hair_back=hlb,fem=True,lip=(196,74,88),eye=(40,90,70),jaw=0.72,hw=168,smile=True),
 dict(skin=(150,102,72),cloth=((38,70,50),(22,42,30)),collar='crew',front=cap((30,40,60)),beard=(26,20,18),stache=(26,20,18),jaw=0.86,hw=190,blush=(160,80,60)),
 dict(skin=(214,166,128),cloth=((40,44,60),(22,24,36)),front=hair_short((34,26,22)),glasses=True,stache=(40,30,26),jaw=0.84,hw=182,smile=True),
 dict(skin=(222,176,140),cloth=((110,60,120),(70,34,84)),front=hf2,hair_back=hb2,fem=True,lip=(160,60,70),brow=(120,110,110),eye=(80,56,34),earring=True,jaw=0.78,hw=176),
 dict(skin=(120,80,56),cloth=((170,60,40),(110,34,24)),front=hair_short((16,12,12),fringe=True,curly=True),stache=None,jaw=0.82,hw=178,blush=(140,60,50),smile=True),
]
if __name__=='__main__':
    sheet=Image.new('RGB',(4*520,2*520),(11,61,36))
    for i,P in enumerate(PEOPLE,1):
        o=bake(i,P); sheet.paste(o,(((i-1)%4)*520+4,((i-1)//4)*520+4),o)
    sheet.save('/tmp/av_sheet.png')
    sm=Image.new('RGB',(8*70,70),(11,61,36))
    for i in range(8):
        a=Image.open(f'app/src/main/res/drawable-nodpi/avatar_{i+1}.png').resize((64,64),Image.LANCZOS); sm.paste(a,(i*70+3,3),a)
    sm.resize((8*140,140)).save('/tmp/av_small.png'); print('ok')
