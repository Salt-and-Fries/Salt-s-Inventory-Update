"""Visual-first Chipped design study; native images and proposals are labeled separately."""
import json,shutil
from pathlib import Path
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase.pdfmetrics import stringWidth

out=Path('output/project-everything-chipped-design');out.mkdir(parents=True,exist_ok=True)
analysis=Path('build/project-everything-chipped-design/analysis.json');shutil.copyfile(analysis,out/'analysis.json')
manifest=json.loads((out/'design-manifest.json').read_text());native={x['loader']+':'+x['name']:x for x in manifest['native_captures']};proposal={x['state']:x for x in manifest['proposals']}
W,H=1000,660;pdf=Path('output/pdf/siu-chipped-ui-analysis-and-window-design.pdf');pdf.parent.mkdir(parents=True,exist_ok=True);c=canvas.Canvas(str(pdf),pagesize=(W,H),pageCompression=1);c.setTitle('Chipped UI analysis and SIU window proposal')
INK=HexColor('#21343b');MUTED=HexColor('#60747a');ACC=HexColor('#9a6235');PALE=HexColor('#f3eee7');LINE=HexColor('#d9dedc');TEAL=HexColor('#137c7b')
def text(x,y,s,size=11,color=INK,bold=False):c.setFillColor(color);c.setFont('Helvetica-Bold' if bold else 'Helvetica',size);c.drawString(x,y,s)
def wrap(s,width,size=11,bold=False):
 font='Helvetica-Bold' if bold else 'Helvetica';lines=[];line=''
 for word in s.split():
  t=(line+' '+word).strip()
  if line and stringWidth(t,font,size)>width:lines.append(line);line=word
  else:line=t
 if line:lines.append(line)
 return lines
def para(x,y,s,width,size=11,color=INK,leading=None,bold=False):
 leading=leading or size*1.42
 for line in wrap(s,width,size,bold):text(x,y,line,size,color,bold);y-=leading
 return y
def card(x,y,w,h,color=PALE):c.setFillColor(color);c.roundRect(x,y,w,h,9,stroke=0,fill=1)
def image(path,x,y,w,h):
 im=ImageReader(path if isinstance(path,Image.Image) else str(path));iw,ih=im.getSize();sc=min(w/iw,h/ih);c.drawImage(im,x+(w-iw*sc)/2,y+(h-ih*sc)/2,width=iw*sc,height=ih*sc,mask='auto')
def head(kicker,title,sub=None):text(38,617,kicker.upper(),9,ACC,True);text(38,585,title,23,INK,True);sub and text(38,562,sub,10,MUTED)
def footer(n,kind):
 c.setStrokeColor(LINE);c.line(38,37,W-38,37);text(38,22,'Minecraft 1.20.1 | '+kind,8,MUTED);c.setFont('Helvetica',8);c.setFillColor(MUTED);c.drawRightString(W-38,22,f'{n:02d} / 13')
def finish(n,kind):footer(n,kind);c.showPage()
def nativeimg(key):return native[key]['crop']
def prop(key):return proposal[key]['file']
def bullet(x,y,title,body,width=235):
 text(x,y,title,12,INK,True);return para(x,y-18,body,width,10,MUTED)-12
# 1 - cover
text(38,610,'CHIPPED / SIU DESIGN STUDY',10,ACC,True);text(38,565,'A compact Chipped workshop',28,INK,True)
para(38,530,'Keep Chipped\'s visual language and full variant browser. Select a source stack from the existing player inventory without adding another inventory grid.',345,13,INK)
text(38,405,'RECOMMENDED CONCEPT',9,TEAL,True);para(38,384,'One warm-toned window, with an explicit material picker, the native preview and crafting controls, and a stable 9 x 6 variant viewport.',345,11,MUTED)
card(430,118,532,422);image(prop('populated'),450,142,492,380);text(455,131,'PROPOSED VISUAL - NOT IMPLEMENTED',9,TEAL,True)
finish(1,'Design proposal; native source captures exclude SIU')
# 2 - native anatomy
head('Native reference','What the original screen contains','One shared 256 x 256 interface; the station title and material filter change.')
card(38,83,535,460);image(nativeimg('forge:forge-mason-clear'),62,105,485,420)
text(600,515,'ORIGINAL MINECRAFT SCREEN',9,ACC,True)
y=490
y=bullet(600,y,'1  Material selection','The lower 36 cells mirror the player inventory. Clicking one selects its stack; it does not move an item.')
y=bullet(600,y,'2  Variant browser','The right side shows 9 columns and 6 visible rows. Search filters names; the mouse wheel scrolls the list.')
y=bullet(600,y,'3  Preview and action','The left panel keeps a 3D preview, four arrangement modes, and Craft. Shift + Craft All converts matching stacks in the player inventory, not other open containers.')
finish(2,'Native reference - Forge 3.0.7, SIU absent')
# 3 workflow
head('Native behavior','A selection, not a container input','Observed in both native Fabric and Forge Mason Table sessions.')
for x,n,title,body in [(38,'01','Select','Choose an existing player-inventory stack.'),(272,'02','Choose','Pick a variant and inspect the preview.'),(506,'03','Craft','The source stack is replaced in the same inventory slot.'),(740,'04','Reset','Native selection and grid reset after a successful craft.')]:
 card(x,415,218,115);text(x+16,507,n,10,ACC,True);text(x+16,482,title,14,INK,True);para(x+16,460,body,185,10,MUTED)
