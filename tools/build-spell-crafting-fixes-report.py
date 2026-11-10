"""Two-window follow-up using verified in-game screenshots only."""
from pathlib import Path
import hashlib,json
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader

ROOT=Path(__file__).resolve().parents[1]
RUN=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-spell-crafting03'
OUT=ROOT/'output/spell-crafting-fixes'
PDF=ROOT/'output/pdf/siu-bind-spells-and-crafting-fixes.pdf'

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def text(c,s,x,y,size=11,bold=False,color='#243c42'):
    c.setFont('Helvetica-Bold' if bold else 'Helvetica',size);c.setFillColor(HexColor(color));c.drawString(x,y,s)

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    rows={r['id']:r for r in json.loads((RUN/'audit-core-cases.json').read_text())}
    cases=[
      ('spell-engine-spell-binding','Bind Spells','Complete top border restored',
       'The book outline is the empty input-slot guide. The animated book appears on the table in the world.'),
      ('minecraft-crafting-table','Visual Workbench','Regular SIU window integration',
       'The crafting grid, output and recipe-book toggle fit inside one frame. Grid contents remain persistent.'),
    ]
    c=canvas.Canvas(str(PDF),pagesize=(840,594),pageCompression=1)
    c.setTitle('SIU - Bind Spells and Visual Workbench fixes - 1.20.1')
    evidence=[]
    for page,(ident,title,subtitle,note) in enumerate(cases,1):
        row=rows[ident];assert not row.get('error'),row
        source=ROOT/row['capture'];assert sha(source)==row['sha256']
        state=row['observation']['client'];windows=state['allWindows'];assert len(windows)==1
        assert 'NativeTemplateWindow' in windows[0]['definition']
        b=windows[0]['bounds'];im=Image.open(source).convert('RGB');sx=im.width/state['guiWidth'];sy=im.height/state['guiHeight']
        rect=[int((b['x']-2)*sx),int((b['y']-2)*sy),int((b['x']+b['width']+2)*sx),int((b['y']+b['height']+2)*sy)]
        crop=OUT/(ident+'-fixed.png');im.crop(rect).save(crop);picture=Image.open(crop)
        text(c,'SIU / FINAL WINDOW CORRECTIONS',32,560,9,True,'#177b80')
        text(c,title,32,525,27,True);text(c,subtitle,32,500,13)
        text(c,'MINECRAFT 1.20.1 / FABRIC PRODUCTION SCREENSHOT',32,471,9,True,'#177b80')
        c.setFillColor(HexColor('#eef4f3'));c.roundRect(32,139,776,309,8,fill=1,stroke=0)
        scale=min(736/picture.width,281/picture.height)
        c.drawImage(ImageReader(picture),420-picture.width*scale/2,153+(281-picture.height*scale)/2,picture.width*scale,picture.height*scale)
        text(c,note,34,108,10)
        text(c,'Actual fixed windows. Focused rendering, crafting and recipe-button checks only.',34,82,9,color='#64767c')
        c.setStrokeColor(HexColor('#cbd7d8'));c.line(32,40,808,40)
        text(c,'Core 0.1.6 + Compatibility 0.1.3 / Both Fabric and Forge JARs rebuilt',32,23,8,color='#64767c')
        text(c,f'{page} / 2',780,23,8,color='#64767c');c.showPage()
        evidence.append(dict(id=ident,capture=str(source.relative_to(ROOT)),sha256=sha(source),crop_rect=rect,crop=str(crop.relative_to(ROOT))))
    c.save();(OUT/'evidence.json').write_text(json.dumps(dict(pdf=str(PDF.relative_to(ROOT)),sha256=sha(PDF),pages=2,cases=evidence),indent=2)+'\n')
    print(json.dumps(dict(pdf=str(PDF),pages=2)))

if __name__=='__main__':main()
