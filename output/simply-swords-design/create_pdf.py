from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
W,H=1000,650;p=Path('output/simply-swords-design');c=canvas.Canvas('output/pdf/siu-simply-swords-ui-analysis-and-design.pdf',pagesize=(W,H));ink=HexColor('#263339');muted=HexColor('#5a6b71');accent=HexColor('#92703c');pale=HexColor('#f2f0eb')
def t(x,y,s,size=12,col=ink,b=False):c.setFillColor(col);c.setFont('Helvetica-Bold' if b else 'Helvetica',size);c.drawString(x,y,s)
def lines(x,y,items,size=12,leading=20,col=muted):
 for i,s in enumerate(items):t(x,y-i*leading,s,size,col)
def box(x,y,w,h):c.setFillColor(pale);c.roundRect(x,y,w,h,12,stroke=0,fill=1)
def image(path,x,y,w,h):
 a=ImageReader(str(path));iw,ih=a.getSize();k=min(w/iw,h/ih);c.drawImage(a,x+(w-iw*k)/2,y+(h-ih*k)/2,width=iw*k,height=ih*k,mask='auto')
def head(k,title,subtitle):t(36,615,k,10,accent,True);t(36,579,title,24,ink,True);t(36,551,subtitle,11,muted)
def foot(n):c.setStrokeColor(HexColor('#dcded9'));c.line(36,36,964,36);t(36,20,'DESIGN STUDY  /  MOCKUPS ARE NOT IMPLEMENTED',8,muted,True);c.setFont('Helvetica',8);c.drawRightString(964,20,f'{n} / 4');c.showPage()
head('01 / NATIVE REFERENCE','Runic Forge as it exists','Simply Swords 1.70.2 on Minecraft 1.20.1 Fabric; SIU absent.')
for x,name,label in [(36,'native-forge-empty-crop.png','ACTUAL NATIVE SCREEN / EMPTY'),(517,'native-forge-seeded-crop.png','ACTUAL NATIVE SCREEN / SEEDED')]:
 box(x,135,447,386);t(x+16,493,label,9,accent,True);image(p/'native'/name,x+18,170,410,300)
lines(45,109,['Center: weapon; left: Runefused Gem; right: Netherfused Gem; below: eight Runic Tablets.','The native player inventory sits beneath them. Seeded items illustrate layout only.'],10,16)
foot(1)
head('02 / ALTERNATIVES','Three ways to host the same slab','Each image is a static SIU design mockup; no live implementation is shown.')
for x,name,label,caption in [(36,'gray-empty.png','A / GRAY FRAME','Familiar, but contrasts with the artwork.'),(362,'bars-empty.png','B / TWO BARS','Preserves native edges; taller caps.'),(688,'dark-empty.png','C / DARK STONE','Palette chosen for the B + C hybrid.')]:
 box(x,200,300,320);t(x+15,489,label,10,accent,True);image(p/'mockups'/name,x+12,290,276,150);lines(x+15,245,[caption],10,15)
foot(2)
head('03 / RECOMMENDED','Two bars in charcoal stone','B layout + C colors: the original torn sides stay exposed between the bars.')
for x,name,label in [(36,'hybrid-empty.png','B + C MOCKUP / EMPTY'),(517,'hybrid-seeded.png','B + C MOCKUP / SEEDED')]:
 box(x,185,447,336);t(x+16,493,label,9,accent,True);image(p/'mockups'/name,x+15,275,417,170)
lines(45,149,['The native torn edges and transparent sides remain exposed; no full side frame is added.','The bag/player grid is omitted. Seeded items are illustrative; no new controls are added.'],11,18)
foot(3)
head('04 / BEHAVIOR AND EVIDENCE','Preserve the forge workflow','A design direction for a possible Compatibility-side public API integration.')
box(36,185,928,335);t(58,484,'PRESERVE THE NATIVE INTERACTION',11,accent,True)
lines(58,457,['Keep all 11 visible sockets, the weapon preview tooltip and native item behavior.','Removing the weapon or closing the screen commits the native edit; Shift-move remains native.','Keep the full animation corridor and its 800 ms channel/pulse feedback; do not expose hidden slot 47.'],12,22,ink)
t(58,368,'EVIDENCE AND LIMITS',11,accent,True)
lines(58,341,['Pinned Simply Swords 1.70.2-1.20.1 Fabric; artifact SHA-1: 902fd3ac7558bebb249f61572227d499f51b3bde.','Original ornate texture is used by the audited screen. A bundled alternate texture is not a verified skin setting.','Native capture: SIU absent; a tooltip-only dependency mixin was disabled in named-dev QA.','No implementation, successful awakening or broad compatibility claim is made.'],10,19)
t(58,245,'Official source: RunicForgeScreen (pinned commit)  |  Modrinth: pinned 1.20.1 release',9,accent)
c.linkURL('https://github.com/Sweenus/SimplySwords/blob/5132625f7ef0916168b53999587b73490b8876d4/common/src/main/java/net/sweenus/simplyswords/client/screen/RunicForgeScreen.java',(58,240,288,254),relative=0)
c.linkURL('https://modrinth.com/mod/simply-swords/version/Tq7tcDiO',(291,240,500,254),relative=0)
foot(4);c.save();print('saved')
