"""Actual Tech Reborn windows and immutable SIU-free game references."""
from pathlib import Path
from PIL import Image
import json,hashlib,importlib.util,math
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from pypdf import PdfReader
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/tech-reborn-implementation';PDF=ROOT/'output/pdf/siu-tech-reborn-implemented-comparisons.pdf'
DESIGN=ROOT/'output/tech-reborn-window-design'
N=Image.Resampling.NEAREST
W,H=1191,842

def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def read(p):return json.loads(Path(p).read_text())
def crop_window(row):
    s=row['state'];w=s['allWindows'][-1];b=w['bounds'];rects=[(0,0,b['width'],b['height'])]
    for r in w['composition']['regions']:
        if r.get('exterior'):rects.append((r['x'],r['y'],r['x']+r['source']['width'],r['y']+r['source']['height']))
    st=w['templateState']
    if st.get('upgrades'):rects.append((b['width']-4,24,b['width']+24,106))
    if st.get('popupHeight',0)>0:rects.append((b['width']+28,24,b['width']+132,st['popupHeight']+47))
    x0=b['x']+min(r[0]for r in rects)-8;y0=b['y']+min(r[1]for r in rects)-8
    x1=b['x']+max(r[2]for r in rects)+8;y1=b['y']+max(r[3]for r in rects)+8
    raw=Image.open(ROOT/row['capture']).convert('RGB');assert sha(ROOT/row['capture'])==row['sha256']
    return raw.crop(tuple(v*2 for v in (x0,y0,x1,y1)))

def original(row):
    b=row['state']['bounds'];raw=Image.open(ROOT/row['capture']).convert('RGB');assert sha(ROOT/row['capture'])==row['sha256']
    return raw.crop((max(0,(b['x']-29)*2),max(0,(b['y']-28)*2),(b['x']+b['width']+9)*2,(b['y']+176+9)*2))

