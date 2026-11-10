"""Mine Cells 2.0.0 native UI research and design-only SIU proposals.

Native captures remain immutable. Proposed views are code-authored illustrations,
not screenshots of implemented compatibility.
"""
from pathlib import Path
from io import BytesIO
import json, hashlib, zipfile, shutil
from PIL import Image, ImageDraw
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import Paragraph
from reportlab.lib.styles import ParagraphStyle

ROOT=Path(__file__).resolve().parents[1]
RUN=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-native-minecells-research01'
SRC=RUN/'screenshots'
OUT=ROOT/'output/minecells-ui-research'
TMP=ROOT/'tmp/pdfs/minecells-ui-research'
PDF=ROOT/'output/pdf/siu-minecells-ui-research-and-design.pdf'
JAR=ROOT/'build/ui-modpack-audit-20261025/jars/58c4a332663ca10818d2b72af107062eeecdcc4b.jar'
GUI=ROOT/'artwork/shared-resources/core/assets/salts_inventory_update/textures/gui'
for p in (OUT,OUT/'native',OUT/'mockups',TMP,PDF.parent):p.mkdir(parents=True,exist_ok=True)
NEAR=Image.Resampling.NEAREST
FRAME=Image.open(GUI/'window.png').convert('RGBA')
CONTROL=Image.open(GUI/'window_controls.png').convert('RGBA')
with zipfile.ZipFile(Path.home()/'.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar') as z:
    ASCII=Image.open(BytesIO(z.read('assets/minecraft/textures/font/ascii.png'))).convert('RGBA')
    ITEMS=[Image.open(BytesIO(z.read('assets/minecraft/textures/item/'+n+'.png'))).convert('RGBA') for n in ['stick','lapis_lazuli','emerald']]
with zipfile.ZipFile(JAR) as z:
    native_sheet=Image.open(BytesIO(z.read('assets/minecells/textures/gui/cell_crafter/container.png'))).convert('RGBA')
    paper_sheet=Image.open(BytesIO(z.read('assets/minecells/textures/gui/cell_crafter/recipes.png'))).convert('RGBA')
    recipe=json.loads(z.read('data/minecells/recipes/cell_crafter/gear/balanced_blade.json'))
    crafting=json.loads(z.read('data/minecells/recipes/cell_crafter.json'))
    STEEL=Image.open(BytesIO(z.read('assets/minecells/textures/item/cell_infused_steel.png'))).convert('RGBA')