card(38,85,440,300);card(500,85,462,300);image(nativeimg('forge:forge-mason-clear'),54,105,408,260);image(nativeimg('forge:forge-mason-crafted'),516,105,430,260);text(54,93,'BEFORE: STONE x32 SELECTED',9,ACC,True);text(516,93,'AFTER: VARIANT x32 IN THE SAME SLOT',9,ACC,True)
finish(3,'Native capture and source analysis - no SIU integration')
# 4 alternatives
head('Three directions','Choose a compact, self-contained workshop','These are design options, not existing behavior.')
for x,label,title,body in [(38,'A','Upper crop + inventory','Close to native selection, but duplicates the player inventory and keeps a large window.'),(345,'B','Detached preview + browser','Independent placement, but adds window management and makes selection less obvious.'),(652,'C','Integrated + picker','Keeps the full browser and native preview together; an explicit picker avoids a duplicate inventory.')]:
 card(x,145,290,375,HexColor('#edf5f3') if label=='C' else PALE);text(x+20,490,label,16,TEAL if label=='C' else ACC,True);text(x+20,463,title,14,INK,True);para(x+20,439,body,250,11,MUTED)
 if label=='A':image(nativeimg('fabric:mason-table-material-clean'),x+40,172,210,210)
 elif label=='C':image(prop('populated'),x+20,185,250,180)
 else:
  c.setFillColor(HexColor('#c6a374'));c.roundRect(x+25,280,105,100,6,fill=1,stroke=0);c.roundRect(x+155,230,110,150,6,fill=1,stroke=0);text(x+44,326,'Preview',10,INK,True);text(x+170,300,'Variants',10,INK,True)
 if label=='C':text(x+20,158,'RECOMMENDED',9,TEAL,True)
finish(4,'Alternatives are conceptual proposals')
# 5 hero
head('Recommended direction','One workshop, one source reference','A proposed 270 x 181 GUI-unit layout, shown at enlarged scale.')
card(38,90,670,435);image(prop('populated'),58,110,630,395)
text(730,508,'PROPOSED VISUAL',9,TEAL,True)
y=480
y=bullet(730,y,'Material strip','Icon, name, count and Choose refer to a real player stack. This is not an input slot.',225)
y=bullet(730,y,'Native work area','The preview reflects the chosen variant; retain four modes, Craft and all 54 visible cells.',225)
y=bullet(730,y,'Position indicator','A narrow scrollbar is proposed; the original supports wheel scrolling only.',225)
finish(5,'Visual proposal - not implemented or tested in SIU')
# 6 empty/picker
head('Proposed states','Choose a material without moving it','The picker lists eligible existing player-inventory stacks only.')
for x,key,title,desc in [(38,'empty','Empty','No source chosen; the window prompts for a material.'),(510,'picker','Picker open','Choose an eligible stack by reference; multiple or duplicate stacks can be listed separately.')]:
 card(x,142,452,370);image(prop(key),x+15,182,422,290);text(x+16,490,title,13,INK,True);para(x+16,165,desc,420,10,MUTED)
finish(6,'Visual proposal - picker and material strip are new')
# 7 search
head('Proposed states','Search keeps the grid steady','Keep the full 9 x 6 viewport and match its visible and clickable bounds.')
for x,key,title,desc in [(38,'search','Filtered results','Keep native name filtering and preserve the current search field.'),(510,'no-results','No matches','Empty cells remain reserved; no misleading item or output slot.')]:
 card(x,142,452,370);image(prop(key),x+15,182,422,290);text(x+16,490,title,13,INK,True);para(x+16,165,desc,420,10,MUTED)
finish(7,'Visual proposal - source search pixels are native captures')
# 8 scroll preview
head('Proposed states','Browse and inspect without clipping','Reserve room for the complete native 3D silhouette, including tall or connected blocks.')
for x,key,title,desc in [(38,'scrolled','Scrolled variants','Preserve native ordering and wheel behavior; the scrollbar is a proposed addition.'),(510,'preview-vertical','Vertical arrangement','Example uses native Tinkering Table iron bars in the vertical preview mode.')]:
 card(x,142,452,370);image(prop(key),x+15,182,422,290);text(x+16,490,title,13,INK,True);para(x+16,165,desc,420,10,MUTED)
