"""Design-only SIU proposals for the <=3-interface remaining 1.20.1 shortlist.

Original gameplay PNGs are immutable inputs. Every proposed view is explicitly a
code-authored mockup, composed from pinned native artwork and SIU sprites.
"""
from pathlib import Path
from io import BytesIO
from collections import Counter
import hashlib, json, math, zipfile
from PIL import Image, ImageDraw, ImageFont
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'output/project-everything-small-design'
TMP = ROOT/'build/project-everything-small-design'
PDF = ROOT/'output/pdf/siu-project-everything-small-ui-window-mockups.pdf'
GUI = ROOT/'artwork/shared-resources/core/assets/salts_inventory_update/textures/gui'
REF = json.loads((OUT/'native-references.json').read_text(encoding='utf-8'))
ROWS = REF['cases']
BYID = {r['id']: r for r in ROWS}
INV = json.loads((ROOT/'output/remaining-ui-audit/inventory.json').read_text(encoding='utf-8'))
ENTRIES = {r['entry']:r for r in INV['entries']}
FRAME = Image.open(GUI/'window.png').convert('RGBA')
CONTROLS = Image.open(GUI/'window_controls.png').convert('RGBA')
with zipfile.ZipFile(Path.home()/'.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar') as z:
    ASCII = Image.open(BytesIO(z.read('assets/minecraft/textures/font/ascii.png'))).convert('RGBA')
BG = (198,198,198,255)
DARK = (45,43,56,255)
NEAR = Image.Resampling.NEAREST
PROTECTED = []

