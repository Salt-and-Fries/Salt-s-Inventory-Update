"""Original in-game SSN interfaces beside the implemented SIU windows."""
from pathlib import Path
import json,hashlib
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from pypdf import PdfReader

ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'output/ssn-implemented';OUT.mkdir(parents=True,exist_ok=True)
PDF=ROOT/'output/pdf/siu-simple-storage-network-implemented.pdf'
design=json.loads((ROOT/'output/ssn-window-design/manifest.json').read_text())
paths=['inventory','request','request_expanded','inventory_remote','crafting_remote','expanded_remote','import_filter_kabel','export_kabel','filter_kabel','collector','process_kabel','cradle','receiver']
runs=['1.20.1-forge-siu-ssn-implemented-06','1.21.1-neoforge-integrated-ssn-implemented-04']
pdfmetrics.registerFont(TTFont('UI','C:/Windows/Fonts/segoeui.ttf'));pdfmetrics.registerFont(TTFont('UIB','C:/Windows/Fonts/segoeuib.ttf'))
c=canvas.Canvas(str(PDF),pagesize=(842,595));c.setTitle('Simple Storage Network - implemented SIU windows');c.setAuthor("Salt's Inventory Update")
TEAL='#247C80';INK='#19373D';MUTED='#5E7076';images=[]
def text(x,y,s,size=11,font='UI',color=INK):c.setFont(font,size);c.setFillColor(HexColor(color));c.drawString(x,y,s)
def wrap(x,y,s,width,size=10):
    line=''
    for word in s.split():
        proposed=(line+' '+word).strip()
        if pdfmetrics.stringWidth(proposed,'UI',size)>width and line:text(x,y,line,size);y-=size*1.5;line=word
        else:line=proposed
    if line:text(x,y,line,size);y-=size*1.5
    return y
def footer(page,width=842):
    c.setStrokeColor(HexColor('#CCDADD'));c.line(36,37,width-36,37)
    text(36,22,'Actual native/SIU game captures | SSN 1.13.2 | Core and public API unchanged',8,color=MUTED)
    text(width-58,22,str(page),8,color=MUTED)
def fitted(im,x,y,width,height,scale=None):
    k=scale or min(width/im.width,height/im.height)
    iw,ih=im.width*k,im.height*k
    c.drawImage(ImageReader(im.convert('RGB')),x+(width-iw)/2,y+(height-ih)/2,iw,ih)
def header(title,subtitle,width=842,height=595):
    c.setPageSize((width,height));text(36,height-36,'PROJECT EVERYTHING / IMPLEMENTED WINDOWS',10,'UIB',TEAL)
    text(36,height-70,title,23,'UIB');text(36,height-93,subtitle,10,color=MUTED)

header('Simple Storage Network','Approved mockups implemented in the Compatibility add-on')
text(36,456,'Native layouts, separate player inventory',20,'UIB')
y=wrap(36,427,'The native searchable lists, crafting grids, cable settings, ghost filters and newer container controls remain in their original relative positions. One SIU frame supplies the title, movement and management controls.',740,13)
rows=[('1.20.1','Forge','10 container screen types'),('1.21.1','NeoForge','13 container screen types'),('26.1.2','NeoForge','13 container screen types')]
for i,row in enumerate(rows):
    y=321-i*40
    for x,value in zip([48,200,380],row):text(x,y,value,13,'UIB'if x==48 else'UI')
wrap(36,175,'The comparisons on the following pages use original game captures on the left and actual implemented SIU windows on the right. Test contents can differ between the two captures. Expanded terminals retain all 525 visible entries and use GUI scale 1.',744,11)
wrap(36,104,'Focused validation covers the affected menus. It is not the complete gameplay suite; dedicated multiplayer and optional JEI/Curios combinations were not exercised.',744,10)
footer(1);c.showPage()
for page,(path,spec)in enumerate(zip(paths,design['cases']),2):
    run=runs[0 if page<=11 else 1];folder=ROOT/'build/remaining-ui-audit/gameplay'/run
    variant=dict(import_filter_kabel='import-filter-kabel-operation',export_kabel='export-kabel-operation',process_kabel='processing-active',cradle='cradle-active',receiver='receiver-active').get(path,path.replace('_','-'))
    state=json.loads((folder/'ssn-checks'/(variant+'-client.json')).read_text())
    w=next(w for w in state['windows']if w['menuId']=='storagenetwork:'+path);b=w['bounds'];shot=folder/'screenshots'/('ssn-'+variant+'.png')
    screen=Image.open(shot).convert('RGB');factor=screen.width/state['guiWidth']
    bounds=tuple(round(factor*v)for v in [b['x'],b['y'],b['x']+b['width'],b['y']+b['height']])
    window=screen.crop(bounds).resize((b['width'],b['height']),Image.Resampling.NEAREST)
    window_path=OUT/(path+'-window.png');window.save(window_path)
    native=Image.open(ROOT/spec['original']).convert('RGB');nscale=spec['geometry']['scale'];native=native.resize((round(native.width/nscale),round(native.height/nscale)),Image.Resampling.NEAREST)
    large='expanded'in path;width,height=(1190,842)if large else(842,595)
    ver='1.20.1 / Forge'if run==runs[0]else'1.21.1 / NeoForge'
    header(spec['title'],f'Minecraft {ver} | original publisher release and actual SIU game capture',width,height)
    gap=22;panelw=(width-72-gap)/2;panely=130;panelh=height-260
    for x,label in [(36,'ORIGINAL / SIU ABSENT'),(36+panelw+gap,'IMPLEMENTED SIU WINDOW')]:
        text(x,height-124,label,10,'UIB',TEAL);c.setFillColor(HexColor('#EEF5F5'));c.roundRect(x,panely,panelw,panelh,7,stroke=0,fill=1)
    k=min((panelw-18)/max(native.width,window.width),(panelh-16)/max(native.height,window.height))
    fitted(native,36,panely,panelw,panelh,k);fitted(window,36+panelw+gap,panely,panelw,panelh,k)
    wrap(36,106,spec['design'],width-72,11)
    text(36,54,'Native source: github.com/Lothrazar/Storage-Network',8,color=MUTED)
    c.linkURL('https://github.com/Lothrazar/Storage-Network',(36,50,340,65),relative=0,thickness=0)
    footer(page,width);c.showPage();images.append(dict(path=path,image=str(window_path.relative_to(ROOT)),capture=str(shot.relative_to(ROOT)),run=run))
header('Windows in the real game','Full-screen example with the terminal and player inventory open together')
folder=ROOT/'build/remaining-ui-audit/gameplay'/runs[0]
full=Image.open(folder/'screenshots/ssn-inventory.png').convert('RGB');fitted(full,36,85,770,385)
footer(15);c.save()
assert len(PdfReader(PDF).pages)==15
(OUT/'pdf-manifest.json').write_text(json.dumps(dict(pdf=str(PDF.relative_to(ROOT)),sha256=hashlib.sha256(PDF.read_bytes()).hexdigest(),pages=15,images=images),indent=2))
print(PDF)
