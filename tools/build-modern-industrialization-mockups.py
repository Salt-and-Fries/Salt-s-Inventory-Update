"""Design-only MI atlas: immutable native captures beside SIU window proposals.

No compatibility/runtime/API code is changed. Includes all 110 pinned 1.20.1
views, rather than mistaking four shared Screen classes for four layouts.
"""
from pathlib import Path
from io import BytesIO
import hashlib,json,math,zipfile
from collections import Counter
from PIL import Image,ImageDraw
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/modern-industrialization-design'
TMP=ROOT/'tmp/pdfs/modern-industrialization-design'
PDF=ROOT/'output/pdf/siu-modern-industrialization-window-mockups.pdf'
GUI=ROOT/'artwork/shared-resources/core/assets/salts_inventory_update/textures/gui'
NEAR=Image.Resampling.NEAREST;BG=(198,198,198,255)
FRAME=Image.open(GUI/'window.png').convert('RGBA')
CONTROLS=Image.open(GUI/'window_controls.png').convert('RGBA')
with zipfile.ZipFile(Path.home()/'.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar') as z:
    ASCII=Image.open(BytesIO(z.read('assets/minecraft/textures/font/ascii.png'))).convert('RGBA')
W,H=1191,842
INK='#19373D';TEAL='#247C80';MUTED='#5E7076';PALE='#EEF4F4';LINE='#D7E2E3'
BUTTON_LIBRARY=[]