def glyph(ch):
    n=ord(ch);im=ASCII.crop((n%16*8,n//16*8,n%16*8+8,n//16*8+8));b=im.getbbox()
    return im,3 if ch==' ' else b[2] if b else 5
def tw(s):return sum(glyph(c)[1]+1 for c in s)
def pixeltext(im,s,x,y,col=(244,237,213,255)):
    assert x+tw(s)<=im.width,(s,im.size)
    for ch in s:
        g,w=glyph(ch);ink=Image.new('RGBA',(8,8),col);ink.putalpha(g.getchannel('A'));im.alpha_composite(ink,(x,y));x+=w+1
def native(name,rect=(231,145,409,305)):
    src=Image.open(SRC/(name+'.png')).convert('RGBA')
    return src.crop(tuple(v*2 for v in rect)).resize((rect[2]-rect[0],rect[3]-rect[1]),NEAR)
def frame(w,h):
    im=Image.new('RGBA',(w,h))
    for a,b in [((0,0,5,5),(0,0,5,5)),((6,0,11,5),(w-5,0,w,5)),((0,6,5,11),(0,h-5,5,h)),((6,6,11,11),(w-5,h-5,w,h)),((5,0,6,5),(5,0,w-5,5)),((5,6,6,11),(5,h-5,w-5,h)),((0,5,5,6),(0,5,5,h-5)),((6,5,11,6),(w-5,5,w,h-5)),((5,5,6,6),(5,5,w-5,h-5))]:
        im.alpha_composite(FRAME.crop(a).resize((b[2]-b[0],b[3]-b[1]),NEAR),b[:2])
    return im
def controls(im,y=5):
    # Existing SIU focus, more-options and close sprites. No fake storage sorts.
    for i,col in enumerate((0,3,2)):
        im.alpha_composite(CONTROL.crop((11*col,0,11*col+11,11)),(im.width-46+i*14,y))
def copper_panel(w,h,title):
    im=Image.new('RGBA',(w,h),(24,36,65,255));d=ImageDraw.Draw(im)
    d.rectangle((0,0,w-1,h-1),outline=(30,25,27),width=1)
    d.rectangle((1,1,w-2,h-2),outline=(119,78,36),width=1)
    d.line((2,2,w-3,2),fill=(168,116,54));d.line((2,2,2,h-3),fill=(127,93,48))
    d.line((3,21,w-4,21),fill=(83,58,34))
    pixeltext(im,title,7,8);controls(im)
    return im
def inventory():
    im=frame(176,105);pixeltext(im,'Inventory',7,7,(64,64,64,255));controls(im)
    d=ImageDraw.Draw(im)
    for row in range(4):
        for col in range(9):
            x=7+col*18;y=23+row*18+(4 if row==3 else 0)
            d.rectangle((x,y,x+17,y+17),fill=(139,139,139),outline=(244,244,244))
            d.line((x,y,x+16,y),fill=(55,55,55));d.line((x,y,x,y+16),fill=(55,55,55))
    # Original transparent item sprites, without the old navy slot backgrounds.
    for col,(icon,count) in enumerate(zip([ITEMS[0],STEEL,ITEMS[1],ITEMS[2]],[1,6,4,2])):
        x=8+col*18;im.alpha_composite(icon.resize((16,16),NEAR),(x,24))
        if count>1:
            pixeltext(im,str(count),x+11,34,(30,30,30,255))
            pixeltext(im,str(count),x+10,33,(255,255,255,255))
    return im
def main_window(name='normal-ready'):
    original=native(name)
    im=copper_panel(178,100,'Cell Crafter')
    # Preserve the complete original working area, at the native pixel spacing.
    im.alpha_composite(original.crop((4,5,174,75)),(4,26))
    return im
def stack(main):
    inv=inventory();w=max(main.width,inv.width);im=Image.new('RGBA',(w,main.height+9+inv.height))
    im.alpha_composite(main,((w-main.width)//2,0));im.alpha_composite(inv,((w-inv.width)//2,main.height+9));return im
def parchment(name):
    # Masks from the exact native texture geometry, not a rectangular page crop.
    im=native(name,(220,137,420,313))
    mask=Image.new('L',im.size)
    # The native page is at x=40, with each 40x25 flag at x=8, y=24+25*n.
    mask.paste(paper_sheet.crop((0,0,160,176)).getchannel('A'),(40,0))
    selected=1 if name=='picker-decoration' else 2 if name=='picker-other' else 0
    for i in range(3):
        # Exact original flag alpha includes the whole colored item icon inside it.
        sy=25 if i==selected else 0;dx=8 if i==selected else 16
        flag=paper_sheet.crop((160,sy,200,sy+25)).getchannel('A')
        # Union with the parchment so the overlapping paper remains complete.
        from PIL import ImageChops
        layer=Image.new('L',im.size);layer.paste(flag,(dx,24+i*25))
        mask=ImageChops.lighter(mask,layer)
    im.putalpha(mask);return im
def recipe_window(name='picker-all-gear'):
    paper=parchment(name)
    im=Image.new('RGBA',(204,204));head=copper_panel(204,23,'Cell Crafter')
    im.alpha_composite(head,(0,0));im.alpha_composite(paper,(2,27));return im
def save(im,name):
    p=OUT/'mockups'/(name+'.png');im.resize((im.width*4,im.height*4),NEAR).save(p);return p

originals={}
for name in ('normal-empty','normal-ready','normal-missing','normal-after-craft','normal-recipes','normal-search-balanced','normal-recipe-selected','picker-all-gear','picker-decoration','picker-other','unbreakable-ready'):
    shutil.copy2(SRC/(name+'.png'),OUT/'native'/(name+'.png'))
    originals[name]=native(name,(220,137,420,313)) if 'picker-' in name or 'recipes' in name or 'search' in name or 'recipe-selected' in name else native(name)
designs={
 'main':save(stack(main_window()),'cell-crafter-and-inventory'),
 'unbreakable':save(stack(main_window('unbreakable-ready')),'unbreakable-crafter-and-inventory'),
 'picker':save(recipe_window(),'recipe-picker'),
 'picker-selected':save(recipe_window('normal-recipe-selected'),'recipe-picker-search'),
 'decoration':save(recipe_window('picker-decoration'),'recipe-picker-decoration'),
 'other':save(recipe_window('picker-other'),'recipe-picker-other'),
}

pdfmetrics.registerFont(TTFont('Segoe','C:/Windows/Fonts/segoeui.ttf'))
pdfmetrics.registerFont(TTFont('SegoeBold','C:/Windows/Fonts/segoeuib.ttf'))
W,H=900,650
c=canvas.Canvas(str(PDF),pagesize=(W,H));c.setTitle('Mine Cells - native container research and SIU window designs')
c.setAuthor('SIU Project Everything')
NAVY='#142441';INK='#243044';MUTED='#536275';TEAL='#16767A';COPPER='#9A632D'
styles={
 'body':ParagraphStyle('body',fontName='Segoe',fontSize=11,leading=16,textColor=HexColor(INK)),
 'small':ParagraphStyle('small',fontName='Segoe',fontSize=9,leading=13,textColor=HexColor(MUTED)),
 'caption':ParagraphStyle('caption',fontName='Segoe',fontSize=10,leading=14,textColor=HexColor(MUTED)),
}
def text(s,x,y,w=800,style='body'):
    p=Paragraph(s,styles[style]);_,h=p.wrap(w,1000);p.drawOn(c,x,H-y-h);return h
def label(s,x,y,color=TEAL):
    c.setFillColor(HexColor(color));c.setFont('SegoeBold',9);c.drawString(x,H-y-9,s)
def rule(y):
    c.setStrokeColor(HexColor('#DCE3E8'));c.line(36,H-y,864,H-y)
def page(n,title,sub):
    c.setFillColor(HexColor('#FFFFFF'));c.rect(0,0,W,H,fill=1,stroke=0)
    label('PROJECT EVERYTHING / NATIVE UI RESEARCH',36,24)
    c.setFillColor(HexColor(NAVY));c.setFont('SegoeBold',26);c.drawString(36,H-71,title)
    text(sub,36,85,828,'caption');rule(120)
    rule(612);text('Mine Cells 2.0.0 | Minecraft 1.20.1 Fabric | Research + proposed designs',36,622,780,'small')
    c.setFillColor(HexColor(MUTED));c.setFont('Segoe',9);c.drawRightString(864,19,str(n))
def box(x,y,w,h,color='#F0F4F6'):
    c.setFillColor(HexColor(color));c.roundRect(x,H-y-h,w,h,8,fill=1,stroke=0)
def pic(im,x,y,w,h,bg=None):
    if isinstance(im,(str,Path)):im=Image.open(im).convert('RGBA')
    if bg:box(x,y,w,h,bg)
    scale=min(w/im.width,h/im.height);ow,oh=im.width*scale,im.height*scale
    c.drawImage(ImageReader(im),x+(w-ow)/2,H-y-(h+oh)/2,width=ow,height=oh,mask='auto')
def link(s,url,x,y,w=800):
    text(f'<link href="{url}" color="{TEAL}"><u>{s}</u></link>',x,y,w,'small')
def bullet(title,body,x,y,w):
    label(title,x,y);return text(body,x,y+19,w)+19

# 1 - Complete inventory and actual workflow.
page(1,'Mine Cells: a small, distinctive UI family',
     'Every custom inventory/container interface found in the audited 1.20.1 release, with native captures and proposed SIU designs.')
for x,num,title,body in [(36,'1','Menu family','Cell Crafter'),(318,'2','Block variants','Normal + Unbreakable'),(600,'2','Native modes','Crafting + recipe browser')]:
    box(x,138,264,72)
    c.setFont('SegoeBold',30);c.setFillColor(HexColor(COPPER));c.drawString(x+14,H-187,num)
    label(title.upper(),x+60,150);text(body,x+60,172,190,'caption')
label('CRAFTING MODE / NATIVE',36,230);label('RECIPE BROWSER / NATIVE',330,230)
box(36,251,264,280,'#343C48');pic(originals['normal-ready'],48,263,240,256)
box(318,251,264,280,'#343C48');pic(originals['picker-all-gear'],330,263,240,256)
label('WHAT THE PLAYER DOES',606,230)
steps=[('01  Open','Place the Cell Crafter and right-click it.'),('02  Choose','Press the parchment, search or browse a category, select a recipe, then confirm.'),('03  Craft','Carry the ingredients in the player inventory and press the native craft button.'),('04  Collect','The block releases the crafted item into the world. The displayed result is a preview.')]
y=254
for title,body in steps:y+=bullet(title,body,606,y,246)+12
text('Scope: one unique inventory menu, not one window per recipe. The normal block is craftable; its unbreakable counterpart opens the same screen. Other interfaces are accounted for on page 6.',36,550,828)
link('Pinned release: Mine Cells 2.0.0 for 1.20.1 Fabric','https://modrinth.com/mod/minecells/version/a0CPJveH',36,591)
c.showPage()

# 2 - Normal container / recommended main view.
page(2,'Cell Crafter: preserve the blue and copper',
     'Recommended design: retain the original working area, add a matching title strip, and move the player slots into SIU Inventory.')
label('ORIGINAL IN GAME / SIU ABSENT',36,139);label('PROPOSED SIU / DESIGN ONLY',462,139)
box(36,160,402,352,'#343C48');pic(originals['normal-ready'],54,172,366,328)
box(462,160,402,352,'#E7EEF3');pic(designs['main'],478,171,370,330)
bullet('KEEP THE NATIVE WORK AREA','The parchment button, material icons and counts, craft button, arrow and result preview retain their native arrangement. No artificial input or output slots are added.',36,532,397)
bullet('A SMALL MATCHING HEADER','Navy fill and copper edges continue the original palette. SIU controls sit above the artwork. The lower inventory is a separate window, so the same player slots appear only once.',462,532,397)
c.showPage()

# 3 - Full native paper mode.
page(3,'Recipe browser: keep the whole parchment',
     'A second mode of the same crafter window. The title strip stays available while the original paper replaces the crafting body.')
label('ORIGINAL IN GAME / SIU ABSENT',36,139);label('PROPOSED SIU / DESIGN ONLY',462,139)
box(36,160,402,350,'#343C48');pic(originals['picker-all-gear'],48,169,378,332)
box(462,160,402,350,'#E7EEF3');pic(designs['picker'],474,170,378,330)
bullet('PRESERVE ITS CHARACTER','Keep the torn page silhouette, all three red flags, the complete search field, 6 x 5 recipe grid, locked question marks, ingredient preview and confirmation mark.',36,529,397)
bullet('PRESERVE ITS INPUT RULES','Categories, search, wheel and arrow scrolling, recipe selection, tooltips and back remain native. The original browser hides/disables the inventory slots; the SIU version should do the same while it is open.',462,529,397)
c.showPage()

# 4 - All recipe states rather than inventing separate containers.
page(4,'One browser, three categories',
     'These are views within the crafter, not three additional container screens. All illustrations below are proposed SIU windows using native captured content.')
for x,title,key in [(36,'GEAR / SEARCH + SELECTION','picker-selected'),(318,'DECORATION','decoration'),(600,'OTHER','other')]:
    label(title,x,141);box(x,163,264,292,'#E7EEF3');pic(designs[key],x+12,174,240,270)
bullet('SEARCH AND CHOOSE','The native search narrows the grid to Balanced Blade. Selecting it shows its ingredient costs; the checkmark returns to the crafting view.',36,478,254)
bullet('DECORATIVE RECIPES','The same layout browses decorations such as flags. Keep the full native scrolling grid rather than forcing it into a small generic item list.',318,478,254)
bullet('PROGRESSION MATTERS','The Other category includes locked entries. Unlocks come from advancements; a window must retain the native locks and their requirement feedback.',600,478,254)
text('Captured example: Balanced Blade uses 1 stick, 6 Cell Infused Steel, 4 lapis lazuli and 2 emeralds to make 1 blade.',36,565,828,'small')
link('Official recipe format: input maps, categories and advancement requirements','https://mim1q.dev/minecells/cell_crafter_recipes/',36,589)
c.showPage()

# 5 - Second physical block, same behavior and design.
page(5,'Unbreakable Cell Crafter',
     'A second block variant using the same native menu and two-mode workflow. It should share the same SIU adapter and visual treatment.')
label('UNBREAKABLE BLOCK / ORIGINAL IN GAME',36,139);label('PROPOSED SIU / SAME DESIGN',462,139)
box(36,160,402,350,'#343C48');pic(originals['unbreakable-ready'],54,172,366,326)
box(462,160,402,350,'#E7EEF3');pic(designs['unbreakable'],478,171,370,330)
bullet('WHAT IS DIFFERENT','The registered block has unbreakable hardness and a very high blast resistance. It is not a different recipe system or a separate storage container.',36,531,397)
bullet('WHAT STAYS THE SAME','It opens CellCrafterScreen and uses the same crafting and parchment browser views. The proposed title remains Cell Crafter; the body should not be redesigned for this variant.',462,531,397)
link('Pinned block registry source','https://github.com/mim1q/MineCells/blob/minecells-2.0.0%2B1.20.1/src/main/java/com/github/mim1q/minecells/registry/MineCellsBlocks.java',36,590)
c.showPage()

# 6 - Complete accounting + evidence limits + primary sources.
page(6,'What stays native, and what was verified',
     'Inventory-only scope: other Mine Cells interfaces and items are recorded here rather than given unnecessary container windows.')
label('INTERFACE OR ITEM',36,140);label('PURPOSE / SIU TREATMENT',266,140)
rows=[
 ('Doorway selection','Standalone travel / destination selector. No inventory menu: keep its native full-screen behavior.'),
 ('Mine Cells Guidebook','Optional Patchouli book. Keep the book presentation; do not turn it into an inventory window.'),
 ('Cell Holder','Cell-storage item with direct inventory click interactions. No separate container screen was found.'),
 ('Small Crate','Registered as a regular block, not a custom inventory-menu provider. No extra crate window.'),
 ('Boss / weapon HUD and toasts','Overlays and notifications, not inventory containers. Keep the native presentation.'),
 ('Dungeon loot chests','Vanilla chest menus, where used, remain covered by SIU chest support rather than a new Mine Cells design.'),
]
y=163
for i,(title,body) in enumerate(rows):
    if i%2==0:box(36,y-4,828,41,'#F0F4F6')
    text(title,46,y,204,'caption');text(body,266,y,586,'caption');y+=45
bullet('EVIDENCE AND FOCUSED CHECKS','Original release JAR inspection and native-only gameplay captures confirmed both block variants, the two view modes, all categories, search, selection and one craft consuming its exact materials. The menu exposes only the 36 player slots. Source inspection confirms queued item drops at the block.',36,448,397)
bullet('COVERAGE LIMIT','This is research and a design proposal for Mine Cells 2.0.0 on 1.20.1 Fabric. No SIU implementation or complete gameplay suite is claimed. The guidebook and travel selector were classified from source/assets; they were not exercised in this minimal capture client.',462,448,397)
link('Release / version','https://modrinth.com/mod/minecells/version/a0CPJveH',36,567,180)
link('Official recipe documentation','https://mim1q.dev/minecells/cell_crafter_recipes/',216,567,230)
link('Native screen source','https://github.com/mim1q/MineCells/blob/minecells-2.0.0%2B1.20.1/src/main/java/com/github/mim1q/minecells/screen/cellcrafter/CellCrafterScreen.java',462,567,190)
link('Official wiki / guidebook','https://mim1q.dev/minecells/',666,567,198)
text('Native artwork: Mine Cells / mim1q; Minecraft / Mojang. SIU mockups use native captures and existing SIU controls. Original captures, hashes and recipe evidence are saved alongside this report.',36,590,828,'small')
c.showPage();c.save()

manifest={'scope':'Mine Cells 2.0.0+1.20.1 Fabric; research and design only',
 'release':'https://modrinth.com/mod/minecells/version/a0CPJveH',
 'native_jar':str(JAR.relative_to(ROOT)),'jar_sha256':hashlib.sha256(JAR.read_bytes()).hexdigest(),
 'screenshots':[{ 'file':str(p.relative_to(ROOT)), 'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted((OUT/'native').glob('*.png'))],
 'ui_families':['CellCrafterScreen'], 'blocks':['minecells:cell_crafter','minecells:unbreakable_cell_crafter'],
 'modes':['crafting','recipe picker'],'balanced_blade_recipe':recipe,'crafting_recipe':crafting,
 'mockups':{k:str(v.relative_to(ROOT)) for k,v in designs.items()},'pdf':str(PDF.relative_to(ROOT)),
 'verification_limit':'Focused native observations and source/binary research; no compatibility implementation or full gameplay suite.'}
(OUT/'manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
for n in ['before-craft.json','after-craft.json','unbreakable-state.json','picker-state.json']:
    shutil.copy2(RUN/n,OUT/n)
print(PDF)
