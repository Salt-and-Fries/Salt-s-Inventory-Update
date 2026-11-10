"""Publish the independent screenshot review in a short, visually checked PDF."""
from pathlib import Path
from xml.sax.saxutils import escape
import json,math
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import landscape,A4
from reportlab.lib.colors import HexColor
from reportlab.platypus import Paragraph
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'output/villager-trading-review'
PDF=ROOT/'output/pdf/siu-villager-trading-independent-visual-review.pdf'
W,H=landscape(A4);M=34;CW=W-2*M
INK=HexColor('#20383c');MUTED=HexColor('#63767a');ACCENT=HexColor('#217c76');LINE=HexColor('#d8e4e3')
for name,file in [('Review','segoeui.ttf'),('ReviewBold','segoeuib.ttf')]:
    pdfmetrics.registerFont(TTFont(name,'C:/Windows/Fonts/'+file))
pdfmetrics.registerFontFamily('Review',normal='Review',bold='ReviewBold',italic='Review',boldItalic='ReviewBold')
STYLES={
 'body':ParagraphStyle('body',fontName='Review',fontSize=9.6,leading=13,textColor=INK),
 'small':ParagraphStyle('small',fontName='Review',fontSize=8.2,leading=10.7,textColor=MUTED),
 'verdict':ParagraphStyle('verdict',fontName='Review',fontSize=11,leading=15,textColor=INK),
 'finding':ParagraphStyle('finding',fontName='Review',fontSize=9.4,leading=12.3,textColor=INK),
 'head':ParagraphStyle('head',fontName='ReviewBold',fontSize=11.2,leading=14,textColor=ACCENT),
}

def clean(s):return str(s).replace('\u2011','-').replace('\u2013','-').replace('\u2014',' - ')
def para(c,text,x,y,width=CW,style='body',rich=False):
    p=Paragraph(text if rich else escape(clean(text)),STYLES[style]);_,height=p.wrap(width,H)
    assert y-height>=38,('Page overflow',c.getPageNumber(),y,height,text[:120])
    p.drawOn(c,x,y-height);return y-height
def frame(c,title,subtitle):
    c.setFillColor(ACCENT);c.setFont('ReviewBold',8.3);c.drawString(M,H-25,'SIU / VANILLA VILLAGER TRADING / INDEPENDENT VISUAL REVIEW')
    c.setFillColor(INK);c.setFont('ReviewBold',23);c.drawString(M,H-58,title)
    para(c,subtitle,M,H-69,style='small')
    c.setStrokeColor(LINE);c.line(M,29,W-M,29);c.setFillColor(MUTED);c.setFont('Review',7.8)
    c.drawString(M,17,'Original = reference | SIU = review subject | GPT-6.1 Sol, extra-high, fresh context')
    c.drawRightString(W-M,17,str(c.getPageNumber()))
def pair(c,case,y,maxheight,workspace=False):
    col=(CW-18)/2
    files=[case['sides']['vanilla']['window_crop'],case['sides']['siu']['workspace_crop' if workspace else 'window_crop']]
    sizes=[Image.open(p).size for p in files]
    scale=min(col/max(s[0] for s in sizes),maxheight/max(s[1] for s in sizes))
    c.setFont('ReviewBold',8.8);c.setFillColor(ACCENT)
    c.drawString(M,y,'VANILLA / SIU ABSENT');c.drawString(M+col+18,y,'CURRENT SIU WINDOW')
    y-=10;used=max(s[1]*scale for s in sizes)
    for i,(path,size) in enumerate(zip(files,sizes)):
        x=M+i*(col+18);iw,ih=size[0]*scale,size[1]*scale
        c.drawImage(str(path),x+(col-iw)/2,y-ih,iw,ih,mask='auto')
    return y-used-8
def findings(c,items,y):
    for f in items:
        y=para(c,f['id']+' | '+f['priority'].upper()+' - '+f['title'],M,y,style='head')-3
        text='<b>Vanilla:</b> '+escape(clean(f['vanilla_evidence']))+' <b>SIU:</b> '+escape(clean(f['siu_evidence']))+' <b>Change:</b> '+escape(clean(f['recommendation']))
        y=para(c,text,M,y,style='finding',rich=True)-10
    return y

def main():
    review=json.loads((BASE/'independent-review.json').read_text(encoding='utf-8-sig'))
    manifest=json.loads((BASE/'comparison-manifest.json').read_text())
    cases={r['id']:r for r in manifest['cases']};items=review['findings'];mid=math.ceil(len(items)/2)
    PDF.parent.mkdir(parents=True,exist_ok=True)
    c=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1)
    c.setTitle('Villager trading: vanilla versus current SIU - independent visual review')
    c.setAuthor('Independent GPT-6.1 Sol review; compiled by Codex')
    frame(c,'Keep vanilla\u2019s trading identity','Minecraft 1.20.1 | Identical Expert librarian, XP and eight offers | Actual game captures at GUI scale 2')
    y=pair(c,cases['overview'],H-104,246,workspace=True)
    y=para(c,'Both panels use the same source-pixel scale. SIU\u2019s separate Inventory and window controls are intentional.',M,y,style='small')-13
    y=para(c,'INDEPENDENT VERDICT',M,y,style='head')-4
    y=para(c,review['verdict'],M,y,style='verdict')-10
    y=para(c,'KEEP THESE STRENGTHS',M,y,style='head')-4
    for s in review['strengths']:y=para(c,'- '+s,M,y,style='body')-3
    y=para(c,'Method: fresh GPT-6.1 Sol at extra-high reasoning reviewed all six captures independently. Vanilla is the reference; SIU is the subject. The current build was captured without changing its trading implementation.',M,y-7,style='small')
    c.showPage()
    frame(c,'Priorities for a closer match','Trade with both payment slots filled; the native and SIU payment/result stacks were verified identical.')
    y=pair(c,cases['two-input'],H-104,183)
    y=para(c,'Window-only crops. The player Inventory remains separate in SIU, as shown on page 1.',M,y,style='small')-12
    findings(c,items[:mid],y)
    c.showPage()
    frame(c,'Finish the details consistently','Sold-out offer selected on both sides. These are current screenshots, not proposed mockups.')
    y=pair(c,cases['sold-out'],H-104,145)
    y=findings(c,items[mid:],y-4)
    y=para(c,'RECOMMENDED DIRECTION',M,y,style='head')-3
    y=para(c,review['recommended_direction'],M,y,style='body')-6
    y=para(c,'Order: '+' > '.join(review['implementation_order']),M,y,style='small')-6
    para(c,'Scope: '+review['limitations'],M,y,style='small')
    c.save()
    (BASE/'pdf-content-map.json').write_text(json.dumps({'pdf':str(PDF.resolve()),'reviewer':review['reviewer'],'included_findings':[f['id'] for f in items],'pages':3,'all_review_sections_included':True,'images':'Exact screenshot crops; no redraw or color changes'},indent=2))
    print(PDF)

if __name__=='__main__':main()