def digest(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()

def glyph(ch):
    n=ord(ch) if ord(ch)<128 else ord('?');im=ASCII.crop((n%16*8,n//16*8,n%16*8+8,n//16*8+8))
    box=im.getbbox();return im,3 if ch==' ' else box[2] if box else 5

def tw(s):return sum(glyph(ch)[1]+1 for ch in s)

def text(im,s,x,y,color=(64,64,64,255)):
    assert 0<=x and 0<=y and x+tw(s)<=im.width,(s,x,im.size)
    for ch in s:
        g,w=glyph(ch);ink=Image.new('RGBA',(8,8),color);ink.putalpha(g.getchannel('A'))
        im.alpha_composite(ink,(int(x),int(y)));x+=w+1

def frame(w,h):
    im=Image.new('RGBA',(w,h))
    for s,d in [((0,0,5,5),(0,0,5,5)),((6,0,11,5),(w-5,0,w,5)),
        ((0,6,5,11),(0,h-5,5,h)),((6,6,11,11),(w-5,h-5,w,h)),
        ((5,0,6,5),(5,0,w-5,5)),((5,6,6,11),(5,h-5,w-5,h)),
        ((0,5,5,6),(0,5,5,h-5)),((6,5,11,6),(w-5,5,w,h-5)),((5,5,6,6),(5,5,w-5,h-5))]:
        im.alpha_composite(FRAME.crop(s).resize((d[2]-d[0],d[3]-d[1]),NEAR),d[:2])
    return im

def header(im,title):
    # Preserve full names. Long names use an ellipsis menu and Close; the
    # native machine controls belong in their own row, never the SIU header.
    cols=[0,6,4,9,2] if tw(title)+90<=im.width else [3,2]
    text(im,title,7,7)
    start=im.width-5-len(cols)*14
    assert 7+tw(title)+7<=start,(title,im.size,start)
    for i,col in enumerate(cols):
        sy=22 if col==0 else 0
        im.alpha_composite(CONTROLS.crop((col*11,sy,col*11+11,sy+11)),(start+i*14,5))
    return ['focus','pin','lock','link','close'] if len(cols)==5 else ['more','close']

def safe_paste(dst,src,at):
    x,y=at;assert x>=0 and y>=0 and x+src.width<=dst.width and y+src.height<=dst.height,(dst.size,src.size,at)
    dst.alpha_composite(src,at)

def source_rows():
    scope=json.loads((ROOT/'output/project-everything-remaining/window-scope-1.20.1.json').read_text())
    expected=next(r for r in scope['mods'] if r['entry']==389)
    rows={}
    for filename in ['pr-mi-pairs.json','pr-sweep-pairs.json','own-pr-extra-pairs.json']:
        for row in json.loads((ROOT/'output/remaining-ui-audit'/filename).read_text())['cases']:
            if row.get('entry')==389:
                assert row['id'] not in rows
                rows[row['id']]={**row,'reference_manifest':filename}
    assert set(rows)==set(expected['captured_case_ids']) and len(rows)==110
    entry=next(e for e in json.loads((ROOT/'output/remaining-ui-audit/inventory.json').read_text())['entries'] if e['entry']==389)
    return [rows[k] for k in expected['captured_case_ids']],entry

def local_capture(row):
    n=row['native'];path=ROOT/Path(n['capture']);assert digest(path)==n['sha256'],path
    source=Image.open(path).convert('RGBA');b=n['bounds']
    # All pinned captures use GUI scale 2; also verify the actual source size.
    assert source.size==(1280,900),(path,source.size)
    def crop(rect):
        box=tuple(2*v for v in (b['x']+rect[0],b['y']+rect[1],b['x']+rect[2],b['y']+rect[3]))
        assert box[0]>=0 and box[1]>=0 and box[2]<=source.width and box[3]<=source.height
        return source.crop(box).resize((rect[2]-rect[0],rect[3]-rect[1]),NEAR)
    return crop

def tab(crop,side,content_bottom):
    # MI's original side trays start at y=10, extend 32px, and may contain
    # several buttons. Detect their real lower edge rather than clipping them
    # to the first icon or importing a rectangle of the game world.
    x0,x1=(176,208) if side=='right' else (-32,0)
    sample=crop((x0,0,x1,content_bottom))
    ys=[]
    for y in range(content_bottom):
        pixels=[sample.getpixel((x,y)) for x in range(32)]
        if sum(1 for p in pixels if min(p[:3])>150 and max(p[:3])-min(p[:3])<3)>=8:ys.append(y)
    if not ys:return None
    y0=max(0,min(ys)-1);y1=min(content_bottom,max(ys)+2)
    # Native trays in the retained cohort have the same upper attachment.
    assert y0==10,(side,y0,y1)
    im=crop((x0,y0,x1,y1))
    # Only the outer chamfer is transparent. Keep every native border pixel.
    outside=31 if side=='right' else 0;adjacent=30 if side=='right' else 1
    for x,y in [(outside,0),(adjacent,0),(outside,1),(outside,im.height-1),(adjacent,im.height-1),(outside,im.height-2)]:
        im.putpixel((x,y),(0,0,0,0))
    return {'side':side,'source_rect':[x0,y0,x1,y1],'image':im}

def clean_native_actions(rows):
    seen=set()
    for row in rows:
        n=row['native']
        if not n['screen'].endswith('.MachineScreen'):continue
        crop=local_capture(row)
        for x in range(n['bounds']['width']-24,3,-22):
            if max(crop((x,4,x+1,5)).getpixel((0,0))[:3])>=60:continue
            if 6+tw(n['title'])>=x:continue
            if any(not s['player'] and s['x']-1<x+20 and s['x']+17>x and s['y']-1<24 for s in n['slots']):continue
            im=crop((x,4,x+20,24))
            if im.tobytes() not in seen:
                seen.add(im.tobytes());BUTTON_LIBRARY.append({'image':im,'case':row['id'],'rect':[x,4,x+20,24]})
    assert BUTTON_LIBRARY

def clean_action(im,x,row):
    n=row['native']
    if 6+tw(n['title'])<x:return im,None
    # Native long titles are actually painted over some original buttons.
    # Match every unaffected pixel against clean captures of the same native
    # icon/state. Never import those old text fragments into the new toolbar.
    reliable=[]
    for yy in range(20):
        for xx in range(20):
            if 2<=yy<=9:continue
            if any(not s['player'] and s['x']-1<=x+xx<s['x']+17 and s['y']-1<=4+yy<s['y']+17 for s in n['slots']):continue
            reliable.append((xx,yy))
    matches=[a for a in BUTTON_LIBRARY if all(a['image'].getpixel(p)==im.getpixel(p) for p in reliable)]
    distinct={a['image'].tobytes() for a in matches}
    assert len(distinct)==1,('ambiguous native button repair',row['id'],x,len(distinct))
    return matches[0]['image'],{'case':matches[0]['case'],'source_rect':matches[0]['rect']}

def design(row):
    n=row['native'];title=n['title'];crop=local_capture(row);width=n['bounds']['width']
    players=[s for s in n['slots'] if s['player']];assert players
    # Vanilla/MI label lies 13px above the first player slot. Stop on the
    # preceding clear row, not on the player slot and not on a device bezel.
    label_y=min(s['y'] for s in players)-12
    bottom=max(label_y-1,max((s['y']+18 for s in n['slots'] if not s['player'] and 4<=s['x']<width-20),default=0))
    machine=n['screen'].endswith('.MachineScreen')
    body_rect=(4,14,width-4,bottom)
    body=crop(body_rect)
    if bottom>label_y:
        ImageDraw.Draw(body).rectangle((2,label_y-14,2+tw('Inventory'),bottom-14),fill=BG)
    actions=[];repaired_cells={}
    if machine:
        for x in range(width-24,3,-22):
            p=crop((x,4,x+1,5)).getpixel((0,0))
            if max(p[:3])<60:
                art,repair=clean_action(crop((x,4,x+20,24)),x,row)
                actions.append({'source_rect':[x,4,x+20,24],'image':art,'clean_art_source':repair})
        actions.reverse()
        # Remove only the lower tail of each button from the working area;
        # its complete 20x20 native sprite is relocated to the utility row.
        d=ImageDraw.Draw(body)
        for action in actions:
            x=action['source_rect'][0]-4
            d.rectangle((x,0,x+19,9),fill=BG)
        # Some original hatches put a header button over the corner of their
        # first slot row. Relocating that control must also restore the hidden
        # native cell artwork, rather than reproducing the original collision.
        cells=[s for s in n['slots'] if not s['player'] and 4<=s['x']<width-20]
        def overlaps(s):
            return any(s['x']-1<a['source_rect'][2] and s['x']+17>a['source_rect'][0]
                and s['y']-1<a['source_rect'][3] and s['y']+17>a['source_rect'][1] for a in actions)
        for s in cells:
            if not overlaps(s):continue
            other=next((t for t in cells if t['y']==s['y'] and not overlaps(t)),None)
            assert other and s['item']=='minecraft:air' and other['item']=='minecraft:air',('missing native repair guide',row['id'],s)
            tile=crop((other['x']-1,other['y']-1,other['x']+17,other['y']+17))
            safe_paste(body,tile,(s['x']-5,s['y']-15));repaired_cells[s['index']]=tile.crop((1,1,17,17))
    warning=None
    if n['screen'].endswith('.ItemPipeScreen'):
        warning=crop((55,5,146,14))
    # Widen the chrome only when necessary to keep a full machine name.
    w=max(180,tw(title)+47)
    y=24;rows=[]
    if warning:
        rows.append(('warning',warning,8,y));y+=15
    if actions:
        for i,a in enumerate(actions):rows.append(('action',a['image'],w-8-len(actions)*22+i*22,y))
        y+=23
    body_at=((w-body.width)//2,y)
    result=frame(w,y+body.height+7);chrome=header(result,title)
    for _,im,x,yy in rows:safe_paste(result,im,(x,yy))
    safe_paste(result,body,body_at)
    tabs=[]
    if machine:
        for side in ['left','right']:
            pane=tab(crop,side,n['bounds']['height'])
            if pane:tabs.append(pane)
    # Attached trays stay parent-owned and follow the same complete native art.
    left=32 if any(t['side']=='left' for t in tabs) else 0
    right=32 if any(t['side']=='right' for t in tabs) else 0
    tray_y=24
    h=max(result.height,max((tray_y+t['image'].height for t in tabs),default=0))+2
    composed=Image.new('RGBA',(left+result.width+right+4,h+2))
    for pane in tabs:
        x=2 if pane['side']=='left' else left+result.width+1
        safe_paste(composed,pane['image'],(x,tray_y+2))
    safe_paste(composed,result,(left+2,2))
    native_left=-32 if left else 0;native_right=width+32 if right else width
    native_rect=(native_left-1,-1,native_right+1,n['bounds']['height']+2)
    native=crop(native_rect)
    key=row['id'];np=OUT/'original'/f'{key}.png';mp=OUT/'mockups'/f'{key}.png'
    native.resize((native.width*4,native.height*4),NEAR).save(np)
    composed.resize((composed.width*4,composed.height*4),NEAR).save(mp)
    # Geometric, pixel and provenance checks concern the design composition,
    # not implementation or gameplay. Each real device cell retains its art.
    for s in n['slots']:
        if s['player']:continue
        x,y0=s['x'],s['y']
        a=repaired_cells.get(s['index'],crop((x,y0,x+16,y0+16)))
        if x>=4 and x+16<=width-4 and y0>=14 and y0+16<=bottom:
            dx=left+2+body_at[0]+x-4;dy=2+body_at[1]+y0-14
        else:
            pane=next((t for t in tabs if x>=t['source_rect'][0] and x+16<=t['source_rect'][2]
                and y0>=t['source_rect'][1] and y0+16<=t['source_rect'][3]),None)
            assert pane is not None,('device slot outside full working crop or attachment',key,s,body_rect)
            px=2 if pane['side']=='left' else left+result.width+1
            dx=px+x-pane['source_rect'][0];dy=tray_y+2+y0-pane['source_rect'][1]
        assert a.tobytes()==composed.crop((dx,dy,dx+16,dy+16)).tobytes(),(key,s)
    row={
        'id':key,'title':title,'screen':n['screen'],'release':'1.8.6','minecraft':'1.20.1','loader':'fabric',
        'native':str(np.relative_to(ROOT)),'mockup':str(mp.relative_to(ROOT)),
        'native_sha256':digest(np),'mockup_sha256':digest(mp),
        'source_capture':n['capture'],'source_sha256':n['sha256'],
        'source_manifest':row['reference_manifest'],'native_crop':list(native_rect),'working_crop':list(body_rect),
        'native_actions':[a['source_rect'] for a in actions],'chrome':chrome,
        'logical_size':list(composed.size),'device_cells':sum(not s['player'] for s in n['slots']),
        'attachments':[{'side':t['side'],'source_rect':t['source_rect']} for t in tabs],
        'warning_relocated':warning is not None,'status':'theoretical SIU window; not an in-game implementation'}
    row['original_header_slot_collisions_repaired']=list(repaired_cells)
    row['clean_native_button_sources']=[a['clean_art_source'] for a in actions if a['clean_art_source']]
    return row

def contact_sheets(rows):
    from PIL import ImageFont
    font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',17)
    for index in range(0,len(rows),12):
        group=rows[index:index+12];sheet=Image.new('RGB',(1600,1060),'#eef4f4');d=ImageDraw.Draw(sheet)
        for i,row in enumerate(group):
            x=(i%4)*400;y=(i//4)*350;d.text((x+8,y+8),row['title'][:42],font=font,fill='#19373d')
            for j,key in enumerate(['native','mockup']):
                im=Image.open(ROOT/row[key]).convert('RGBA');scale=min(188/im.width,290/im.height)
                im=im.resize((round(im.width*scale),round(im.height*scale)),NEAR)
                sheet.paste(im,(x+j*200+(200-im.width)//2,y+40+(300-im.height)//2),im)
        sheet.save(TMP/f'contact-{index//12+1:02d}.png')

def make_pdf(rows):
    pdfmetrics.registerFont(TTFont('Segoe','C:/Windows/Fonts/segoeui.ttf'))
    pdfmetrics.registerFont(TTFont('SegoeB','C:/Windows/Fonts/segoeuib.ttf'))
    c=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1)
    c.setTitle('Modern Industrialization | Complete SIU Window Design Review | 1.20.1')
    c.setAuthor("Salt's Inventory Update / Project Everything")
    def txt(s,x,y,size=10,bold=False,color=INK):
        c.setFillColor(HexColor(color));c.setFont('SegoeB' if bold else 'Segoe',size);c.drawString(x,y,s)
    def para(s,x,y,w,size=11,leading=16):
        line=''
        for word in s.split():
            test=(line+' '+word).strip()
            if pdfmetrics.stringWidth(test,'Segoe',size)>w:
                txt(line,x,y,size,color=MUTED);y-=leading;line=word
            else:line=test
        if line:txt(line,x,y,size,color=MUTED);y-=leading
        return y
    def begin(title,sub):
        c.setFillColor(HexColor('#FFFFFF'));c.rect(0,0,W,H,fill=1,stroke=0)
        txt('PROJECT EVERYTHING / MODERN INDUSTRIALIZATION',32,H-31,9,True,TEAL)
        txt(title,32,H-67,26,True);txt(sub,32,H-89,11,color=MUTED)
    def end(note):
        c.setStrokeColor(HexColor(LINE));c.line(32,33,W-32,33)
        txt(note,32,19,8,color=MUTED);txt(str(c.getPageNumber()),W-48,19,8,color=MUTED);c.showPage()
    def image(path,x,y,w,h):
        im=Image.open(ROOT/path).convert('RGBA');scale=min(w/im.width,h/im.height)
        dw,dh=im.width*scale,im.height*scale
        c.drawImage(ImageReader(im),x+(w-dw)/2,y+(h-dh)/2,dw,dh,mask='auto')
    byid={r['id']:r for r in rows}
    def detail(row,y,label,note):
        c.bookmarkPage(row['id']);c.addOutlineEntry(row['title'],row['id'],level=1)
        row['pdf_page']=c.getPageNumber()
        txt(label,32,y,16,True);txt('ORIGINAL IN GAME / SIU ABSENT',32,y-22,9,True,TEAL)
        txt('PROPOSED SIU WINDOW / MOCKUP',610,y-22,9,True,TEAL)
        for x in [32,610]:
            c.setFillColor(HexColor(PALE));c.roundRect(x,y-257,549,222,7,fill=1,stroke=0)
        image(row['native'],43,y-250,527,208);image(row['mockup'],621,y-250,527,208)
        assert para(note,32,y-277,W-64,11,15)>35

    begin('Machines and the Forge Hammer','4 shared screen types / 110 captured views / Minecraft 1.20.1 Fabric / MI 1.8.6 / design only')
    c.bookmarkPage('machines-hammer');c.addOutlineEntry('Machines and Forge Hammer','machines-hammer',level=0)
    detail(byid['modern-industrialization-electric-macerator'],H-127,'01 / Electric Macerator - the shared machine-window design',
        'Match MI gray exactly. Keep its real item/fluid slot frames, progress, power and side trays. Put native lock/redstone controls below the SIU title. Long names use More + Close; player storage lives in the separate SIU Inventory window.')
    detail(byid['modern-industrialization-forge-hammer'],H-459,'02 / Forge Hammer - retain the entire strike surface',
        'Keep the native hammer guide, input, complete central work surface and large output rim. Only the player inventory is removed. The gray working body blends into SIU chrome, with no pasted outer border or cropped panel edges.')
    end('Real original screenshots + authored mockups. No compatibility, Core or API code changed; no gameplay tests run.')

    begin('Pipe controls remain complete','Two inventory-adjacent configuration screens / original button order, labels and filter behavior')
    c.bookmarkPage('pipe-configurations');c.addOutlineEntry('Pipe configurations','pipe-configurations',level=0)
    detail(byid['own-mi-connected-item-pipe'],H-127,'03 / Item Pipe - filters, mode, priority and stack controls',
        'Keep all native filter/upgrade cells, IN mode, warning control and both numeric button rows. Move the full Empty Filter warning below the window title so it cannot collide with SIU controls. Filters remain native configuration, not extra storage.')
    detail(byid['own-mi-connected-fluid-pipe'],H-459,'04 / Fluid Pipe - keep the fluid selector and priority row',
        'Retain the native fluid ghost/selection display, IN mode and all four adjustment buttons with their value. The following compact atlas includes every one of the 110 captured views. Formed multiblocks, filled tanks and recipe states need live validation during implementation.')
    end('Scope: the pinned 1.20.1 backlog. Compact variants follow; zoom to inspect full-resolution pixel art.')

    featured={'modern-industrialization-electric-macerator','modern-industrialization-forge-hammer',
        'own-mi-connected-item-pipe','own-mi-connected-fluid-pipe'}
    gallery=[r for r in rows if r['id'] not in featured]
    assert len(gallery)==106
    for start in range(0,len(gallery),12):
        batch=gallery[start:start+12]
        begin(f'All remaining views / {start+5:03d}-{start+4+len(batch):03d} of 110',
            'Four detailed comparisons on pages 1-2; every other captured view follows. Original left / theoretical SIU window right.')
        key=f'gallery-{start}';c.bookmarkPage(key);c.addOutlineEntry(f'Views {start+5}-{start+4+len(batch)}',key,level=0)
        card_w=(W-64-3*12)/4;card_h=217;gap=12
        for i,row in enumerate(batch):
            x=32+(i%4)*(card_w+gap);top=H-113-(i//4)*(card_h+12);bottom=top-card_h
            c.setFillColor(HexColor(PALE));c.roundRect(x,bottom,card_w,card_h,5,fill=1,stroke=0)
            c.bookmarkPage(row['id']);c.addOutlineEntry(row['title'],row['id'],level=1)
            row['pdf_page']=c.getPageNumber()
            title=f'{start+i+5:03d}  '+row['title']
            yy=para(title,x+8,top-17,card_w-16,9,11)
            txt('ORIGINAL',x+8,top-45,7,True,TEAL);txt('SIU MOCKUP',x+card_w/2+3,top-45,7,True,TEAL)
            bw=(card_w-20)/2
            image(row['native'],x+5,bottom+8,bw,card_h-59)
            image(row['mockup'],x+card_w/2+3,bottom+8,bw,card_h-59)
        end('110 hash-checked native views; all have a mockup. Native multiblock errors are captured fixture states, not proposed missing features.')
    c.save()

def main():
    for folder in [OUT,OUT/'original',OUT/'mockups',TMP,PDF.parent]:folder.mkdir(parents=True,exist_ok=True)
    source,entry=source_rows();clean_native_actions(source);rows=[design(r) for r in source]
    assert len(rows)==110 and len(set(r['screen'] for r in rows))==4
    manifest={'status':'design proposals only; not implemented','minecraft':'1.20.1','loader':'fabric','release':'1.8.6',
        'native_views':len(rows),'screen_types':4,'source_release':entry['releases'][0],
        'design':'MI native working body and real controls inside matching SIU gray chrome; separate player inventory; full original slot art.',
        'cases':rows,'coverage_limits':'Saved real captures, predominantly empty/idle, include unformed multiblocks. No new game launches or functional tests. Later releases/loaders and extra dynamic states are outside this design atlas.'}
    contact_sheets(rows);make_pdf(rows)
    (OUT/'mockup-manifest.json').write_text(json.dumps(manifest,indent=2))
    print(json.dumps({'pdf':str(PDF),'views':110,'screen_types':4,'pages':11,
        'native_action_counts':dict(Counter(len(r['native_actions']) for r in rows)),
        'attachments':sum(len(r['attachments']) for r in rows),'slot_art_checks':sum(r['device_cells'] for r in rows)},indent=2))

if __name__=='__main__':main()