def glyph(ch):
    n=ord(ch) if ord(ch)<128 else ord('?')
    im=ASCII.crop((n%16*8,n//16*8,n%16*8+8,n//16*8+8))
    box=im.getbbox()
    return im,3 if ch==' ' else box[2] if box else 5

def tw(s): return sum(glyph(ch)[1]+1 for ch in s)

def text(im,s,x,y,color=(64,64,64,255)):
    assert x>=0 and y>=0 and x+tw(s)<=im.width, (s, x,im.size)
    for ch in s:
        g,w=glyph(ch)
        ink=Image.new('RGBA',(8,8),color); ink.putalpha(g.getchannel('A'))
        im.alpha_composite(ink,(int(x),int(y)));x+=w+1

def blank(w,h,color=BG): return Image.new('RGBA',(w,h),color)

def paste(im,src,at,tag=None):
    assert at[0]>=0 and at[1]>=0 and at[0]+src.width<=im.width and at[1]+src.height<=im.height, (tag,im.size,src.size,at)
    im.alpha_composite(src,at)
    if tag: PROTECTED.append({'part':tag,'source_size':list(src.size),'destination':list(at),'canvas':list(im.size)})

def asset(entry,suffix):
    matches=list((TMP/'assets'/str(entry)).rglob(suffix))
    assert len(matches)==1,(entry,suffix,matches)
    return Image.open(matches[0]).convert('RGBA')

def region(row,rect):
    n=row['native'];b=n['bounds'];p=ROOT/n['capture']
    assert hashlib.sha256(p.read_bytes()).hexdigest()==n['sha256']
    im=Image.open(p).convert('RGBA')
    return im.crop(tuple(2*v for v in (b['x']+rect[0],b['y']+rect[1],b['x']+rect[2],b['y']+rect[3]))).resize((rect[2]-rect[0],rect[3]-rect[1]),NEAR)

def frame(w,h,color=BG):
    im=Image.new('RGBA',(w,h));sheet=FRAME.copy()
    if color[:3]!=BG[:3]:
        def tint(c):
            lum=sum(c[:3])/3
            if not c[3] or lum<10:return c
            delta=55 if lum>240 else lum-198
            return tuple(round(max(0,min(255,k+delta))) for k in color[:3])+(c[3],)
        sheet.putdata([tint(c) for c in sheet.getdata()])
    for src,dst in [((0,0,5,5),(0,0,5,5)),((6,0,11,5),(w-5,0,w,5)),((0,6,5,11),(0,h-5,5,h)),((6,6,11,11),(w-5,h-5,w,h)),((5,0,6,5),(5,0,w-5,5)),((5,6,6,11),(5,h-5,w-5,h)),((0,5,5,6),(0,5,5,h-5)),((6,5,11,6),(w-5,5,w,h-5)),((5,5,6,6),(5,5,w-5,h-5))]:
        im.alpha_composite(sheet.crop(src).resize((dst[2]-dst[0],dst[3]-dst[1]),NEAR),dst[:2])
    return im

def header(im,title,color=BG,full=False):
    cols=[0,1,4,9,2] if full and tw(title)+89<im.width else [3,2]
    assert tw(title)+len(cols)*14+14<=im.width,(title,im.size)
    text(im,title,7,7,(237,235,232,255) if max(color[:3])<135 else (58,51,45,255))
    start=im.width-5-len(cols)*14
    for i,c in enumerate(cols):
        sy=22 if c==0 else 0
        paste(im,CONTROLS.crop((c*11,sy,c*11+11,sy+11)),(start+i*14,5))

def panel(body,title,color=BG,minimum=0,full=False):
    w=max(minimum,body.width+14,tw(title)+45)
    im=frame(w,body.height+29,color)
    header(im,title,color,full)
    paste(im,body,((w-body.width)//2,22),'complete working body')
    return im

def join(parts,pad=3):
    w=max(x+p.width for p,x,y in parts);h=max(y+p.height for p,x,y in parts)
    im=Image.new('RGBA',(w+pad*2,h+pad*2))
    for p,x,y in parts:paste(im,p,(x+pad,y+pad))
    return im

def button(s,w=None,h=18,color=BG):
    w=max(w or 0,tw(s)+12);im=frame(w,h,color)
    text(im,s,(w-tw(s))//2,(h-8)//2,(225,223,218,255) if max(color[:3])<135 else (60,56,49,255))
    return im

def inset(w,h,color=(139,139,139,255)):
    im=blank(w,h,color);d=ImageDraw.Draw(im)
    d.line((0,0,w-1,0),fill=(55,55,55,255));d.line((0,0,0,h-1),fill=(55,55,55,255))
    d.line((1,h-1,w-1,h-1),fill=(255,255,255,255));d.line((w-1,1,w-1,h-1),fill=(255,255,255,255))
    return im

def grid(cols,rows,slot=None):
    im=blank(cols*18,rows*18)
    for y in range(rows):
        for x in range(cols):paste(im,slot or inset(18,18),(x*18,y*18))
    return im

def themed_native(row,rect,title,color,source=None):
    body=source or region(row,rect)
    return panel(body,title,color)

META={}
MOCK={}

def save(row,label,im,native_rect,idea,keep,behavior,limit=''):
    key=row['id'];(OUT/'mockups').mkdir(exist_ok=True);(OUT/'native-crops').mkdir(exist_ok=True)
    native_rect=list(native_rect)
    player=[s for s in row['native']['slots'] if s['player'] and 0<=s['y']<1000]
    if player:native_rect[3]=max(native_rect[3],max(s['y'] for s in player)+24)
    png=OUT/'mockups'/f'{key}-mockup.png';im.resize((im.width*4,im.height*4),NEAR).save(png)
    native=OUT/'native-crops'/f'{key}-original.png';region(row,native_rect).resize(((native_rect[2]-native_rect[0])*4,(native_rect[3]-native_rect[1])*4),NEAR).save(native)
    MOCK[key]=im
    META[key]={'id':key,'mod':row['mod'],'entry':row['entry'],'label':label,'native':str(native.relative_to(ROOT)),
               'mockup':str(png.relative_to(ROOT)),'logical_size':list(im.size),'native_rect':native_rect,
               'idea':idea,'preserve':keep,'behavior':behavior,'limit':limit,
               'release':ENTRIES[row['entry']]['releases'][0]['version'],'loader':row['loader'],
               'source_capture':row['native']['capture'],'source_sha256':row['native']['sha256']}

def build():
    # Accessories: real default slot definitions, not an invented extra inventory.
    r=ROWS[0]
    char=blank(96,86);paste(char,inset(54,78,(15,15,15,255)),(23,3))
    avatar=region(r,[43,10,66,69]);paste(char,avatar,(36,11),'full player preview')
    for y in range(4):paste(char,region(r,[7,7+y*18,25,25+y*18]),(1,3+y*18),'armor ghost')
    paste(char,region(r,[151,61,169,79]),(77,61),'offhand')
    pane=blank(153,143)
    groups=[('head',['hat','face']),('chest',['necklace','cape','back']),('arm',['hand','hand','ring','ring','wrist']),('leg',['belt']),('feet',['shoes','anklet']),('misc',['charm'])]
    for i,(g,names) in enumerate(groups):
        y=i*20;ico=asset(15,'group/'+g+'.png');paste(pane,ico.resize((16,16),NEAR),(1,y+1))
        for j,n in enumerate(names):
            cell=inset(18,18);ico=asset(15,'slot/'+n+'.png');ico=ico.resize((16,16),NEAR)
            paste(cell,ico,(1,1));paste(pane,cell,(23+j*20,y))
    paste(pane,button('Cosmetics',84),(24,124))
    im=join([(panel(char,'Character'),0,0),(panel(pane,'Accessories'),115,0)])
    save(r,'Character + Accessories',im,[-39,-3,178,168],
         'An Accessories drawer beside the existing SIU Character window.',
         'Armor, offhand, player preview, six native groups and all 14 default accessory positions. Group icons and slot silhouettes come from Accessories.',
         'Group and cosmetic controls live below the title. Server-defined slot counts drive scrolling; the player inventory stays in SIU Inventory.',
         'The native capture has its accessory panel collapsed. The expanded proposal illustrates the mod\'s default slot data, not an equipped loadout.')

    r=ROWS[1];body=blank(182,74)
    search=inset(182,12,(120,120,120,255));text(search,'Search...',3,2,(208,208,208,255));paste(body,search,(0,0))
    paste(body,region(r,[7,17,189,72]),(0,17),'terminal grid and scrollbar')
    main=panel(body,'Portable Mana Cell',minimum=219)
    rail=blank(18,77)
    for i in range(4):paste(rail,region(r,[-21,6+i*20,-5,22+i*20]),(1,i*19))
    upgrades=region(r,[198,0,231,49]);im=join([(frame(24,86),0,20),(rail,3,24),(main,26,0),(upgrades,main.width+28,22)])
    save(r,'Portable Mana Cell',im,[-26,-3,233,170],
         'Use SIU\'s established AE2 terminal language for Applied Botanics.',
         'All 27 terminal cells, the complete scroll track, the settings rail and both upgrade sockets. Keep the AE2 ordering instead of inventing mana-machine controls.',
         'Search gets a complete row below the title. Native settings and upgrades remain attached; SIU Inventory handles the player slots.')

    r=ROWS[2];a=asset(31,'transmutation_table.png')
    # The label-free atlas retains the complete upper border and circuit shape.
    body=a.crop((0,0,176,107))
    im=panel(body,'Transmutation Table',(24,14,38,255),minimum=202)
    save(r,'Transmutation Table',im,[-2,-2,178,203],
         'A dark purple machine window with the original circuit silhouette intact.',
         'Three outcome strips, selection areas, input socket, luminous green control and the complete lower circuitwork.',
         'Keep native choice, cost/progress and activation feedback in place. The old centered heading is replaced by one SIU title.')

    r=ROWS[3];brown=(89,52,36,255)
    offers=region(r,[4,16,173,160]);left=panel(offers,'Bounties',brown)
    board=region(r,[179,16,334,80]);right=panel(board,'Bounty Board',brown,minimum=191)
    im=join([(left,0,0),(right,left.width-1,0)])
    save(r,'Bounty Board',im,[-2,-2,350,168],
         'Attach a full-height Bounties list to a compact board window.',
         'The full native offer viewport, 21 bounty cells, three decree cells and the entire reputation strip. Native brown slot borders remain visible.',
         'Selecting an offer, scrolling and inspecting rewards keep the original behavior. The off-screen transfer slot remains virtual, never an extra visible cell.')

    r=ROWS[4];center=region(r,[6,16,171,98]);main=panel(center,'Cargo Airship',minimum=180)
    # Each outboard panel is 4x4. All four are present; no inferred single chest.
    port=blank(72,146);star=blank(72,146)
    for i,y in enumerate([18,106]):
        paste(port,region(r,[-81,y-1,-9,y+71]),(0,i*74),'port cargo 4x4')
        paste(star,region(r,[185,y-1,257,y+71]),(0,i*74),'starboard cargo 4x4')
    lp=panel(port,'Port',minimum=86);rp=panel(star,'Starboard',minimum=100)
    im=join([(lp,0,0),(main,lp.width-1,0),(rp,lp.width+main.width-2,0)])
    save(r,'Vehicle inventory / Cargo Airship',im,[-91,-3,267,198],
         'Keep the central vehicle equipment panel and attach both cargo wings.',
         'All 87 owned positions: six equipment sockets, one fuel socket, 16 central cargo cells and four exterior 4x4 grids.',
         'Port/starboard ordering matches the original. Each vehicle shows only the compartments it actually owns; the player inventory opens independently.')

    r=ROWS[5];a=asset(346,'ministrosity2.png');chest=a.crop((53,0,176,76))
    # The atlas joins this shaped chest to the player inventory at its lower
    # corners. Remove only those two joining fragments, not the chest border.
    cm=ImageDraw.Draw(chest)
    cm.rectangle((0,28,8,75),fill=(0,0,0,0));cm.rectangle((115,28,122,75),fill=(0,0,0,0))
    cm.line((9,28,9,75),fill=(0,0,0,255));cm.line((114,28,114,75),fill=(0,0,0,255))
    cm.line((9,75,114,75),fill=(0,0,0,255))
    # Source model is isolated by a reviewed contour, avoiding a copied world card.
    pet=region(r,[8,24,59,74]);mask=Image.new('L',pet.size)
    ImageDraw.Draw(mask).polygon([(8,5),(11,1),(31,0),(37,6),(36,22),(31,25),(39,28),(50,35),(49,43),(39,46),(35,42),(34,49),(24,49),(22,45),(14,43),(8,47),(1,42),(3,34),(15,24),(10,23)],fill=255)
    pet.putalpha(mask)
    for py in range(40,pet.height):
        for px in range(pet.width):
            v=pet.getpixel((px,py))
            if (min(v[:3])>100 and max(v[:3])-min(v[:3])<18) or (py>=48 and min(v[:3])>=40 and max(v[:3])-min(v[:3])<18):pet.putpixel((px,py),v[:3]+(0,))
            elif py==43 and max(v[:3])<4:pet.putpixel((px,py),v[:3]+(0,))
    body=blank(184,86,(59,51,45,255));paste(body,inset(55,75,(24,22,23,255)),(1,6));paste(body,pet,(3,17),'pet preview');paste(body,chest,(61,4),'complete gold chest silhouette')
    im=panel(body,'Netherite Ministrosity',(59,51,45,255),minimum=202)
    save(r,'Netherite Ministrosity',im,[-3,-8,180,168],
         'Keep the pet and its shaped gold chest together in one companion window.',
         'The entire lid, chest frame, 15 gold cells and a contained pet preview. The world behind the original shaped UI is not copied into the proposal.',
         'The pet preview is a live viewport in a future implementation. Storage uses the original 5x3 arrangement and specialized gold artwork.')

    r=ROWS[6];a=asset(346,'fusion.png');body=a.crop((0,0,176,70))
    # This atlas is label-free; using it avoids a flat patch over the red circuitry.
    im=panel(body,'Weapon Fusion',(70,73,79,255),minimum=190)
    save(r,'Fusion Anvil / Weapon Fusion',im,[-2,-2,178,168],
         'A steel SIU header above the original red-circuit fusion machine.',
         'The full hammer, two input sockets, plus sign, fusion arrow, output socket and every red circuit branch.',
         'Leave native item validation and result behavior intact. Custom sockets are never overpainted with ordinary storage slots.')

    for r in ROWS[7:9]:
        body=blank(174,83,(24,37,68,255));native=region(r,[4,6,173,76]);paste(body,native,(2,0),'empty-state parchment')
        # Keep the small parchment and add the native recipe drawer affordance.
        paste(body,button('Recipes',70,18,(40,48,70,255)),(98,60))
        title='Unbreakable Cell Crafter' if 'unbreakable' in r['id'] else 'Cell Crafter'
        im=panel(body,title,(24,37,68,255),minimum=tw(title)+49)
        save(r,title,im,[-2,-2,178,168],
             'A compact blue-and-gold crafter that keeps its complete parchment.',
             'Native recipe parchment and the space used by recipe selection, ingredients and crafting feedback when recipes are unlocked.',
             'The Recipes action opens an attached parchment drawer using the mod\'s search, recipe list, paging and crafting widgets. No fake item slots replace those widgets.',
             'This native world has no unlocked recipe. The main comparison intentionally shows that empty state; the expanded drawer on the interaction page is a layout concept.')

    r=ROWS[9];a=asset(459,'primordial_forge.png');body=a.crop((0,0,176,135))
    # This native screen draws above its declared bounds. Copy only the actual
    # stage/material region, ending before the transient tooltip starts at y=67.
    paste(body,region(r,[4,-11,172,64]),(4,18),'stage and material requirements')
    # Original disabled stone button, sliced horizontally without scaling text.
    stone=a.crop((192,64,352,94))
    text(stone,'Start Elemental Stage',(160-tw('Start Elemental Stage'))//2,12,(133,132,132,255))
    paste(body,stone,(8,99),'complete disabled action')
    im=panel(body,'Primordial Forge: Upgrade',DARK,minimum=213)
    save(r,'Primordial Forge / Upgrade',im,[-4,-33,182,205],
         'Keep the forge\'s cracked stone, reinforcement stage and requirement board.',
         'The full material list and counts, missing-material markers, current percentage and whole Start action. The hover tooltip is not baked into the artwork.',
         'Materials and disabled-state reasons remain live. The future renderer should use the native stone button slices rather than stretch its icon or text.')

    r=ROWS[10];a=asset(459,'primordial_forge.png');body=blank(277,180,DARK)
    # Native forge spine is complete, including the red center and top cap.
    spine=a.crop((371,256,477,419))
    # Keep the selected Lightbringer rendering from the actual reference. It is
    # wholly inside the forge spine, so no world/background card is introduced.
    rendered=region(r,[35,-45,141,78]);sm=spine.crop((0,0,106,123)).getchannel('A')
    rendered.putalpha(sm)
    # The hover hint overlaps the lower-right spine. Restore that exact area
    # from the clean atlas; the armor silhouettes lie to its left.
    paste(rendered,spine.crop((62,112,106,123)),(62,112))
    paste(spine,rendered,(0,0))
    paste(body,spine,(86,1),'complete forge spine and selected armor')
    armor=['arcane_mage','warrior','forge_guard','paladin','frost_dk','blood_dk','unholy_dk','archer','bard']
    # Native embedded item art allows clean silhouettes instead of copied world pixels.
    cols=[2,23,44,65,196,217,238,259]
    armor=['arcane_mage','warrior','forge_guard','valkyrie','frost_dk','blood_dk','unholy_dk','archer']
    for i,name in enumerate(armor):
        if name=='paladin':name='forge_guard'
        for j,part in enumerate(['helmet','chestplate','leggings','boots']):
            icon=asset(459,f'armor/{name}_{part}.png')
            if icon.height>icon.width:icon=icon.crop((0,0,icon.width,icon.width))
            icon=icon.resize((16,16),NEAR)
            paste(body,icon,(cols[i],42+j*21),'armor option')
    paste(body,button('Forge',200,25,DARK),(38,150),'forge action')
    im=panel(body,'Primordial Forge',DARK,full=True)
    save(r,'Primordial Forge / Craft',im,[-66,-65,243,196],
         'Keep the complete armor-selection fan and forge spine on one stone canvas.',
         'The native arrangement of four-piece armor sets, central selected set, its name and the complete Forge action. No top-of-texture crop based on the declared menu bounds.',
         'Selection swaps the central armor and opens the original requirements feedback. Stage/craft actions remain in the same working area.',
         'Side-set thumbnails use representative embedded armor sprites. The central Lightbringer is retained from the native capture. Implementation must draw the actual ordered item stacks at every position.')

    r=ROWS[11];a=asset(459,'trader_screen.png')
    body=blank(252,151,DARK);board=a.crop((0,0,216,124));paste(body,board,(0,24))
    # Preserve the native animated trader within its explicit framed viewport.
    portrait=region(r,[-59,32,-7,98]);paste(body,portrait,(5,66),'merchant portrait')
    for i,label in enumerate(['BUY','SELL','TRADE']):paste(body,button(label,64,20,DARK),(60+i*64,1))
    text(body,'0',26,37,(230,226,215,255))
    im=panel(body,'Serkonid Trader',DARK)
    save(r,'Serkonid Trader',im,[-68,-28,179,208],
         'An integrated stone trader with its three native modes below the title.',
         'Buy, Sell, Trade, the cinder balance, the complete portrait frame and the native offer viewport. The empty native offer state remains empty.',
         'Switch modes inside one window; scrolling and offer details remain native. SIU controls sit above the tabs, away from their hover/selected artwork.')

    r=ROWS[12];a=asset(578,'container/crystallized_flower.png');gold=(234,174,100,255)
    body=blank(168,102,gold)
    paste(body,region(r,[5,13,65,103]),(0,0),'XP and feed controls')
    paste(body,region(r,[67,13,173,48]),(62,0),'book/result path')
    paste(body,region(r,[67,48,73,110]),(62,35),'native divider')
    # Rebuild the empty list from its clean texture, avoiding the inventory-sort
    # widget that overlaps its lower-right edge in the reference capture.
    paste(body,a.crop((72,48,172,110)),(67,35),'complete enchantment list frame')
    paste(body,region(r,[162,38,170,63]),(157,25),'native list sorting and scroll thumb')
    im=panel(body,'Crystalline Flower',gold,minimum=196)
    save(r,'Crystalline Flower',im,[-2,-2,178,188],
         'Use honey-colored chrome around the native consume-and-enchant layout.',
         'The complete XP tower, feed buttons, input, book/result path, list viewport and full scroll track.',
         'Enchantment-list sorting is a distinct native action and remains above the list. Only the redundant inventory-sort button is removed.')

    r=ROWS[13];im=panel(region(r,[7,17,169,53]),'Honey Cocoon',gold,minimum=177,full=True)
    save(r,'Honey Cocoon',im,[-2,-2,178,152],
         'A compact two-row honey chest using SIU\'s standard storage proportions.',
         'All 18 cells and their native brown/gold fill and borders. The header shares the original honey palette.',
         'Normal storage gestures and SIU sorting act only on the cocoon; player items stay in SIU Inventory.')

    r=ROWS[14];a=asset(578,'buzzing_briefcase/background.png');case=a.crop((0,0,302,170))
    # Preserve every honey latch and the whole lower handle. Empty bee panels
    # must not accidentally contain occupied/disabled control sprites.
    s=asset(578,'buzzing_briefcase/bee_slots.png');cells=Image.new('RGBA',(22,44))
    paste(cells,s.crop((22,0,44,22)),(0,0))
    for yy in range(2):
        for xx in range(2):paste(cells,s.crop((11,22,22,33)),(xx*11,22+yy*11))
    # Empty/disabled bee controls are faint in the native view. Retain that
    # treatment rather than making them resemble active gray inventory slots.
    cells.putalpha(cells.getchannel('A').point(lambda v:round(v*0.30)))
    for y in range(2):
        for x in range(7):paste(case,cells,(40+x*32,48+y*46),'custom bee cell')
    band=frame(302,23,(126,80,33,255));header(band,'Buzzing Briefcase',(126,80,33,255))
    im=join([(band,0,0),(case,0,22)],0)
    save(r,'Buzzing Briefcase',im,[-74,-47,313,201],
         'Turn the whole bee case into a movable window, preserving its silhouette.',
         'The full lid, straps, honey latches, handle and two rows of seven custom bee panels. Bee health/status controls remain bee panels, never ordinary item slots.',
         'A wood-toned title strip above the lid carries SIU controls without covering either honey latch. Native per-bee actions appear in their own cells when bees are present.',
         'The reference is empty. Cell occupancy/status will be drawn by the original bee renderer in implementation.')

    r=ROWS[15];body=blank(228,132)
    name=inset(139,14,(24,24,24,255));text(name,'Vending Machine',3,3,(239,239,239,255));paste(body,name,(0,1))
    paste(body,button('Trade View',74,18),(150,0))
    # Cost/result and side controls copied at full 1:1 sprite scale.
    text(body,'Cost',1,27);text(body,'Result',69,27)
    paste(body,region(r,[7,34,43,70]),(0,38),'cost filters')
    paste(body,region(r,[75,34,111,70]),(68,38),'result filters')
    paste(body,region(r,[45,43,72,62]),(38,46),'exchange arrow')
    side=region(r,[120,31,169,77]);ImageDraw.Draw(side).rectangle((0,40,11,45),fill=BG)
    paste(body,side,(133,30),'all side I/O toggles')
    paste(body,button('C',18,18),(204,42))
    text(body,'Input',1,85);text(body,'Output',151,85)
    paste(body,region(r,[7,81,79,117]),(0,95),'input buffer')
    paste(body,region(r,[97,81,169,117]),(150,95),'output buffer')
    im=panel(body,'Vending Machine: Configure',minimum=242)
    save(r,'Vending Machine / Owner configuration',im,[-2,-20,178,213],
         'Give the editable name and view switch their own row below the SIU title.',
         'Eight cost/result filter cells, both eight-cell buffers, all six side toggles and the native creative-mode C action.',
         'Keep face direction, auto I/O and item/NBT filter gestures. C remains permission-gated. The full Trade View label replaces the original clipped label.')

    r=ROWS[16];body=blank(184,76)
    text(body,'Cost',1,1);text(body,'Result',111,1)
    # Customer previews are conditional; keep the original empty offer state.
    text(body,'No offer configured',25,24,(103,103,103,255))
    quantity=inset(23,12);text(quantity,'1',4,2,(240,240,240,255));paste(body,quantity,(69,56),'quantity field')
    paste(body,button('Trade',79,20),(102,52),'trade action')
    im=panel(body,'Vending Machine',minimum=198)
    save(r,'Vending Machine / Customer',im,[-2,-2,178,168],
         'A compact trading window, separate from the owner configuration.',
         'The conditional cost/result preview area, editable quantity and complete Trade action. The proposal retains the captured empty-offer state.',
         'Configured offers render their original item previews and arrow in this reserved area. Buying, limits and disabled reasons remain native, not a new trade system.')

    r=ROWS[17];offers=blank(98,160)
    paste(offers,region(r,[4,4,101,16]),(0,0),'search field')
    paste(offers,region(r,[4,18,101,159]),(0,18),'full offer list and four captured offers')
    left=panel(offers,'Offers',minimum=112)
    body=blank(145,58);paste(body,region(r,[131,31,244,61]),(15,8),'merchant payment and result')
    right=panel(body,'Trading Post',minimum=167)
    im=join([(left,0,0),(right,left.width-1,0)])
    save(r,'Trading Post',im,[-2,-2,278,168],
         'Reuse SIU\'s existing Merchant + Offers window pattern.',
         'The search field, every offer, full list scroll area, two payment sockets, arrow and result. No new merchant layout or invented buttons.',
         'Trading Post offer aggregation/search remain native. Existing SIU trade feedback and player inventory integration provide the surrounding behavior.')

    for r in ROWS[18:20]:
        settings='settings' in r['id'];body=blank(162,77)
        if settings:
            text(body,'Slot filters',1,1)
        else:
            # Retain unique native transfer controls at 1:1 scale below the title.
            for i,xx in enumerate([113,135,146,157]):paste(body,region(r,[xx,3,xx+11,14]),(118+i*11,0),'unique native utility control')
            text(body,'Storage',1,1)
        paste(body,region(r,[7,17,169,71]),(0,18),'all 27 grid cells')
        main=panel(body,'Backpack Settings' if settings else 'Backpack',minimum=189,full=True)
        rail=region(r,[176,4,198,79]);parts=[(main,23,0),(rail,main.width+23,22)]
        if not settings:
            # Actual tool sockets live at x=-14. Keep a complete native slot frame.
            tools=frame(24,48)
            paste(tools,inset(18,18),(3,4));paste(tools,inset(18,18),(3,24))
            parts.append((tools,0,40))
        im=join(parts)
        save(r,'Backpack / Settings' if settings else 'Backpack / Storage',im,[-23,-3,202,170],
             'Match the storage and settings views so switching modes keeps one stable window.',
             'All 27 '+('filter' if settings else 'storage')+' cells, complete side tabs'+(' and the Back action.' if settings else ', both tool sockets and the native utility controls.'),
             'Settings, transfers, tools and tank actions remain reachable. Open tank/crafting attachments should use their complete native tabs; no duplicate player inventory is embedded.',
             'The storage proposal exposes the two tool sockets. Optional tank/crafting drawers are closed; their open states use native dimensions and appear only when enabled.')

    r=ROWS[20];body=blank(162,105)
    paste(body,region(r,[7,17,169,89]),(0,0),'36 loot cells with counts')
    ImageDraw.Draw(body).line((0,78,161,78),fill=(126,126,126,255))
    paste(body,region(r,[7,89,169,105]),(0,86),'grave action footer')
    im=panel(body,"UFAudit's Grave",minimum=190,full=True)
    save(r,'Grave storage / Universal Graves',im,[-3,-3,179,206],
         'Use a standard SIU storage window with a distinct native action footer.',
         'All 36 loot cells, item counts and the complete nine-position SGUI action row. Empty action positions stay empty rather than becoming storage.',
         'Loot transfer/sorting excludes the control row. Navigation, grave information and collection actions keep their original server-side handlers.',
         '“Gravestone” in the backlog maps to the audited Universal Graves release, not a different similarly named mod.')

def contact_sheets():
    f=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',17)
    for start in range(0,len(ROWS),6):
        im=Image.new('RGB',(1500,1260),(225,233,234));d=ImageDraw.Draw(im)
        for i,r in enumerate(ROWS[start:start+6]):
            m=META[r['id']];x=(i%2)*750;y=(i//2)*420
            d.text((x+12,y+8),m['mod']+' / '+m['label'],font=f,fill=(25,56,62))
            p=Image.open(ROOT/m['mockup']).convert('RGBA');ratio=min(720/p.width,365/p.height)
            q=p.resize((round(p.width*ratio),round(p.height*ratio)),NEAR)
            im.paste(q,(x+(750-q.width)//2,y+45+(360-q.height)//2),q)
        im.save(TMP/f'mockups-{start//6+1}.png')

def recipes_drawer():
    paper=asset(381,'cell_crafter/recipes.png').crop((0,0,151,176))
    # This is an empty content scaffold, not fabricated unlocked recipes.
    im=panel(paper,'Recipes',(24,37,68,255),minimum=169)
    return im

def make_pdf():
    pdfmetrics.registerFont(TTFont('Segoe','C:/Windows/Fonts/segoeui.ttf'))
    pdfmetrics.registerFont(TTFont('SegoeB','C:/Windows/Fonts/segoeuib.ttf'))
    c=canvas.Canvas(str(PDF),pagesize=(842,595),pageCompression=1)
    c.setTitle('Project Everything | Small UI Window Proposals | Minecraft 1.20.1')
    c.setAuthor('Salts Inventory Update / design review')
    teal='#247C80';ink='#19373D';muted='#5E7076';light='#EEF4F4'
    def txt(s,x,y,size=10,bold=False,color=ink):
        c.setFont('SegoeB' if bold else 'Segoe',size);c.setFillColor(HexColor(color));c.drawString(x,y,s)
    def wrap(s,w,size=10,bold=False):
        out=[]
        for paragraph in s.splitlines():
            line=''
            for word in paragraph.split():
                trial=(line+' '+word).strip()
                if pdfmetrics.stringWidth(trial,'SegoeB' if bold else 'Segoe',size)>w and line:out.append(line);line=word
                else:line=trial
            out.append(line)
        return out
    def para(s,x,y,w,size=10,leading=14,color=muted):
        for line in wrap(s,w,size):txt(line,x,y,size,color=color);y-=leading
        return y
    def start(kicker,title,sub=''):
        c.setFillColor(HexColor('#FFFFFF'));c.rect(0,0,842,595,fill=1,stroke=0)
        txt(kicker.upper(),32,562,9,True,teal);txt(title,32,530,23,True)
        if sub:txt(sub,32,509,9,color=muted)
    def end(kind='Design proposals • Not implemented'):
        c.setStrokeColor(HexColor('#D5E0E1'));c.line(32,36,810,36)
        txt(kind,32,22,8,color=muted);txt(str(c.getPageNumber()),796,22,8,color=muted);c.showPage()
    def image_box(path,x,y,w,h,back=light):
        c.setFillColor(HexColor(back));c.roundRect(x,y,w,h,7,fill=1,stroke=0)
        im=Image.open(path).convert('RGBA');rat=min((w-18)/im.width,(h-18)/im.height)
        # Resample only with integer pixel replication for crisp sprite previews.
        dw=im.width*rat;dh=im.height*rat
        c.drawImage(ImageReader(im),x+(w-dw)/2,y+(h-dh)/2,dw,dh,mask='auto')
    def pilbox(im,x,y,w,h):
        path=TMP/'pdf-inline.png';im.resize((im.width*4,im.height*4),NEAR).save(path);image_box(path,x,y,w,h)

    start('Project Everything / Design atlas','Small interfaces. Native character.','Minecraft 1.20.1 • 13 mods • 20 inventory UI types • 21 native comparison views')
    para('Proposed SIU windows for the remaining mods that have one to three inventory/container interface types. Each comparison pairs a retained in-game screenshot without SIU with a separately authored window concept.',32,473,730,12,17,ink)
    cover=[MOCK['bf-fusion-anvil'],MOCK['the-bumblezone-honey-cocoon'],MOCK['bountiful-bountyboard']]
    pilbox(cover[0],32,229,250,176);pilbox(cover[1],296,229,230,176);pilbox(cover[2],540,229,270,176)
    para('Design direction',32,193,240,12,16,ink)
    para('Preserve the mod’s palette, special slots and workflow. Use SIU for window controls and player-inventory separation. Keep native artwork whole, including shapes that extend beyond the declared menu bounds.',32,167,360,10,14)
    para('How to read this atlas',436,193,360,12,16,ink)
    para('Left: original gameplay capture, cropped for legibility. Right: theoretical SIU composition. These are proposed designs, not screenshots of implemented compatibility or proof that interactions work.',436,167,360,10,14)
    txt('Six native-only mods are accounted for in the appendix.',32,72,10,True,teal)
    end()

    start('Scope / Navigation','The entire small-interface batch','Counts refer to audited inventory UI types, not every screen class or every block skin.')
    groups=[]
    for r in ROWS:
        if not groups or groups[-1][0]!=r['mod']:groups.append((r['mod'],[]))
        groups[-1][1].append(r)
    y=468
    for i,(mod,rr) in enumerate(groups):
        x=32 if i<7 else 433
        yy=468-(i if i<7 else i-7)*51
        types=1 if mod=='Mine Cells' else len(rr)
        txt(mod,x,yy,12,True)
        labels='; '.join(META[r['id']]['label'].replace(' / ',' — ') for r in rr)
        para(labels,x,yy-16,365,9,12)
        pages=','.join(str(4+ROWS.index(r)) for r in rr)
        txt(f'{types} UI type'+('s' if types!=1 else '')+f'  •  pp. {pages}',x,yy-39,8,color=teal)
    para('Mine Cells has one crafter UI type with normal and unbreakable variants; both are shown. Universal Graves duplicate opening paths are consolidated into one storage design. Reskins and conditional attachments retain the same underlying layout rules.',32,72,768,9,12)
    end()

    start('Reference language / Existing SIU windows','What these proposals borrow','These reference images are implemented SIU windows. The following target-mod designs are proposals.')
    references=[('Create / Schematicannon',ROOT/'output/create-native-all/pairs/schematicannon/fixed-ui.png',
                 'Retain native panels and complete sprites; integrate controls into a deliberate header.'),
                ('AE2 family / Portable Cell',ROOT/'output/project-everything-56-implementation/crops/item-megacells-portable-item-cell-1m-core.png',
                 'Use an existing terminal layout: compact storage, search and attached settings/upgrades.'),
                ('Trading / Existing Merchant',ROOT/'output/project-everything-56-implementation/crops/own-supp-red-merchant-core.png',
                 'Reuse the merchant and offers pattern; do not redesign villager trading from scratch.')]
    for i,(label,path,note) in enumerate(references):
        x=32+i*263;txt(label,x,470,11,True);image_box(path,x,245,250,210);para(note,x,224,247,10,14)
    para('Shared rules across every proposal',32,151,730,12,16,ink)
    para('Complete slot frames and input borders • native palette and ghost artwork • utility actions below the title when space is tight • dynamic attachments only when enabled • no world fragments pasted into a panel • no duplicated player inventory • native server behavior remains authoritative.',32,128,758,11,16)
    end('Existing SIU references • Target proposals begin on the next page')

    for idx,r in enumerate(ROWS):
        m=META[r['id']]
        start(f'Project Everything / {idx+1:02d} of {len(ROWS)} native views',m['mod'],m['label']+f" • Minecraft 1.20.1 / {m['loader'].title()} / {m['release']}")
        txt('ORIGINAL IN GAME / SIU ABSENT',32,482,9,True,teal)
        txt('PROPOSED SIU WINDOW / MOCKUP',431,482,9,True,teal)
        image_box(ROOT/m['native'],32,202,379,265)
        image_box(ROOT/m['mockup'],431,202,379,265)
        # The idea is concise; the preservation and behavior columns explain the design.
        y=para(m['idea'],32,181,768,12,16,ink)-10
        txt('KEEP THE ORIGINAL',32,y,8.5,True,teal);txt('WINDOW BEHAVIOR',431,y,8.5,True,teal)
        yl=para(m['preserve'],32,y-16,369,9.3,13)
        yr=para(m['behavior'],431,y-16,369,9.3,13)
        if m['limit']:
            yy=min(yl,yr)-9
            yy=para(m['limit'],32,yy,768,8.5,11)
            assert yy>43,(m['id'],yy)
        end('Native gameplay reference + theoretical SIU mockup • No compatibility code changed')

    start('Interaction concepts / Conditional layouts','What opens beyond the main view','Proposed open states clarify dynamic behavior; they are not additional native capture claims.')
    crafter=MOCK['minecells-cell-crafter'];drawer=recipes_drawer()
    both=join([(drawer,0,0),(crafter,drawer.width-1,0)])
    pilbox(both,32,243,379,234)
    txt('Cell Crafter / attached recipe parchment',32,222,12,True)
    para('The complete native parchment, search and recipe cells are kept in an attached drawer. Selecting an unlocked recipe uses native ingredient/cost/craft widgets. The retained reference world is empty; no invented recipe, item or cost is shown.',32,201,373,10,14)
    backpack=MOCK['item-travelersbackpack-standard']
    tabs=asset(591,'tabs.png');craft=tabs.crop((51,0,116,111))
    opened=join([(backpack,0,0),(craft,backpack.width-2,23)])
    pilbox(opened,431,243,379,234)
    txt('Backpack / native drawer attachment',431,222,12,True)
    para('A complete native 3x3 crafting drawer can attach when that feature is available. Tank/tool drawers use the same rule. Keep native permissions, enable flags and result logic; drawers do not become permanent blank columns.',431,201,373,10,14)
    para('Implementation acceptance: keyboard focus, hover tooltips, scrolling, drag/shift-click, recipe selection, mode changes and server validation must be checked against the original UI. This atlas defines the visual plan; it does not claim those functional checks have run.',32,91,768,10,14)
    end()

    start('Native-only appendix','Six mods that should keep their original screens','Zero inventory-window types means preserve the native screen, not remove the mod from compatibility tracking.')
    native_only=[('DungeonZ','Dungeon and difficulty selection.'),('ElevatorMod','Elevator options/configuration.'),('Sortilege','Knowledge Book; synthetic display slot is not item storage.'),('Armor Statues','Pose and configuration editors.'),('Things','Displacement Tome destination selection.'),('Wraith Waystones','Destination selection and settings.')]
    y=462
    for mod,why in native_only:
        txt(mod,32,y,13,True);para(why,239,y,562,11,15);y-=49
    para('Preservation rule',32,142,750,12,16,ink)
    para('Open the original Minecraft Screen with its own layout, focus, controls and close/back flow. Do not stretch centered artwork to the display and do not add an SIU frame. Scope exclusions to the exact screen; a native-only guide or settings page must not exclude a genuine inventory from the same mod.',32,119,761,10.5,15)
    para('Evidence limit: the saved checklist has prior opening evidence for five entries. Armor Statues has no retained opening pair. This design task ran no new gameplay checks.',32,66,768,9,12)
    end('Native preservation appendix • No window mockup proposed for these screens')

    start('Evidence / Delivery','Reference coverage and review limits','All proposals in this document were composed separately from the original gameplay captures.')
    para('Sources',32,469,360,13,17,ink)
    para('21 retained SIU-absent captures from the pinned 1.20.1 audit. Original files are read-only inputs and SHA-256 checked. GUI sprites come from the exact audited releases; window chrome and controls come from this workspace’s SIU resources.',32,442,359,11,16)
    para('What was checked',431,469,360,13,17,ink)
    para('All 13 selected mods and 20 UI types are represented, including both Cell Crafter variants. Source images and sprites retain their aspect ratios. Mockups and PDF pages are rendered and visually reviewed for cut-off parts, crowded headers, missing actions and layout mistakes.',431,442,365,11,16)
    para('Artifacts saved with the PDF',32,320,360,13,17,ink)
    para('native-references.json — immutable capture provenance\nmockup-manifest.json — per-view design decisions and input hashes\nmockups/ — standalone proposed windows\nnative-crops/ — legible crops of the original captures\nqa.json — scope and artifact verification',32,293,363,10.5,17)
    para('What this does not establish',431,320,360,13,17,ink)
    para('These are theoretical compositions. No game/runtime/API compatibility changes are included, and no functional or gameplay suite was run. A future implementation must use live native renderers and validate all interactions, conditional states and server-defined capacity.',431,293,365,11,16)
    para('Coverage is pinned to the existing 1.20.1 backlog. It is not a claim about every later mod release or every loader. Non-inventory books, configuration and travel selectors stay native; other screen families inside these mods remain outside this window-design scope.',32,151,767,11,16)
    end()
    c.save()

def main():
    OUT.mkdir(exist_ok=True);TMP.mkdir(exist_ok=True);PDF.parent.mkdir(exist_ok=True)
    build();assert len(META)==21
    contact_sheets();make_pdf()
    manifest={'minecraft':'1.20.1','status':'theoretical mockups; no implementation',
              'mods':13,'inventory_ui_types':20,'native_views':21,'pdf':str(PDF.relative_to(ROOT)),
              'cases':[META[r['id']] for r in ROWS],'protected_components':PROTECTED}
    (OUT/'mockup-manifest.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False),encoding='utf-8')
    print(json.dumps({'pdf':str(PDF),'mods':13,'types':20,'views':len(META),'protected_compositions':len(PROTECTED)}))

if __name__=='__main__': main()
