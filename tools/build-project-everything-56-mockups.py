"""Design-only native-reference/SIU proposal atlas; never a gameplay screenshot claim.

All original captures remain unchanged. Proposals are separately named code-native
pixel UI compositions using native artwork and the repository's SIU frame sprites.
"""
from collections import defaultdict
from pathlib import Path
from io import BytesIO
import hashlib,json,math,re,zipfile
from PIL import Image,ImageDraw,ImageFont
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/project-everything-56-design'
TMP=ROOT/'build/project-everything-56-design'
PDF=ROOT/'output/pdf/siu-project-everything-56-native-ui-window-mockups.pdf'
GUI=ROOT/'artwork/shared-resources/core/assets/salts_inventory_update/textures/gui'
FRAME=Image.open(GUI/'window.png').convert('RGBA')
CONTROLS=Image.open(GUI/'window_controls.png').convert('RGBA')
with zipfile.ZipFile(Path.home()/'.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar') as z:
    ASCII=Image.open(BytesIO(z.read('assets/minecraft/textures/font/ascii.png'))).convert('RGBA')
PLAN=json.loads((OUT/'native-reference-plan.json').read_text())
ROWS=PLAN['cases']
RECIPES={}

# Fresh focused references supplement, rather than overwrite, the audit evidence.
for run,capture_id,entry,family in [
    ('1.20.1-fabric-native-prod-design-pr07','design-wands-tools-request',101,'Building Wands'),
    ('1.20.1-fabric-native-prod-design-ag01','design-cooking-book-open',10,"[Let's Do] Farm & Charm")]:
    records=ROOT/'build/remaining-ui-audit/gameplay'/run/'manual-ui-records.json'
    if not records.exists():continue
    record=next((r for r in json.loads(records.read_text()) if r['id']==capture_id),None)
    if not record:continue
    state=record['observation']['client'];assert state['siu_loaded'] is False
    ROWS.append({'id':capture_id,'entry':entry,'family':family,'label':state.get('title',capture_id),
        'minecraft':'1.20.1','loader':'fabric','native_state':state,'bounds':state.get('bounds'),'scale':[2,2],
        'native':{'capture':record['capture'],'sha256':record['sha256'],'crop_rect':[256,280,970,650]}})