finish(8,'Visual proposal - scrollbar is not native Chipped UI')
# 9 modes
head('Native preview','Four arrangements, one fixed perspective','Iron bars demonstrate connected shapes. These are mode changes, not a rotate control.')
for i,name in enumerate(['Single block','Horizontal: three','Vertical: three','Two by two']):
 x=38+i*232;card(x,145,216,360);text(x+15,480,name,12,INK,True);path=nativeimg('fabric:tinkering-preview-mode-'+str(i+1));img=Image.open(path).crop((9*3,26*3,81*3,139*3));image(img,x+22,210,172,250)
finish(9,'Native Fabric 3.0.7 screenshots; SIU absent')
# 10-11 gallery
station_specs=[('mason-table','Mason Table','Stone'),('carpenters-table','Carpenters Table','Oak planks'),('loom-table','Loom Table','White wool'),('glassblower','Glassblower','Glass'),('botanist-workbench','Botanist Workbench','Oak leaves'),('alchemy-bench','Alchemy Bench','Amethyst block'),('tinkering-table','Tinkering Table','Iron bars')]
for page,group in [(10,station_specs[:4]),(11,station_specs[4:])]:
 head('Native station gallery','Seven stations, one shared screen', 'Titles and available material variants differ; the interface and control layout are shared.')
 for i,(key,title,item) in enumerate(group):
  x=38+(i%2)*472;y=83+(1-i//2)*217;card(x,y,452,202);image(nativeimg('fabric:'+key+'-material-clean'),x+6,y+7,190,188);text(x+210,y+151,title,12,INK,True);para(x+210,y+130,'Example material: '+item+'.',225,10,MUTED);text(x+210,y+47,'ORIGINAL SCREEN',9,ACC,True)
 finish(page,'Native Fabric 3.0.7 screenshots; SIU absent')
# 12 implementation
head('Implementation forecast','Preserve the behavior behind the layout','A compatibility-side design forecast; no implementation has been made.')
for x,y,title,body in [(38,470,'Selection','Picker chooses an eligible player-inventory stack by reference. It should refresh if the stack moves or changes.'),(510,470,'Craft bridge','Send authenticated session actions; revalidate the selected stack and allowed variant on the server.'),(38,300,'Window lifetime','Track the originating workbench and recipe filter, including range and source removal. Keep simultaneous windows independent.'),(510,300,'Native widgets','Reuse preview, modes, search and variant artwork where practical. Make visible and clickable grid bounds identical.')]:
 card(x,y-130,452,150);text(x+18,y-12,title,13,INK,True);para(x+18,y-39,body,415,11,MUTED)
para(38,92,'The existing public desktop API appears promising, but a prototype is needed to prove the integration path. A simple upper-screen crop cannot provide native selection or safe crafting by itself.',924,10,INK)
finish(12,'Technical forecast - no production change or SIU gameplay claim')
# 13 evidence
head('Evidence and limits','What was observed, and what remains proposed')
y=520
for title,body in [('Runtime','Fabric: seven native stations, four preview modes, search, scrolling and a Mason craft. Forge: native Mason smoke and craft. SIU was absent in both reference clients.'),('Native hit area','The lower part of the sixth visible row did not select in the pinned 3.0.7 Fabric capture, while the upper part did. The proposed window should use one consistent viewport for drawing and hit testing.'),('Source authority','Published 3.0.7 binaries and native observations are the baseline. The public 1.20.1 source branch may contain later changes.'),('Coverage','No Chipped-to-SIU integration was built or tested. The mockups are compositions; they are not runtime screenshots.')]:
 text(38,y,title,11,INK,True);y=para(170,y,body,790,10,MUTED)-15
text(38,210,'PRIMARY SOURCES',9,ACC,True)
links=[('Chipped 3.0.7 - Fabric','https://modrinth.com/mod/chipped/version/pwyEaKDs'),('Chipped 3.0.7 - Forge','https://modrinth.com/mod/chipped/version/pi3f4er3'),('Chipped 1.20.1 source branch','https://github.com/terrarium-earth/Chipped/tree/1.20.1')]
for i,(label,url) in enumerate(links):
 text(38,190-i*18,label,10,TEAL);c.linkURL(url,(38,185-i*18,380,200-i*18),relative=0)
text(38,103,'Original screenshots, capture hashes, fixture versions and proposal manifest accompany this PDF.',9,MUTED)
finish(13,'Evidence, limits and source links')
c.save();print('Saved',pdf)