def main():
    native={r['id']:r for r in read(DESIGN/'fresh-native.json')}
    captures={r['id']:r for r in read(OUT/'captures.json')}
    for r in read(OUT/'functional-captures.json'):captures[r['id']]=r
    designs=read(DESIGN/'manifest.json')['pages'];assert len(designs)==58
    checks=read(OUT/'focused-checks.json')
    assert checks['status']=='passed'
    images=OUT/'images';images.mkdir(exist_ok=True)
    pages=[]
    for p in designs:
        key=p['id'];assert key in captures,key
        a=Image.open(DESIGN/'images'/(key+'-native.png')).convert('RGB');b=crop_window(captures[key]);a.save(images/(key+'-native.png'));b.save(images/(key+'-window.png'))
        pages.append(dict(id=key,title=p['title'],description=p['does'],screen=p['screen'],kind=p['kind'],original_sha256=native[key]['sha256'],implemented_sha256=captures[key]['sha256']))
    for r in native.values():
        if r.get('variant')!='unformed':continue
        key=r['id'];assert key in captures,key
        original(r).save(images/(key+'-native.png'));crop_window(captures[key]).save(images/(key+'-window.png'))
        pages.append(dict(id=key,title=r['state']['title']+' - Incomplete structure',description='The native warning and hologram action stay available. The formed machine comparison appears in the main section.',screen=r['state']['screen'].split('.')[-1],kind='unformed',original_sha256=r['sha256'],implemented_sha256=captures[key]['sha256']))
    pdfmetrics.registerFont(TTFont('Segoe','C:/Windows/Fonts/segoeui.ttf'));pdfmetrics.registerFont(TTFont('SegoeB','C:/Windows/Fonts/segoeuib.ttf'))
    c=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1);c.setTitle('Tech Reborn - Implemented SIU Windows');c.setAuthor("Salt's Inventory Update")
    def text(s,x,y,size=12,bold=False,color='#213C46'):
        assert x>=25 and y>=20 and x+pdfmetrics.stringWidth(s,'SegoeB'if bold else'Segoe',size)<=W-25,(s,x,y)
        c.setFont('SegoeB'if bold else'Segoe',size);c.setFillColor(HexColor(color));c.drawString(x,y,s)
    def para(s,x,y,width,size=12,leading=18):
        line=''
        for word in s.split():
            t=(line+' '+word).strip()
            if pdfmetrics.stringWidth(t,'Segoe',size)>width:text(line,x,y,size);y-=leading;line=word
            else:line=t
        if line:text(line,x,y,size);y-=leading
        return y
    def base(title):
        text('PROJECT EVERYTHING / TECH REBORN / IMPLEMENTED WINDOWS',36,H-34,10,True,'#247C80')
        text(title,36,H-78,25,True);text('Minecraft 1.20.1 / Fabric / Tech Reborn 5.8.14 + Reborn Core 5.8.14',36,H-102,11,color='#60727B')
        c.setStrokeColor(HexColor('#D6E3E6'));c.line(36,42,W-36,42)
        text('Actual game captures / Original UI and installed SIU compatibility',36,25,9,color='#60727B');text(str(c.getPageNumber()),W-55,25,9,color='#60727B')
    def panel(x,y,w,h):c.setFillColor(HexColor('#EFF5F5'));c.roundRect(x,y,w,h,14,fill=1,stroke=0)
    def picture(p,x,y,w,h,scale=None):
        im=Image.open(p).convert('RGB');lw,lh=im.width/2,im.height/2
        sc=min(w/lw,h/lh)if scale is None else min(scale,w/lw,h/lh)
        c.drawImage(ImageReader(im.resize((im.width*2,im.height*2),N)),x+(w-lw*sc)/2,y+(h-lh*sc)/2,lw*sc,lh*sc)
    base('Tech Reborn, now on the SIU desktop')
    text('51 MACHINE SCREENS / NATIVE SETTINGS / RIGHT UPGRADE TRAYS',36,686,14,True,'#247C80')
    para('The approved designs are implemented. Native machine artwork and slot behavior remain intact. Settings tabs connect directly to the left of the window; upgrade wells are attached to the right.',36,650,1115,15,23)
    for i,key in enumerate(['techreborn-alloy-smelter','techreborn-industrial-blast-furnace','pr-techreborn-adjustable-su']):
        x=36+i*379;panel(x,255,361,300);picture(images/(key+'-window.png'),x+10,270,341,270,1.6)
    text('Attached tabs and upgrades',48,232,14,True);text('Formed multiblock',427,232,14,True);text('Equipment and native controls',806,232,14,True)
    para('All comparisons use actual screenshots. Source images are preserved with hashes. Machine views, configuration inspectors, populated storage and incomplete multiblocks are included.',36,188,540,12)
    para('The compatibility jar contains this implementation. Core and the public API are unchanged. Checks focus on the affected windows and representative native actions.',615,188,540,12)
    para('Scope: the approved 1.20.1 Fabric design set. Other releases and loaders are not claimed by this report. The manual remains a normal native book screen.',36,97,1115,11,16);c.showPage()
    base('Complete machine index')
    text('Each registered screen has a comparison page. Tiers sharing a screen use its same window layout.',36,686,12)
    for i,p in enumerate(pages[:51]):
        col=i//17;row=i%17;x=36+col*379;y=650-row*30
        text(f'{i+1:02}  {p["title"]}',x,y,12);text(str(3+i),x+335,y,11,color='#60727B');c.linkAbsolute(p['title'],p['id'],Rect=(x,y-3,x+355,y+16),thickness=0)
    para('The shared settings and two populated examples follow. The appendix compares all eight incomplete structures and describes the focused verification and its limits.',36,106,1115,12);c.showPage()
    for p in pages:
        key=p['id'];c.bookmarkPage(key);c.addOutlineEntry(p['title'],key,level=0,closed=False);base(p['title'])
        left,right,pw,py,ph=36,610,545,210,460
        text('ORIGINAL IN GAME / SIU ABSENT',left,690,11,True,'#247C80');text('IMPLEMENTED SIU WINDOW / IN GAME',right,690,11,True,'#247C80')
        panel(left,py,pw,ph);panel(right,py,pw,ph)
        a=images/(key+'-native.png');b=images/(key+'-window.png');ia,ib=Image.open(a),Image.open(b)
        sc=min(2.2,(pw-24)/max(ia.width/2,ib.width/2),(ph-24)/max(ia.height/2,ib.height/2))
        picture(a,left+12,py+12,pw-24,ph-24,sc);picture(b,right+12,py+12,pw-24,ph-24,sc)
        y=para(p['description'],36,181,1115,12,18);assert y>85
        if key=='techreborn-industrial-electrolyzer':text('The original reference includes an input-slot hover highlight.',36,95,10,color='#60727B')
        text(p['screen']+' / Equal GUI-pixel magnification / Native slot identities retained',36,72,10,color='#60727B');c.showPage()
    base('Focused verification and coverage')
    text('What was checked',36,685,20,True)
    y=656
    for line in checks['summary']:
        y=para('- '+line,36,y,1115,13,20)-9
    text('Limits and preserved native screens',36,y-5,18,True)
    y=para('These are focused UI and representative-action checks. They are not a complete Tech Reborn recipe, automation or gameplay suite. The manual and external link prompts keep their normal native screens. The unused DestructoPack handler has no registered reachable UI in this release.',36,y-36,1115,12,18)
    y=para('The target compatibility remap build passed. The broader repository build reaches a pre-existing module-boundary check failure in other integrations; this adapter uses the public SIU API only. Core/API and unrelated release artifacts were checked for unchanged hashes.',36,y-15,1115,12,18)
    text('Publisher source: TechReborn/TechReborn / tag 5.8.14 / 380a06253bea',36,85,10,color='#60727B')
    c.linkURL('https://github.com/TechReborn/TechReborn/tree/380a06253bea70924a2470454875cbea99682a0a',(36,72,930,103),relative=0,thickness=0)
    c.showPage();c.save()
    r=PdfReader(PDF);assert len(r.pages)==69,len(r.pages)
    manifest=dict(status='created; render review required',pages=len(r.pages),machines=51,shared_settings=5,populated_examples=2,unformed_examples=8,comparisons=pages,pdf_sha256=sha(PDF))
    (OUT/'report-manifest.json').write_text(json.dumps(manifest,indent=2));print(json.dumps({k:v for k,v in manifest.items()if k!='comparisons'},indent=2))
if __name__=='__main__':main()
