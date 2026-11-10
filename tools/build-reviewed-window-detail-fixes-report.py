"""Short report of the five requested corrections, using actual fixed windows."""
from pathlib import Path
import hashlib
import json
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/window-detail-fixes'
PDF=ROOT/'output/pdf/siu-window-detail-fixes.pdf'
FABRIC=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-window-details01'
FORGE=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-forge-integrated-reviewed-details02'

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def fixed_windows():
    result={}
    for folder in [FABRIC,FORGE]:
        for row in json.loads((folder/'audit-core-cases.json').read_text()):
            assert not row.get('error'),row
            original=ROOT/row['capture']
            assert sha(original)==row['sha256']
            state=row['observation']['client']
            windows=[w for w in state['allWindows'] if 'NativeTemplateWindow' in w.get('definition','')]
            assert len(windows)==1,(row['id'],windows)
            b=windows[0]['bounds'];image=Image.open(original).convert('RGB')
            sx,sy=image.width/state['guiWidth'],image.height/state['guiHeight']
            rect=[max(0,int((b['x']-2)*sx)),max(0,int((b['y']-2)*sy)),
                  min(image.width,int((b['x']+b['width']+2)*sx)),min(image.height,int((b['y']+b['height']+2)*sy))]
            crop=OUT/(row['id']+'-fixed.png');image.crop(rect).save(crop)
            result[row['id']]={'capture':str(original.relative_to(ROOT)),'sha256':sha(original),
                'crop_rect':rect,'crop':str(crop.relative_to(ROOT)),'runtime':row['loader']}
    return result

def text(c,value,x,y,size=11,bold=False,color='#243c42'):
    c.setFont('Helvetica-Bold' if bold else 'Helvetica',size)
    c.setFillColor(HexColor(color));c.drawString(x,y,value)

def picture(c,path,box):
    im=Image.open(path);x,y,w,h=box;scale=min(w/im.width,h/im.height)
    c.drawImage(ImageReader(im),x+(w-im.width*scale)/2,y+(h-im.height*scale)/2,
                im.width*scale,im.height*scale,mask='auto')

def main():
    OUT.mkdir(parents=True,exist_ok=True);PDF.parent.mkdir(exist_ok=True)
    shots=fixed_windows()
    pages=[
        ('Magic Bags','Original window color restored',
         'All three tiers match the original UI background: RGB 132, 132, 132 (#848484).',
         ['item-wands-magic-bag-1','item-wands-magic-bag-2','item-wands-magic-bag-3'],'Fabric / production runtime'),
        ('Flower Pouch','Translucent slot guides restored',
         'All 32 designated flowers use the original Botania artwork and transparency.',
         ['item-botania-flower-bag'],'Fabric / production runtime'),
        ('Film Rename','Clean film icon and complete text field',
         'Old tab fragments removed. The original top, left and right field borders are complete.',
         ['exposure-film-rename'],'Fabric / production runtime'),
        ('Paintings','Complete search-bar border',
         'The top bevel is intact. Typing or clearing the search updates the painting list.',
         ['bf-gallery-painting-selector'],'Forge / development runtime'),
        ('Umvuthana','Complete output-slot border',
         'The original output frame keeps its bottom highlight and both lower corners.',
         ['own-bf-minion-trade-real'],'Forge / development runtime'),
    ]
    c=canvas.Canvas(str(PDF),pagesize=(840,594),pageCompression=1)
    c.setTitle('SIU - Five window detail fixes - Minecraft 1.20.1')
    c.setAuthor("Salt's Inventory Update")
    for index,(title,subtitle,note,ids,runtime) in enumerate(pages,1):
        text(c,'SIU / WINDOW DETAIL FIXES',32,560,9,True,'#177b80')
        text(c,title,32,525,27,True)
        text(c,subtitle,32,500,13)
        text(c,'MINECRAFT 1.20.1 / '+runtime.upper(),32,471,9,True,'#177b80')
        if len(ids)==3:
            for i,ident in enumerate(ids):
                x=32+i*264;c.setFillColor(HexColor('#eef4f3'));c.roundRect(x,164,248,284,8,fill=1,stroke=0)
                text(c,'Tier '+str(i+1),x+14,421,12,True)
                picture(c,ROOT/shots[ident]['crop'],(x+10,196,228,202))
        else:
            c.setFillColor(HexColor('#eef4f3'));c.roundRect(32,127,776,321,8,fill=1,stroke=0)
            picture(c,ROOT/shots[ids[0]]['crop'],(52,141,736,293))
        text(c,note,34,99,11)
        text(c,'Actual in-game screenshots. Only the requested windows are included.',34,76,9,color='#64767c')
        c.setStrokeColor(HexColor('#cbd7d8'));c.line(32,40,808,40)
        text(c,'Core 0.1.6 + Compatibility 0.1.3 / Focused visual and input checks',32,23,8,color='#64767c')
        text(c,f'{index} / {len(pages)}',780,23,8,color='#64767c');c.showPage()
    c.save()
    evidence={'pdf':str(PDF.relative_to(ROOT)),'pdf_sha256':sha(PDF),'pages':len(pages),
              'screenshots':shots,'scope':'1.20.1 only; focused affected-window checks, not a complete functional suite.',
              'forge_lineage':str((FORGE/'forge-remap-lineage.json').relative_to(ROOT))}
    (OUT/'evidence.json').write_text(json.dumps(evidence,indent=2)+'\n')
    print(json.dumps({'pdf':str(PDF),'pages':len(pages),'windows':len(shots)}))

if __name__=='__main__':
    main()
