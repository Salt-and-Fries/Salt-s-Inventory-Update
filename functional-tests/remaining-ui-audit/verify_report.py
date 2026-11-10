"""Render every report page and build numbered contact sheets for visual QA."""
import json
from pathlib import Path
import subprocess
from PIL import Image,ImageDraw
import pdfplumber

ROOT=Path(__file__).resolve().parents[2]
pdf=ROOT/'output/pdf/siu-remaining-mods-native-vs-core-ui-audit.pdf'
out=ROOT/'tmp/pdfs/remaining-ui-audit-final';out.mkdir(parents=True,exist_ok=True)
issues=[];counts=[]
with pdfplumber.open(pdf) as doc:
    for n,page in enumerate(doc.pages,1):
        chars=page.chars
        bad=[c['text'] for c in chars if c['x0'] < -1 or c['x1']>page.width+1 or c['top'] < -1 or c['bottom']>page.height+1]
        if bad:issues.append({'page':n,'outside_page_text':''.join(bad)})
        if not chars:issues.append({'page':n,'missing_text':True})
        counts.append({'page':n,'chars':len(chars),'images':len(page.images)})
    page_count=len(doc.pages)
print('PDF pages',page_count,'text boundary issues',len(issues),flush=True)
with (out/'poppler.log').open('w') as log:
    subprocess.run(['pdftoppm','-scale-to','1400','-png',str(pdf),str(out/'page')],stdout=log,stderr=subprocess.STDOUT,check=True)
pages=sorted(out.glob('page-*.png'))
if len(pages)!=page_count:raise ValueError('Rendered page count mismatch')
for start in range(0,len(pages),8):
    sheet=Image.new('RGB',(1640,640),'#dce5eb');draw=ImageDraw.Draw(sheet)
    for k,p in enumerate(pages[start:start+8]):
        col,row=k%4,k//4
        with Image.open(p) as im:
            im.thumbnail((400,283),Image.Resampling.LANCZOS);sheet.paste(im,(10+410*col,22+320*row))
        draw.text((10+410*col,5+320*row),'Page '+str(start+k+1),fill='black')
    sheet.save(out/f'contact-{start//8+1:02}.jpg',quality=90)
(out/'qa.json').write_text(json.dumps({'pages':page_count,'rendered_pages':len(pages),'issues':issues,'page_counts':counts},indent=2),encoding='utf8')
print('Rendered all pages; contact sheets',len(list(out.glob('contact-*.jpg'))),flush=True)