def glyph(ch):
    n=ord(ch) if ord(ch)<128 else ord('?')
    im=ASCII.crop(((n%16)*8,(n//16)*8,(n%16)*8+8,(n//16)*8+8));bb=im.getbbox()
    return im,3 if ch==' ' else (bb[2] if bb else 5)

def text_width(s):return sum(glyph(ch)[1]+1 for ch in s)

def mc_text(im,s,x,y,color=(64,64,64,255)):
    for ch in s:
        g,w=glyph(ch);ink=Image.new('RGBA',(8,8),color);ink.putalpha(g.getchannel('A'))
        im.alpha_composite(ink,(int(x),int(y)));x+=w+1

def native_region(row,rect,absolute=False):
    p=ROOT/row['native']['capture'];im=Image.open(p).convert('RGBA')
    assert hashlib.sha256(p.read_bytes()).hexdigest()==row['native']['sha256']
    sx,sy=row['scale'];b=row.get('bounds') or {'x':0,'y':0};ox,oy=(0,0) if absolute else (b['x'],b['y'])
    # Remove only explicitly reviewed old-label pixels in proposed compositions.
    # The left-hand native screenshot always reads the untouched original PNG.
    painter=ImageDraw.Draw(im)
    for a in globals().get('CLEAN_AREAS',{}).get(row['id'],[]):
        color=(71,69,78,255) if row['entry']==18 else (198,198,198,255)
        painter.rectangle((round((b['x']+a[0])*sx),round((b['y']+a[1])*sy),round((b['x']+a[2])*sx)-1,round((b['y']+a[3])*sy)-1),fill=color)
    return im.crop(tuple(round(v) for v in ((ox+rect[0])*sx,(oy+rect[1])*sy,(ox+rect[2])*sx,(oy+rect[3])*sy))).resize((rect[2]-rect[0],rect[3]-rect[1]),Image.Resampling.NEAREST)

def frame(w,h,bg=(198,198,198),dark=False):
    im=Image.new('RGBA',(w,h));parts=[((0,0,5,5),(0,0,5,5)),((6,0,11,5),(w-5,0,w,5)),((0,6,5,11),(0,h-5,5,h)),((6,6,11,11),(w-5,h-5,w,h)),((5,0,6,5),(5,0,w-5,5)),((5,6,6,11),(5,h-5,w-5,h)),((0,5,5,6),(0,5,5,h-5)),((6,5,11,6),(w-5,5,w,h-5)),((5,5,6,6),(5,5,w-5,h-5))]
    sheet=FRAME.copy()
    if bg!=(198,198,198):
        # Recolor chrome only. Never alter the native reference or its evidence.
        def tint(c):
            if c[3]==0:return c
            lum=sum(c[:3])/3
            if lum<10:return c
            if lum>240:return (min(bg[0]+55,255),min(bg[1]+55,255),min(bg[2]+55,255),c[3])
            delta=lum-198
            return tuple(max(0,min(255,round(k+delta))) for k in bg)+(c[3],)
        sheet.putdata([tint(c) for c in sheet.getdata()])
    for src,dst in parts:
        im.alpha_composite(sheet.crop(src).resize((dst[2]-dst[0],dst[3]-dst[1]),Image.Resampling.NEAREST),dst[:2])
    return im

def panel(content,title,bg=(198,198,198),minimum=82,header_extra=0,icon=None):
    w=max(minimum,content.width+12);im=frame(w,content.height+26,bg)
    available=w-39-header_extra-(16 if icon else 0)
    shown=title
    while text_width(shown)>available and shown:shown=shown[:-1]
    if shown!=title:
        while shown and text_width(shown+'...')>available:shown=shown[:-1]
        shown+='...'
    mc_text(im,shown,22 if icon else 6,6,(235,235,235,255) if max(bg)<130 else (64,64,64,255))
    if icon:im.alpha_composite(icon,(6,4))
    for col,x in [(3,w-27),(2,w-14)]:im.alpha_composite(CONTROLS.crop((col*11,0,col*11+11,11)),(x,4))
    im.alpha_composite(content,((w-content.width)//2,20))
    return im

def recipe(ids,rect=None,chunks=None,size=None,note=None,**kw):
    if isinstance(ids,str):ids=[ids]
    for key in ids:RECIPES[key]=dict(rect=rect,chunks=chunks,size=size,note=note,**kw)

# Each non-grid layout was individually reviewed against its full native view.
COOK_NOTE='Book toggle inside the frame; the native recipe panel opens beside it. Keep ingredient, heat, progress and serving artwork complete.'
recipe(['farm-and-charm-cooking-pot','farm-and-charm-roaster','candlelight-cooking-pot','candlelight-cooking-pan'],chunks=[([5,25,25,43],[0,17]),([28,16,146,73],[25,0])],size=(143,57),note=COOK_NOTE)
recipe(['farm-and-charm-stove','candlelight-cobblestone-stove'],chunks=[([5,25,25,43],[0,14]),([28,17,146,68],[25,0])],size=(143,51),note=COOK_NOTE)
recipe('meadow-cooking-cauldron',chunks=[([5,25,25,43],[0,17]),([28,16,146,73],[25,0])],size=(143,57),note=COOK_NOTE)
recipe('meadow-cheese-form',chunks=[([5,25,25,43],[0,17]),([32,7,146,68],[25,0])],size=(139,61),note='Keep the full hourglass and cheese-form processing art. Move the recipe toggle inside the left edge.')
recipe('meadow-fondue',rect=[40,8,139,65],note='Include the upper ingredient slot from its true top edge, plus the full heat and ladle path.')
recipe('meadow-woodcutter',chunks=[([32,14,134,72],[0,0]),([143,33,162,52],[110,18])],size=(129,58),note='Keep the complete recipe selector and scroll track; bring the separated output into one compact body.')
recipe(['nethervinery-crimson-fermentation-barrel','nethervinery-warped-fermentation-barrel'],chunks=[([5,25,25,43],[0,17]),([32,16,147,73],[25,0])],size=(140,57),note='Keep both barrel variants and their full pipe/progress artwork. Recipe toggle is inside the window.')
recipe(['nethervinery-crimson-apple-press','nethervinery-warped-apple-press'],rect=[42,14,137,68],note='Preserve the whole press, including its upper plunger and lower base; trim only unused outer background.')
recipe('shared-ag-bakery-small-pot',chunks=[([5,25,25,43],[0,14]),([28,16,146,73],[25,0])],size=(143,57),note=COOK_NOTE)
recipe('item-candlelight-letter-open',chunks=[([32,12,84,68],[0,0]),([88,8,149,42],[58,0]),([109,47,129,68],[78,39])],size=(119,60),note='Keep the address field, both ingredient slots and the result. Fit the full paper/address header rather than cutting through it.')
recipe('exposure-lightroom',chunks=[([-21,41,-3,59],[0,29]),([-19,91,-1,109],[0,78]),([7,14,169,111],[24,0])],size=(186,97),note='Film input and chromatic-process toggle move inside a utility column. Preserve every film frame, chemical slot, preview and Print control.')
recipe('item-exposure-album',rect=[0,0,298,188],style='book',note='Keep both complete pages, captions, page turns and binding. The signing action stays on a small attached book tab.',album_sign=True)
recipe('own-exposure-lectern',rect=[0,0,298,188],style='book',note='Keep the whole album and both page controls. Done and Take Book stay in a compact action strip below.',lectern=True)
recipe('exposure-camera',rect=[12,14,165,89],note='Keep the complete camera preview and all attachment slots; remove only the separate player-inventory band.')
recipe('exposure-film-rename',chunks=[([3,15,22,34],[0,0]),([31,20,175,34],[25,2]),([132,41,174,62],[127,25])],size=(169,46),note='Keep the item, editable name, Apply and Cancel. All actions are inside the compact rename window.')
recipe(['smallships-brigg','ag-entity-galley'],chunks=[([7,17,169,125],[0,0]),([114,124,170,138],[106,112]),([176,17,252,126],[168,0])],size=(244,126),note='Preserve all 54 visible cargo slots, the full ship statistics card and both page buttons. The pager is below the cargo grid.')
recipe('ag-niftycarts-plow',rect=[56,16,121,54],note='Keep the three tool sockets and the entire plow connector shape; compact the empty surround.')
recipe('charm-kiln',rect=[19,15,137,71],note='Keep the recipe toggle, flame and smelting arrow inside the window. The recipe panel can attach outside when opened.')
recipe('charm-woodcutter',rect=[18,14,164,72],note='Retain the full recipe selector, scrollbar, input and output; no clipped panel top edge.')
recipe('minecraft-anvil',chunks=[([59,19,168,35],[0,0]),([26,45,154,66],[0,24])],size=(128,56),note='Name field above both inputs and result; reserve the final row for the live repair cost/status. Complete hammer is an optional header icon.',header_icon=[14,5,51,35])
recipe('minecraft-enchanting-table',rect=[12,13,169,73],note='Preserve the complete animated-book viewport, input/lapis slots and all three enchantment choices. This reference shows the empty state.')
recipe(['minecraft-crafting-table','bb-bwg-pine-workbench'],chunks=[([5,34,25,53],[0,18]),([29,16,145,72],[25,0])],size=(141,56),note='Crafting book toggle remains inside. Keep the full 3x3 grid, arrow and result; do not keep the redundant player inventory.')
recipe(['guard-villager-inventory','bf-forge-guard'],chunks=[([7,7,79,79],[0,0]),([79,17,162,73],[78,8])],size=(161,72),note='Keep every equipment socket, the full NPC portrait, health/armor readout and both guard action buttons. This is the guard state, not the player HUD.')
recipe('bb-polymer-creative-browser',rect=[7,17,169,71],note='Keep the group grid and search entry. Native hover tooltip is transient; it must not become permanent window artwork.')
recipe('bb-spawn-hamster',rect=[7,7,169,72],note='Include the full hamster portrait and every owned storage socket, including the top edge of the ears.')
recipe('bb-charm-atlas',chunks=[([7,17,25,72],[0,12]),([58,2,142,74],[25,0]),([151,2,169,20],[115,0])],size=(133,72),note='Keep the complete map, all direction/zoom controls and map-storage slots. The native title is replaced by the SIU title bar.')
recipe(['blue-skies-snowcap-oven','blue-skies-horizonite-forge'],rect=[19,15,138,73],note='Keep every input/output and both processing lanes where present. Full flame/progress artwork remains inside the frame.')
recipe('blue-skies-summoning-table',rect=[14,15,164,69],note='Preserve both mode actions and their input sockets; keep the action labels readable in a compact panel.')
recipe('blue-skies-alchemy-table',rect=[11,12,165,73],note='Keep the complete ingredient frame, recipe grid, scrollbar and result, including decorative pixels above the first slot.')
recipe('blue-skies-food-prep-table',rect=[42,16,135,82],note='Keep all five ingredients, result/status marks and the complete chopping-board base below the output.')
recipe('stalwart-dungeons-teleporter',rect=[78,38,98,58],minimum=88,note='One socket needs one small window. Keep a compact overflow/close header instead of a chest-sized blank body.')
recipe(['bf-wolf-isolated','bf-moa-clean','pr-mythic-direwolf-inventory'],rect=[7,7,80,73],note='Keep all equipment sockets and the full live creature viewport. Add storage only when that native mount/state actually provides it.')
recipe('bf-gallery-painting-selector',rect=[11,14,169,124],note='Keep the full easel preview, painting selector, search and scroll controls. The native hover tooltip is excluded from permanent UI art.',clean_tooltip=True)
recipe(['bf-sculptor-clean','own-bf-umvuthi-aligned','own-bf-minion-trade-real'],chunks=[([7,7,66,74],[0,0]),([69,18,171,74],[65,6])],size=(167,67),note='Full animated trader portrait beside the complete currency, reward and native trade/test action. No portrait is cut at the top.')
recipe(['bf-waystone','own-bf-sharestone-real-aligned'],custom='waystone',note='Rebuild native search, destinations, cost/actions and pagination on an opaque SIU list panel. Keep settings beside each destination, with no world pixels pasted into the UI.')
recipe('bf-warp-plate',rect=[51,16,126,91],note='Keep the complete five-slot cross. Put activation/status feedback in a compact line below; do not clip it into the title.',status='Warp plate status')
recipe('own-bf-bag-of-spoils-aligned',rect=[43,18,134,38],note='A compact five-slot reward row; remove the large empty surround and the duplicate player inventory.')
recipe('own-bf-camel-real-aligned',rect=[7,7,169,73],note='Keep the entire camel viewport, saddle socket and chested storage grid. The frame hugs the actual populated layout.')
recipe('own-ae-part-cell-dock',rect=[79,36,98,55],minimum=92,gear=True,note='Compact dock socket with the native priority/settings action moved inside a utility row. Optional upgrade rails attach only when visible.')
recipe('own-ae-part-requester-terminal',rect=[7,17,189,78],search=[103,4,182,14],note='Keep search, the requester list and full scrollbar. The empty-list message is inside the same viewport used by populated results.')
recipe('own-supp-red-merchant',custom='merchant',note='Keep every offer in a full-height attached list. The compact trade body retains its warning, payment/result slots and offer labels.')
recipe('own-supp-cannon',rect=[7,7,169,73],note='Keep the full charge gauge, powder/projectile path and aiming/control card. No right-side buttons are clipped.')
recipe('own-supp-present',rect=[11,15,161,71],note='Keep the whole recipient note, item slot and seal/control; move the gift title to the header.')
recipe('own-supp-trapped-present',rect=[56,25,116,57],note='Keep the complete ribbon above the gift socket and the spring control, with a small consistent frame margin.')
recipe('pr-botania-bauble-box',rect=[7,7,169,80],note='Keep the entire player preview and trinket-case grid. The preview has its own viewport and cannot render over the slots.')
recipe('ad-astra-giselle-addon-automation-nasa-workbench',rect=[8,19,166,130],bg=(71,69,78),note='Preserve the complete rocket-shaped ingredient graph, result and power gauge. A native auxiliary/upgrade rail attaches only when it is visible.',native_rail=True)
recipe('ad-astra-giselle-addon-fuel-loader',rect=[61,23,113,81],bg=(71,69,78),minimum=96,note='Keep the full fuel tank and both transfer controls. The compact dark metal frame follows the native machine palette.')
recipe('ad-astra-giselle-addon-gravity-normalizer',rect=[5,17,165,114],bg=(71,69,78),note='All dimensions, offsets, centering action, power gauge and consumption readout remain visible. Optional auxiliary rail stays attached.',native_rail=True)
recipe('ad-astra-giselle-addon-rocket-sensor',rect=[7,17,169,173],bg=(71,69,78),note='Keep all seven detection modes, Invert Output and current output. Hover tooltips remain transient rather than part of the background.',clean_tooltip=True)
recipe('merequester-requester',rect=[7,17,189,116],note='Keep every visible request row, enable flags, amount fields and the full scroll track. No redundant player inventory.')
recipe('megacells-mega-interface',chunks=[([7,35,169,87],[0,0]),([7,97,169,142],[0,58])],size=(162,103),gear=True,note='Keep configuration controls and both owned item grids. Settings/upgrade rails stay attached; remove only the player-inventory section.')
recipe('megacells-mega-pattern-provider',chunks=[([7,33,169,81],[0,0]),([7,87,169,115],[0,54])],size=(162,82),gear=True,note='Keep both pattern rows, return inventory and their labels. Keep native side controls attached when active.')
recipe(['item-megacells-portable-item-cell-1m','item-megacells-portable-fluid-cell-1m'],rect=[7,17,189,72],search=[81,4,161,14],note='Search above the complete terminal grid and scrollbar; optional left settings and right upgrades attach as slim rails instead of blank columns.')
recipe('herbalbrews-cauldron',rect=[55,15,165,73],note='Keep all three ingredient branches, the complete cauldron/drain and result. Do not trim any pipe or progress pixels.')
recipe(['herbalbrews-copper-tea-kettle','herbalbrews-tea-kettle'],rect=[11,15,166,80],note='Keep the entire kettle path, heat base and both tall gauges. The processing base below the usual content band must not be cut off.')
recipe('vinery-apple-press',rect=[39,14,137,68],note='Keep the full press top/base, input and liquid/serving outputs, retaining the native brown slot treatment.')
recipe('vinery-basket',rect=[60,16,115,72],note='Reframe only the basket grid. Do not copy world-colored cutouts from the native shaped background.')
recipe('vinery-fermentation-barrel',rect=[38,16,141,73],note='Keep the entire barrel and colored pipe/coil diagram, including the lower collector row.')
recipe('archon-scripture-table',rect=[14,15,158,73],note='Keep the reagent/book endpoints, all three lower sockets and their complete branching connectors.')
recipe('spell-engine-spell-binding',rect=[13,13,169,73],note='Keep spellbook/reagent sockets and all binding-choice rows. The list stays aligned with the native selection/status behavior.')
recipe('supplementaries-lunch-basket',rect=[60,24,116,62],note='A compact two-row basket grid with even padding on all sides.')
recipe('supplementaries-notice-board',rect=[33,12,144,77],note='Keep the full notice paper and its item socket. No top or bottom paper edge is cut off.')
recipe('supplementaries-pulley-block',rect=[68,28,106,67],minimum=88,note='Keep the entire rope socket and its decorative surround in a small single-purpose window.')
recipe('supplementaries-sack',rect=[60,15,116,71],note='A compact 3x3 sack grid; size follows the owned slots, not the native player-inventory width.')
recipe('transmog-transmogrification-table',rect=[9,15,163,60],note='Preserve the reagent indicator, energy symbol, both inputs and result. Include a small live status line when needed.')
recipe('villagersplus-alchemist-table',rect=[14,15,120,79],note='Keep the warning icon, all bottle sockets, heat/coil and the full connecting pipework.')
recipe('item-archon-experience-pouch',rect=[13,7,224,98],bg=(54,54,54),note='Keep Add XP and Remove XP groups, all amount buttons and the balance readout. The dark body and legible white labels are intentional.')
recipe('item-botania-flower-bag',rect=[15,24,161,98],note='Retain the full colored-flower category grid and its native item artwork, without a duplicate player inventory.')
recipe('item-wands-palette',rect=[7,17,169,126],note='Keep all 54 palette cells. Move the random-mode checkbox/label into a slim row above the grid.',palette=True)
recipe('item-wands-magic-bag-1',rect=[77,17,117,60],bg=(134,134,134),minimum=96,note='Keep the help affordance, bag socket, bag marker and capacity count. The count must not be clipped off at the lower edge.')
recipe('pr-tierify-reforge',rect=[16,13,133,73],note='Keep the entire hammer, reforge input/payment/result arrangement and native action. Anvil/reforge modes use attached tabs.',tier_tabs=True)
recipe('design-wands-tools-request',chunks=[([7,30,170,49],[0,0]),([7,83,169,159],[0,25])],size=(163,101),note='Tool picker keeps its clickable player-inventory grid: here those slots select the wand tool, so removing them would remove the purpose of the UI. Native visual opened through the original mod key-request packet; raw-key opening is not validated.')
recipe('design-cooking-book-open',chunks=[([5,25,25,43],[0,17]),([28,16,146,72],[25,0])],size=(143,56),book_open=True,note='Open state: the recipe button remains inside the compact cooking window. The native recipe panel and category tabs attach beside it without a redundant player-inventory band.')

# Do not accidentally keep the top pixel of the old Inventory label.
for key,q in RECIPES.items():
    if key.startswith(('farm-and-charm','candlelight-cooking','candlelight-cobblestone','shared-ag-bakery','nethervinery-')) or key=='meadow-cooking-cauldron':
        for rect,at in q.get('chunks') or []:
            if rect[3]==73:rect[3]=72
        if q.get('size') and q['size'][1]==57:q['size']=(q['size'][0],56)
    if key.startswith('ad-astra-giselle-addon-'):q['machine_utility_strip']=True

# Corrections from the first visual pass: full frames, no legacy label fragments.
for key in ['farm-and-charm-stove','candlelight-cobblestone-stove']:
    RECIPES[key].update(chunks=[([5,25,25,43],[0,14]),([28,17,150,68],[25,0])],size=(147,51))
RECIPES['meadow-cheese-form'].update(chunks=[([5,25,25,43],[0,17]),([28,7,150,60],[25,0])],size=(147,53))
RECIPES['meadow-woodcutter'].update(chunks=[([30,14,134,72],[0,0]),([137,27,162,55],[111,16])],size=(136,58))
RECIPES['item-candlelight-letter-open'].update(chunks=[([32,12,84,68],[0,0]),([88,8,149,40],[58,0]),([99,41,138,72],[68,33])],size=(119,64))
for key in ['herbalbrews-copper-tea-kettle','herbalbrews-tea-kettle']:
    RECIPES[key].update(rect=None,chunks=[([11,11,166,72],[0,0]),([94,72,166,80],[83,61])],size=(155,69))
for key in ['bf-wolf-isolated','bf-moa-clean','pr-mythic-direwolf-inventory']:
    RECIPES[key]['rect']=[7,17,80,72]
RECIPES['own-bf-camel-real-aligned']['rect']=[7,17,169,72]
for key in ['guard-villager-inventory','bf-forge-guard']:
    RECIPES[key].update(chunks=[([7,7,77,80],[0,0]),([76,43,95,80],[74,36]),([79,17,162,41],[74,0]),([99,43,141,63],[99,36])],size=(157,73))
RECIPES['bb-spawn-hamster']['rect']=[7,16,169,72]
RECIPES['bb-polymer-creative-browser']['rect']=[7,17,169,126]
RECIPES['bb-polymer-creative-browser']['note']='Keep all 54 native group/control cells, including the lower rows and search entry. Do not turn a transient hover tooltip into permanent artwork.'
for key in ['blue-skies-snowcap-oven','blue-skies-horizonite-forge']:
    RECIPES[key]['rect'][3]=72
RECIPES['blue-skies-alchemy-table']['rect']=[11,14,165,72]
RECIPES['minecraft-enchanting-table']['rect'][3]=72
RECIPES['bb-charm-atlas'].update(chunks=[([7,17,25,71],[0,12]),([61,4,135,77],[24,0]),([156,4,173,21],[104,0]),([159,72,169,82],[108,61])],size=(121,78))
RECIPES['supplementaries-notice-board'].update(rect=None,chunks=[([33,32,53,52],[0,22]),([80,9,144,73],[26,0])],size=(90,64))
RECIPES['own-supp-cannon'].update(rect=None,chunks=[([7,17,123,72],[0,8]),([123,9,169,72],[119,0])],size=(165,63))
RECIPES['spell-engine-spell-binding']['rect']=[13,15,169,72]
RECIPES['villagersplus-alchemist-table']['rect'][3]=76
RECIPES['exposure-lightroom']['chunks']= [([-21,41,-3,59],[0,29]),([7,14,169,111],[24,0])]
RECIPES['exposure-lightroom']['chromatic_button']=True
for key in ['bf-sculptor-clean','own-bf-umvuthi-aligned','own-bf-minion-trade-real']:
    RECIPES[key]['chunks'][0][0][3]=72
for key in ['smallships-brigg','ag-entity-galley']:
    RECIPES[key]['chunks'][2]=([177,17,256,124],[168,0]);RECIPES[key]['size']=(247,126);RECIPES[key]['hires_ship']=True
RECIPES['megacells-mega-interface'].update(chunks=[([7,23,70,35],[0,0]),([7,52,169,89],[0,15]),([7,106,169,143],[0,65])],size=(162,102))
RECIPES['pr-tierify-reforge'].update(rect=None,chunks=[([16,5,48,39],[0,10]),([44,46,62,64],[39,25]),([79,33,97,51],[74,10]),([114,46,132,64],[109,25]),([79,54,99,74],[73,35])],size=(127,56))
CLEAN_AREAS={
    'shared-pr-nether-brewing-stand':[[7,72,64,82]],
    'villagersplus-alchemist-table':[[7,72,64,82]],
    'blue-skies-food-prep-table':[[7,72,61,82]],
    'ad-astra-giselle-addon-gravity-normalizer':[[7,104,91,114]],
    'herbalbrews-copper-tea-kettle':[[56,5,140,15]],
    'herbalbrews-tea-kettle':[[56,5,140,15]],
    'bf-gallery-painting-selector':[[8,5,78,15]],
    'blue-skies-alchemy-table':[[156,71,170,83]],
}
RECIPES['ad-astra-giselle-addon-gravity-normalizer']['rect'][3]=112
RECIPES['meadow-woodcutter']['chunks']= [([30,14,134,72],[0,0])]
RECIPES['meadow-woodcutter']['output_reframe']=True
for key in ['shared-bf-ascended-furnace','shared-pr-nether-furnace','shared-pr-cincinnasite-forge','shared-pr-oven']:
    recipe(key,rect=[19,15,138,71],note='Keep the native recipe toggle inside the frame along with input, fuel, flame and output. The open recipe panel attaches beside the window.')

# Prefer the fresh native view with the pointer off the Rocket Sensor controls.
_clean_record=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-native-prod-design-pr07/manual-ui-records.json'
if _clean_record.exists():
    _fresh=next((x for x in json.loads(_clean_record.read_text()) if x['id']=='design-rocket-clean'),None)
    if _fresh:
        _row=next(r for r in ROWS if r['id']=='ad-astra-giselle-addon-rocket-sensor')
        _row['native'].update(capture=_fresh['capture'],sha256=_fresh['sha256']);_row['native_state']=_fresh['observation']['client'];_row['bounds']=_row['native_state']['bounds']
_gallery_records=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-forge-native-design-gallery01/manual-ui-records.json'
if _gallery_records.exists():
    _fresh=next((x for x in json.loads(_gallery_records.read_text()) if x['id']=='design-gallery-selected-clean'),None)
    if _fresh:
        _row=next(r for r in ROWS if r['id']=='bf-gallery-painting-selector')
        _row['native'].update(capture=_fresh['capture'],sha256=_fresh['sha256']);_row['native_state']=_fresh['observation']['client'];_row['bounds']=_row['native_state']['bounds']
RECIPES['bf-gallery-painting-selector'].update(rect=[11,9,169,116],note='Keep the complete easel, selected painting, search field, painting grid and full scrollbar. This fresh native reference has no hover tooltip covering its controls.')
RECIPES['ad-astra-giselle-addon-rocket-sensor']['note']='Keep all native detection modes, Invert Output and current output, plus the three machine utility actions. The original dark metal palette is preserved; hover tooltips remain transient.'
RECIPES['ad-astra-giselle-addon-fuel-loader']['note']='Keep the full fuel tank, both transfer controls and all three native machine utility actions. The compact frame matches the original dark metal palette.'
RECIPES['bf-waystone']['note']='Keep search, destinations, per-destination settings and pagination on an opaque SIU panel. Grow the list with its content; do not keep a tall empty list for one destination.'
RECIPES['own-bf-sharestone-real-aligned']['note']='Keep native destination selection and pagination on an opaque panel. The list grows for more destinations; avoid a tall empty body in the one-entry state.'
_bb_records=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-native-prod-design-bb01/manual-ui-records.json'
if _bb_records.exists():
    _records=json.loads(_bb_records.read_text())
    for _case,_capture in [('bb-polymer-creative-browser','design-polymer-clean'),('minecraft-enchanting-table','design-easymagic-active-clean')]:
        _fresh=next((x for x in _records if x['id']==_capture),None)
        if _fresh:
            _row=next(r for r in ROWS if r['id']==_case);_row['native'].update(capture=_fresh['capture'],sha256=_fresh['sha256']);_row['native_state']=_fresh['observation']['client'];_row['bounds']=_row['native_state']['bounds']
RECIPES['minecraft-enchanting-table']['note']='Keep the active reroll control and its costs, both item/lapis slots and all three enchantment choices. The native empty inset becomes these controls when an item is inserted.'
RECIPES['megacells-mega-interface']['native_rails']={'left':0,'right':1,'slot_x':186}
RECIPES['megacells-mega-pattern-provider']['native_rails']={'left':3,'right':0}
RECIPES['item-megacells-portable-item-cell-1m']['native_rails']={'left':4,'right':4,'slot_x':205}
RECIPES['item-megacells-portable-fluid-cell-1m']['native_rails']={'left':4,'right':3,'slot_x':205}

def title_for(r):
    overrides={'meadow-cheese-form':'Cheese Form','meadow-cooking-cauldron':'Cooking Cauldron','meadow-fondue':'Fondue','item-candlelight-letter-open':'Letter','own-ae-part-cell-dock':'ME Cell Dock','own-ae-part-requester-terminal':'ME Requester Terminal','merequester-requester':'ME Requester','megacells-mega-interface':'MEGA Interface','megacells-mega-pattern-provider':'MEGA Pattern Provider','item-megacells-portable-item-cell-1m':'Portable Item Cell','item-megacells-portable-fluid-cell-1m':'Portable Fluid Cell','bf-waystone':'Waystones','own-bf-sharestone-real-aligned':'Sharestones','pr-tierify-reforge':'Reforge Gear'}
    return overrides.get(r['id'],r['native_state'].get('title') or r['label'].replace('-',' ').title())

def propose(r):
    q=RECIPES.get(r['id'])
    if q is None:
        # All remaining cases are observed regular storage/filter grids.
        owned=[s for s in r['native_state'].get('slots',[]) if not s.get('player') and 0<=s['x']<(r.get('bounds')or{}).get('width',999)]
        if not owned:raise ValueError('Unreviewed nongrid '+r['id'])
        q={'rect':[min(s['x'] for s in owned)-1,min(s['y'] for s in owned)-1,max(s['x'] for s in owned)+17,max(s['y'] for s in owned)+17],
           'note':'Keep the complete native storage grid and item artwork. Even narrow margins replace the unused player-inventory band.'}
        if 'itemfilters' in r['id']:q['note']='Keep the full ghost-filter grid and AND/OR identity. These remain filter entries, not ordinary movable storage items.'
        RECIPES[r['id']]=q
    bg=tuple(q.get('bg',(198,198,198)))
    if q.get('custom')=='waystone':
        # Deliberate vector-like widget redraw; native screen is transparent.
        content=Image.new('RGBA',(218,77),bg+(255,));d=ImageDraw.Draw(content)
        d.rectangle((0,0,217,15),fill='#353535',outline='#eeeeee');mc_text(content,'Search destinations...',4,4,(180,180,180,255))
        d.rectangle((0,20,217,50),fill='#8b8b8b',outline='#555555')
        d.rectangle((4,25,201,44),fill='#5c5c5c',outline='#ffffff');mc_text(content,'<unnamed>',10,31,(235,235,235,255))
        if r['id']=='bf-waystone':mc_text(content,'...',204,31)
        for x,t in [(0,'Previous'),(114,'Next')]:
            d.rectangle((x,58,x+103,76),fill='#777777',outline='#eeeeee');mc_text(content,t,x+8,64,(235,235,235,255))
        return panel(content,title_for(r)),q
    if q.get('custom')=='merchant':
        offers=panel(native_region(r,[7,17,102,159]),'Offers',minimum=107)
        trade=panel(native_region(r,[126,2,262,63]),'Red Merchant',minimum=148)
        result=Image.new('RGBA',(offers.width+trade.width+3,max(offers.height,trade.height)))
        result.alpha_composite(offers,(0,0));result.alpha_composite(trade,(offers.width+3,0));return result,q
    if q.get('chunks'):
        content=Image.new('RGBA',tuple(q['size']),bg+(255,))
        for rect,at in q['chunks']:content.alpha_composite(native_region(r,rect),tuple(at))
    else:content=native_region(r,q['rect'])
    if q.get('style')=='book':bg=(146,106,67)
    if q.get('chromatic_button'):
        button=frame(18,18);d=ImageDraw.Draw(button)
        for x,col in [(4,'#c75252'),(8,'#5ba96d'),(12,'#5b82c5')]:d.rectangle((x,5,x+2,12),fill=col)
        content.alpha_composite(button,(0,78))
    if q.get('machine_utility_strip'):
        rail=native_region(r,[104,-32,176,-2]);new=Image.new('RGBA',(max(content.width,rail.width),content.height+34),bg+(255,));new.alpha_composite(rail,(0,0));new.alpha_composite(content,((new.width-content.width)//2,34));content=new
    extra=0
    if q.get('palette'):
        strip=Image.new('RGBA',(content.width,12),bg+(255,));strip.alpha_composite(native_region(r,[48,3,113,13]),(0,0))
        new=Image.new('RGBA',(content.width,content.height+15),bg+(255,));new.alpha_composite(strip,(0,0));new.alpha_composite(content,(0,15));content=new
    if q.get('search'):
        field=native_region(r,q['search']);new=Image.new('RGBA',(content.width,content.height+15),bg+(255,));new.alpha_composite(field,(0,0));new.alpha_composite(content,(0,15));content=new
    if q.get('gear'):
        # Keep a native action, separate from SIU management controls.
        gear=frame(18,18);gear.alpha_composite(native_region(r,[158,5,170,17]),(3,3))
        if content.width<80:
            new=Image.new('RGBA',(62,max(19,content.height)),bg+(255,));new.alpha_composite(content,(0,0));new.alpha_composite(gear,(44,0));content=new
    if q.get('status'):
        new=Image.new('RGBA',(max(100,content.width),content.height+13),bg+(255,));new.alpha_composite(content,((new.width-content.width)//2,0));new.alpha_composite(native_region(r,[122,4,172,14]),(25,content.height+3));content=new
    if q.get('lectern'):
        new=Image.new('RGBA',(content.width,content.height+26),bg+(255,));new.alpha_composite(content,(0,0));new.alpha_composite(native_region(r,[49,196,249,216]),((content.width-200)//2,content.height+3));content=new
    if q.get('output_reframe'):
        d=ImageDraw.Draw(content);d.rectangle((111,16,134,39),fill='#8b8b8b');d.line((111,39,134,39,134,16),fill='#ffffff');d.line((111,39,111,16,134,16),fill='#373737')
        content.alpha_composite(native_region(r,[144,34,160,50]),(115,20))
    icon=None
    if q.get('header_icon'):
        icon=native_region(r,q['header_icon']);icon.thumbnail((12,12),Image.Resampling.NEAREST)
    result=panel(content,title_for(r),bg,q.get('minimum',82),header_extra=20 if q.get('gear') and content.width>=80 else 0,icon=icon)
    if q.get('gear') and content.width>=80:result.alpha_composite(gear,(result.width-47,2))
    if q.get('book_open'):
        book=Image.new('RGBA',(178,166))
        book.alpha_composite(native_region(r,[161,142,307,308],absolute=True),(32,0))
        for rect,at in [([129,145,163,171],[0,3]),([132,172,161,196],[3,30]),([132,199,161,223],[3,57])]:book.alpha_composite(native_region(r,rect,absolute=True),at)
        new=Image.new('RGBA',(book.width+result.width+3,max(book.height,result.height)));new.alpha_composite(book,(0,0));new.alpha_composite(result,(book.width+3,0));result=new
    if q.get('album_sign'):
        sign=frame(24,22,bg);mc_text(sign,'Sign',2,7);new=Image.new('RGBA',(result.width+25,result.height));new.alpha_composite(result,(25,0));new.alpha_composite(sign,(3,24));result=new
    if q.get('tier_tabs'):
        # Code-native attached tabs from the vanilla anvil identity and full hammer.
        tab_h=24;new=Image.new('RGBA',(result.width,result.height+tab_h-3));new.alpha_composite(result,(0,tab_h-3))
        d=ImageDraw.Draw(new)
        for x,selected in [(0,False),(35,True)]:
            t=frame(35 if not selected else 25,27 if selected else 24)
            new.alpha_composite(t,(x,0 if selected else 2))
            if selected:d.rectangle((x+4,23,x+20,27),fill=bg+(255,))
        mc_text(new,'Anvil',3,8);new.alpha_composite(native_region(r,[16,5,48,39]).resize((16,17),Image.Resampling.NEAREST),(39,5));result=new
    if q.get('native_rails'):
        rail=q['native_rails'];left=19 if rail['left'] else 0;right=29 if rail['right'] else 0
        new=Image.new('RGBA',(left+result.width+right,max(result.height,20+rail['left']*20,20+rail['right']*18+8)))
        new.alpha_composite(result,(left,0))
        for i in range(rail['left']):new.alpha_composite(native_region(r,[-20,8+i*20,-4,24+i*20]),(0,20+i*20))
        if rail['right']:
            side=frame(26,rail['right']*18+8)
            for i in range(rail['right']):
                sx=rail['slot_x'];side.alpha_composite(native_region(r,[sx-1,7+i*18,sx+17,25+i*18]),(4,4+i*18))
            new.alpha_composite(side,(left+result.width+3,20))
        result=new
    if q.get('hires_ship') or q.get('style')=='book' or q.get('status'):
        result=result.resize((result.width*2,result.height*2),Image.Resampling.NEAREST)
        raw=Image.open(ROOT/r['native']['capture']).convert('RGBA');b=r['bounds']
        def overlay(rect,at):
            crop=raw.crop(tuple(round(v*2) for v in (b['x']+rect[0],b['y']+rect[1],b['x']+rect[2],b['y']+rect[3])));result.alpha_composite(crop,(at[0]*2,at[1]*2))
        if q.get('hires_ship'):
            rect=[177,17,256,124];crop=raw.crop(tuple(round(v*2) for v in (b['x']+rect[0],b['y']+rect[1],b['x']+rect[2],b['y']+rect[3])))
            mask=Image.new('L',crop.size,0);ImageDraw.Draw(mask).rounded_rectangle((0,0,crop.width-1,crop.height-1),radius=4,fill=255);crop.putalpha(mask)
            ImageDraw.Draw(result).rectangle((348,40,348+crop.width-1,40+crop.height-1),fill=bg+(255,));result.alpha_composite(crop,(348,40))
        if q.get('style')=='book':overlay([0,0,298,188],[31 if q.get('album_sign') else 6,20])
        if q.get('status'):overlay([122,4,172,14],[31,98])
        result.info['units']=2
    return result,q

def native_display(r):
    b=r.get('bounds');sx,sy=r['scale'];im=Image.open(ROOT/r['native']['capture']).convert('RGB')
    if b and b['width']<400:
        left=-25 if r['entry']==219 or r['entry'] in (375,) else -3
        top=-34 if r['entry']==18 else -3
        rails=RECIPES.get(r['id'],{}).get('native_rails',{})
        right=b['width']+39 if rails.get('right') else b['width']+3
        bottom=b['height']+30 if r['id']=='own-exposure-lectern' else b['height']+3
        rect=[(b['x']+left)*sx,(b['y']+top)*sy,(b['x']+right)*sx,(b['y']+bottom)*sy]
    else:rect=r['native']['crop_rect']
    if r['id']=='design-cooking-book-open':rect=[254,280,972,651]
    if r['id'] in ('bf-waystone','own-bf-sharestone-real-aligned'):
        rect=[210*sx,(b['y']-3)*sy,463*sx,(b['y']+189)*sy]
    return im.crop(tuple(round(x) for x in rect))

def reference_release(row,entry):
    """Select the release actually staged, not the first compatible release."""
    stage=ROOT/Path(row['native']['capture']).parent.parent/'staged-artifacts.json'
    artifacts=json.loads(stage.read_text(encoding='utf-8-sig'))['artifacts']
    hits=[]
    for release in entry['releases']:
        original=release.get('original_path','').replace('\\','/')
        parent=original.split('!/')[0].rsplit('/',1)[-1]
        if any(Path(a.get('file',a.get('path',''))).name==Path(release['path']).name
               or (a.get('sha512') and a['sha512']==release.get('sha512'))
               or ('!/' in original and a.get('filename')==parent)
               for a in artifacts):hits.append(release)
    if len(hits)>1:
        log=stage.with_name('launch.log')
        if log.exists():
            text=log.read_text(encoding='utf-8',errors='replace')
            loaded=[h for h in hits if any(re.search(r'\b'+re.escape(mid)+r'\s+'+re.escape(h['version'])+r'(?:\s|$)',text) for mid in entry['mod_ids'])]
            if len(loaded)==1:hits=loaded
    if len(hits)!=1:raise ValueError('Ambiguous staged subject release: '+row['id']+' '+str([x['version'] for x in hits]))
    return hits[0]

def wrap(c,text,width,font='Helvetica',size=10):
    words=text.split();lines=[];line=''
    for word in words:
        candidate=(line+' '+word).strip()
        if c.stringWidth(candidate,font,size)>width and line:lines.append(line);line=word
        else:line=candidate
    if line:lines.append(line)
    return lines

PW,PH=842,595
NAVY='#20363c';TEAL='#087a7e';MUTED='#54686d';PAPER='#f0f5f5'

def paragraph(c,text,x,y,width,size=10,color=MUTED,leading=14):
    c.setFillColor(HexColor(color));c.setFont('Helvetica',size)
    for line in wrap(c,text,width,size=size):c.drawString(x,y,line);y-=leading
    return y

def heading(c,family,title):
    c.setFillColor(HexColor(TEAL));c.setFont('Helvetica-Bold',10);c.drawString(32,PH-32,'PROJECT EVERYTHING / WINDOW DESIGN REVIEW')
    c.setFillColor(HexColor(NAVY));c.setFont('Helvetica-Bold',19)
    for i,line in enumerate(wrap(c,family,778,'Helvetica-Bold',19)[:2]):c.drawString(32,PH-61-i*23,line)
    c.setFont('Helvetica',12);c.setFillColor(HexColor(MUTED));c.drawString(32,PH-90,title)

def footer(c,page,tag='Native screenshots + proposed mockups | No compatibility implementation'):
    c.setStrokeColor(HexColor('#d6e0e1'));c.line(32,28,810,28);c.setFont('Helvetica',8);c.setFillColor(HexColor(MUTED));c.drawString(32,16,tag);c.drawRightString(810,16,str(page))

def draw_image(c,im,box,nearest=True,max_scale=None):
    x,y,w,h=box;scale=min(w/im.width,h/im.height)
    if max_scale:scale=min(scale,max_scale)
    dw,dh=im.width*scale,im.height*scale
    # Upscale in integer pixels before PDF placement to preserve pixel-art edges.
    if nearest and scale>1:im=im.resize((im.width*math.ceil(scale*2),im.height*math.ceil(scale*2)),Image.Resampling.NEAREST)
    c.drawImage(ImageReader(im),x+(w-dw)/2,y+(h-dh)/2,dw,dh,mask='auto')

def main():
    OUT.mkdir(parents=True,exist_ok=True);(OUT/'mockups').mkdir(exist_ok=True);TMP.mkdir(parents=True,exist_ok=True);PDF.parent.mkdir(parents=True,exist_ok=True)
    inv=json.loads((ROOT/'output/remaining-ui-audit/inventory.json').read_text())
    entries={r['entry']:r for r in inv['entries'] if r['entry'] in PLAN['selected_entries']}
    grouped=defaultdict(list)
    for r in ROWS:
        fam=r['family'].replace(' (Fabric/Quilt)','').replace('compat','Compat')
        grouped[fam].append(r)
    families=sorted(grouped,key=lambda f:min(r['entry'] for r in grouped[f]))
    cases=[r for f in families for r in grouped[f]]
    index_pages=math.ceil(len(families)/22)
    start_page=3+index_pages
    page_map={};n=start_page
    for f in families:page_map[f]=n;n+=len(grouped[f])
    c=canvas.Canvas(str(PDF),pagesize=(PW,PH));c.setTitle('Project Everything - 56 Mod Families - Native UIs and Proposed SIU Windows');c.setAuthor('Salts Inventory Update / Design Review')
    page=1
    c.setFillColor(HexColor(NAVY));c.rect(0,0,PW,PH,fill=1,stroke=0)
    c.setFillColor(HexColor('#69d1cf'));c.setFont('Helvetica-Bold',12);c.drawString(42,PH-48,'SALTS INVENTORY UPDATE / PROJECT EVERYTHING')
    c.setFillColor(HexColor('#ffffff'));c.setFont('Helvetica-Bold',36);c.drawString(42,PH-111,'Native UI to SIU window');c.setFont('Helvetica',21);c.drawString(42,PH-146,'56 mod families - visual design atlas')
    paragraph(c,'Actual in-game native references on the left. Proposed SIU layouts on the right. Every captured container variant is included; shared grid designs still receive their own comparison.',42,PH-195,690,13,'#d2e1e3',19)
    for x,value,label in [(42,'56','MOD FAMILIES'),(282,str(len(cases)),'NATIVE REFERENCE VIEWS'),(564,'2','MINECRAFT REFERENCE VERSIONS')]:
        c.setFillColor(HexColor('#69d1cf'));c.setFont('Helvetica-Bold',32);c.drawString(x,210,value);c.setFont('Helvetica-Bold',10);c.drawString(x,188,label)
    paragraph(c,'Design proposals, not screenshots of implemented compatibility. References use Minecraft 1.20.1, plus a 1.21.1 NeoForge Ecologics view. Actual releases and loaders are named on each page. This is not an all-version runtime validation.',42,132,730,11,'#d2e1e3',16)
    c.setFont('Helvetica',9);c.setFillColor(HexColor('#d2e1e3'));c.drawString(42,45,'28 October 2026 | Prepared from retained native gameplay captures and focused native follow-up checks')
    c.showPage();page+=1
    heading(c,'Design rules','What changes, and what must stay intact')
    rules=[('Compact geometry','Use the actual owned UI bounds. Remove the duplicated normal player-inventory band and unnecessary right/bottom whitespace. Narrow windows use the existing compact overflow and close controls.'),('Native identity','Keep the complete artwork, slot frames, diagrams, live model viewports and status indicators. Preserve intentional parchment, machine and book colors instead of placing a mismatched rectangle on gray.'),('Controls stay usable','Keep recipe-book toggles inside the main frame. Open recipe panels attach outside. Search fields, paging, settings rails, ghost filters and selection actions remain part of the design.'),('Tabs join the frame','Selected tabs connect seamlessly; inactive tabs overlap behind the rounded frame with no gap. Tabs start flush at the left edge and must not clip native artwork.'),('Scope and evidence','This atlas covers the inventory/container workflows represented by the selected audited releases. Standalone configuration, guidebook and sign-like editors are accounted for in the appendix. Native screenshots remain unchanged; only the right-hand compositions are mockups.'),('Dynamic states','Empty screenshots do not remove later controls. Production work must preserve progress animations, model rotation, valid/invalid states, optional rails, filled slots, tooltips and native server actions. A static PDF cannot validate those behaviors.')]
    y=PH-132
    for title,body in rules:
        c.setFont('Helvetica-Bold',11);c.setFillColor(HexColor(TEAL));c.drawString(36,y,title);y=paragraph(c,body,190,y,612,10,leading=13)-21
    footer(c,page);c.showPage();page+=1
    for start in range(0,len(families),22):
        heading(c,'Contents','Click a family name to jump to its proposed windows')
        y=PH-126
        for f in families[start:start+22]:
            c.setFont('Helvetica',10);c.setFillColor(HexColor(NAVY));c.drawString(36,y,f);c.drawRightString(802,y,str(page_map[f]));c.linkAbsolute(f,'family-'+str(min(r['entry'] for r in grouped[f])),Rect=(32,y-3,810,y+11),thickness=0);y-=18
        footer(c,page);c.showPage();page+=1
    manifest=[]
    for f in families:
        bookmark='family-'+str(min(r['entry'] for r in grouped[f]));c.bookmarkPage(bookmark);c.addOutlineEntry(f,bookmark,0,False)
        for r in grouped[f]:
            assert hashlib.sha256((ROOT/r['native']['capture']).read_bytes()).hexdigest()==r['native']['sha256']
            proposal,q=propose(r);proposal.save(OUT/'mockups'/(r['id']+'-proposed.png'))
            original=native_display(r);heading(c,f,title_for(r))
            release=reference_release(r,entries[r['entry']])
            c.setFillColor(HexColor(MUTED));c.setFont('Helvetica',9);c.drawString(32,PH-108,f"Reference: Minecraft {r['minecraft']} / {r['loader'].title()} / mod {release['version']}")
            for x,label in [(32,'NATIVE IN GAME / SIU ABSENT'),(437,'PROPOSED SIU WINDOW / MOCKUP')]:
                c.setFillColor(HexColor(TEAL));c.setFont('Helvetica-Bold',9);c.drawString(x,PH-135,label);c.setFillColor(HexColor(PAPER));c.roundRect(x,111,373,337,8,fill=1,stroke=0)
            # Exact native evidence on the left, separately composed proposal on right.
            draw_image(c,original,(40,119,357,321),max_scale=1.6)
            draw_image(c,proposal,(445,119,357,321),max_scale=2.1/proposal.info.get('units',1))
            paragraph(c,q['note'],34,91,774,10,leading=13)
            c.setFont('Helvetica',7);c.setFillColor(HexColor(MUTED));c.drawString(34,43,'Reference ID: '+r['id']+' | Raw SHA-256: '+r['native']['sha256'][:16]+'...')
            footer(c,page);manifest.append({'page':page,'id':r['id'],'family':f,'native_capture':r['native']['capture'],'native_sha256':r['native']['sha256'],'reference_release':release['version'],'reference_original_jar':release.get('original_path'),'recipe':q,'proposed_size':list(proposal.size),'status':'design mockup; not implemented'});c.showPage();page+=1
    # Complete family-by-family scope/accounting, including non-container screens.
    heading(c,'Appendix','Scope, provenance and dynamic-state limits')
    y=PH-132
    notes=[
        'All 56 requested mod families appear in the contents and comparison pages. The original audit has separate Guard Villagers ports and two Naturalist releases; those are grouped by family but their individual native views are retained.',
        'Native references are actual retained in-game captures with Core, Compatibility and Controlify absent. Original PNG hashes are checked on every build. Most fixtures are Minecraft 1.20.1 Fabric or Forge; this atlas does not claim every release/loader was newly launched. Forge uses a named development launcher with the retained SRG-to-named remapping lineage.',
        'The proposals compose native artwork/controls in SIU chrome; they are not screenshots of a running adaptation. Intentional redraws (notably Waystones on an opaque panel) are design illustrations. Proposed labels and slot positions require implementation validation.',
        'Storage skins that use the same native menu are represented by the captured variant. Optional content depends on installed mods, recipes, equipment, configuration and game state. An empty native state is not proof that additional states never appear.',
        'Focused follow-up: recipe panel opened with its button; Gallery painting selected; Rocket Sensor and Polymer browser captured without hover tooltips; Easy Magic shown with an item to reveal its reroll controls and costs. Building Wands tool-menu visual uses the original mod key-request packet, not a constructed screen; its raw-key opening route remains unvalidated.',
        'The dark Ad Astra addon palette is present in its original native texture assets and is intentionally retained. Hover tooltips are transient, not permanent artwork. Static mockups cannot prove item transfer, packet behavior, animation, focus handling or model rendering at all rotations.'
    ]
    for text in notes:y=paragraph(c,text,36,y,768,11,leading=16)-18
    footer(c,page,'Design review scope | No production code or JAR changes');c.showPage();page+=1
    appendix_rows=[]
    for f in families:
        es=[entries[i] for i in sorted({r['entry'] for r in grouped[f]})]
        standalone=sorted({s.rsplit('/',1)[-1] for e in es for s in e.get('standalone_screens',[])})
        note='Inventory views: '+str(len(grouped[f]))+'. '
        if standalone:note+='Native standalone surfaces retained: '+', '.join(standalone)+'.'
        else:note+='No additional standalone screen class was identified in the retained inventory; shared inherited UI is represented by the captured workflow.'
        if any(e['entry']==613 for e in es):note+=' WaystoneSettingsScreen is a name/global-checkbox configuration dialog and stays native.'
        if any(e['entry']==101 for e in es):note+=' Tool-menu visual coverage and raw key-route limits are called out separately.'
        if any(e['entry']==11 for e in es):note+=' Older HerbalBrews package-name variants were inventoried but not all release variants were independently captured.'
        appendix_rows.append((f,note,es[0]['source_url']))
    y=0
    for f,note,url in appendix_rows:
        height=36+len(wrap(c,note,768,size=10))*13
        if y-height<55:
            if y:footer(c,page,'Appendix | Standalone screens stay native');c.showPage();page+=1
            heading(c,'Appendix','Per-family inventory and standalone UI accounting');y=PH-126
        c.setFont('Helvetica-Bold',11);c.setFillColor(HexColor(TEAL));c.drawString(36,y,f)
        if url:c.linkURL(url,(36,y-2,800,y+12),relative=0)
        y=paragraph(c,note,36,y-18,768,10,leading=13)-20
    if y:footer(c,page,'Appendix | Standalone screens stay native');c.showPage();page+=1
    c.save();(OUT/'design-manifest.json').write_text(json.dumps({'families':len(families),'views':len(cases),'pages':page-1,'pdf':str(PDF.relative_to(ROOT)),'cases':manifest},indent=2))
    print(json.dumps({'pdf':str(PDF),'families':len(families),'native_views':len(cases),'pages':page-1,'explicit_designs':len(RECIPES)},indent=2))

if __name__=='__main__':main()
