"""All implemented MI windows: paired actual captures, never authored mockups."""
from pathlib import Path
import hashlib,json,math
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/modern-industrialization-implementation'
TMP=ROOT/'tmp/pdfs/modern-industrialization-implemented'
PDF=ROOT/'output/pdf/siu-modern-industrialization-all-windows.pdf'
W,H=1191,842
INK='#19373D';TEAL='#247C80';MUTED='#5E7076';PALE='#EEF4F4';LINE='#D7E2E3'
sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()
manifest=[]

def load(p):
    data=json.loads(p.read_text());return data.get('client',data)

def crop_capture(key,mc,state_path,capture_path):
    state=load(state_path)
    ws=state.get('windows',state.get('allWindows',[]))
    assert len(ws)==1,(key,state_path,len(ws))
    window=ws[0];assert not window.get('layoutError'),state_path
    assert window['definition'].endswith('.NativeTemplateWindow')
    b=window['bounds'];left=b['x'];top=b['y'];right=left+b['width'];bottom=top+b['height']
    for region in window['template']['regions']:
        if not region['exterior']:continue
        x=b['x']+region['x'];y=b['y']+region['y'];s=region['source']
        left=min(left,x);top=min(top,y);right=max(right,x+s['width']);bottom=max(bottom,y+s['height'])
    # Keep an overlapping player window whole, rather than cropping a stray
    # strip of its header underneath a moved machine or an attached tray.
    context=[]
    for workspace in state.get('workspaceWindows',[]):
        if workspace['kind']=='CONTAINER' or workspace.get('persistentHidden')=='true':continue
        q=workspace['bounds'];x=q['x'];y=q['y'];r=x+q['width'];d=y+q['height']
        if x<right+2 and r>left-2 and y<bottom+2 and d>top-2:
            left=min(left,x);top=min(top,y);right=max(right,r);bottom=max(bottom,d);context.append(workspace['kind'])
    source=Image.open(capture_path).convert('RGB')
    scale=round(source.width/state['guiWidth'])
    assert scale in [2,3] and (state['guiWidth']-1)*scale<source.width<=state['guiWidth']*scale
    assert (state['guiHeight']-1)*scale<source.height<=state['guiHeight']*scale
    # Keep all native exterior trays, frame pixels and two GUI pixels of context.
    rect=(max(0,(left-2)*scale),max(0,(top-2)*scale),min(source.width,(right+2)*scale),min(source.height,(bottom+2)*scale))
    assert rect[0]<=left*scale and rect[1]<=top*scale and rect[2]>=right*scale and rect[3]>=bottom*scale,(key,rect,b)
    cropped=source.crop(rect);p=TMP/'crops'/f'{mc}-{key}.png';cropped.save(p)
    entry=dict(id=key,minecraft=mc,state=str(state_path.relative_to(ROOT)),capture=str(capture_path.relative_to(ROOT)),
        capture_sha256=sha(capture_path),crop=list(rect),gui_scale=scale,image=str(p.relative_to(ROOT)),image_sha256=sha(p),
        menu=window['menuId'],native_screen=window.get('nativeScreen'),frame_bounds=b,context_windows=context,exterior_trays=sum(r['exterior'] for r in window['template']['regions']))
    manifest.append(entry);return entry

def from_case(row,mc):
    if 'observation' in row:
        observation=ROOT/row['observation'];run=observation.parent.name;tag=observation.stem.removesuffix('-client')
        capture=ROOT/'build/remaining-ui-audit/gameplay'/run/'screenshots'/(tag+'.png')
    else:
        capture=ROOT/row['capture'];run=capture.parent.parent.name
        tag=capture.stem.removeprefix('mi-')
        observation=ROOT/'build/modern-industrialization-implementation'/run/(tag+'.json')
    return crop_capture(row['id'],mc,observation,capture)

def extra(mc,key,run,tag,flat=False):
    work=ROOT/'build/modern-industrialization-implementation'
    observation=work/(run+'-'+tag+'.json') if flat else work/run/(tag+'-client.json')
    capture_tag='forge-hammer-recipe-selected' if flat else tag
    capture=ROOT/'build/remaining-ui-audit/gameplay'/run/'screenshots'/(capture_tag+'.png')
    return crop_capture(key,mc,observation,capture)

def main():
    (TMP/'crops').mkdir(parents=True,exist_ok=True);PDF.parent.mkdir(parents=True,exist_ok=True)
    report=json.loads((OUT/'verification.json').read_text())
    titles={r['id']:r['title'] for r in json.loads((ROOT/'output/modern-industrialization-design/mockup-manifest.json').read_text())['cases']}
    targets=report['targets'];assert [t['minecraft'] for t in targets]==['1.20.1','1.21.1']
    ids=[r['id'] for r in targets[0]['cases']];assert len(ids)==110 and set(ids)==set(titles)
    assert all(set(r['id'] for r in t['cases'])==set(ids) for t in targets)
    pictures={}
    for target in targets:
        mc=target['minecraft'];pictures[mc]={r['id']:from_case(r,mc) for r in target['cases']}
    final={'1.20.1':'1.20.1-fabric-siu-mi-refine03','1.21.1':'1.21.1-neoforge-integrated-mi-refine03'}
    actions=final
    forge={'1.20.1':'1.20.1-fabric-siu-mi-windows03','1.21.1':'1.21.1-neoforge-integrated-mi-windows02'}
    extras=[
        ('Forge Hammer / recipe selected','The original recipe board, tool, input and output are visible. Crafting and native tool damage were checked separately.',lambda mc:extra(mc,'state-forge-recipe',forge[mc],'forge-selected',True)),
        ('Machine / items and native locks','Stone remains in the input; the native lock and auto-eject controls remain available below SIU chrome.',lambda mc:extra(mc,'state-machine-items',final[mc],'final-macerator-with-inventory')),
        ('Item Pipe / saved filter','The item filter and native mode/priority settings persist after closing and reopening; phantom entries do not consume the carried stack.',lambda mc:extra(mc,'state-item-filter',actions[mc],'item-pipe-reopened')),
        ('Fluid Pipe / saved water selection','The original fluid selector, connection mode and priority are shown after reopening. Selecting the fluid preserves the water bucket.',lambda mc:extra(mc,'state-fluid-selection',final[mc],'fluid-pipe-reopened')),
        ('Configurable Tank / bucket transfer','Fabric visibly shows water; NeoForge still shows the placeholder in this capture. Server data recorded stored water and an empty returned bucket on both targets.',lambda mc:extra(mc,'state-tank-water',actions[mc],'tank-bucket-transfer')),
        ('Large Tank / expanded shape controls','The full gauge rim is retained. The taller native tray now has a closed inner wall and bottom corner below its compact parent. Changing Width updates the server shape.',lambda mc:extra(mc,'state-shape-expanded',final[mc],'shape-changed')),
        ('Machine / GUI scale 3','Complete cells, native buttons and upgrade trays remain mapped after resizing the GUI. Native actions were rechecked at this scale.',lambda mc:extra(mc,'state-gui-scale-3',final[mc],'macerator-resized-actions')),
        ('Machine / moved window','The machine and its attached tray follow the title-bar drag together. The native layout remains intact.',lambda mc:extra(mc,'state-moved-window',final[mc],'macerator-moved')),
        ('Energy Hatch / compact charged state','A smaller body retains padding below the entire energy icon. The native charged state uses the same retained bounds. Long titles have a full-name hover tooltip.',lambda mc:extra(mc,'state-energy-padding',final[mc],'energy-charged')),
        ('Boiler / complete thermometer','The full thermometer and heat bar now have breathing room below them. The old Inventory label is removed independently, preserving every gauge pixel.',lambda mc:extra(mc,'state-full-thermometer',final[mc],'full-thermometer')),
    ]
    extras=[(title,note,{mc:factory(mc) for mc in pictures}) for title,note,factory in extras]
    pdfmetrics.registerFont(TTFont('Segoe','C:/Windows/Fonts/segoeui.ttf'))
    pdfmetrics.registerFont(TTFont('SegoeB','C:/Windows/Fonts/segoeuib.ttf'))
    c=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1)
    c.setTitle('Modern Industrialization | All Implemented SIU Windows')
    c.setAuthor("Salt's Inventory Update / Project Everything")
    c.setSubject('110 actual window views across two supported targets, plus focused live states. No mockups.')
    def txt(text,x,y,size=10,bold=False,color=INK):
        c.setFillColor(HexColor(color));c.setFont('SegoeB' if bold else 'Segoe',size);c.drawString(x,y,text)
    def para(text,x,y,width,size=10,leading=14,bold=False,color=MUTED):
        font='SegoeB' if bold else 'Segoe';line='';lines=[]
        for word in text.split():
            candidate=(line+' '+word).strip()
            if pdfmetrics.stringWidth(candidate,font,size)>width:
                assert line;lines.append(line);line=word
            else:line=candidate
        if line:lines.append(line)
        for line in lines:txt(line,x,y,size,bold,color);y-=leading
        return y
    def begin(title,sub):
        c.setFillColor(HexColor('#FFFFFF'));c.rect(0,0,W,H,fill=1,stroke=0)
        txt('PROJECT EVERYTHING / MODERN INDUSTRIALIZATION',32,H-31,9,True,TEAL)
        txt(title,32,H-67,26,True);txt(sub,32,H-90,11,color=MUTED)
    total=3+math.ceil(len(ids)/4)+len(extras)//2
    def end(note):
        c.setStrokeColor(HexColor(LINE));c.line(32,35,W-32,35)
        txt(note,32,20,8,color=MUTED);txt(f'{c.getPageNumber()} / {total}',W-69,20,8,color=MUTED);c.showPage()
    def image(entry,x,y,width,height):
        path=ROOT/entry['image'];im=Image.open(path);scale=min(width/im.width,height/im.height)
        dw,dh=im.width*scale,im.height*scale
        assert dw<=width+.01 and dh<=height+.01
        c.drawImage(ImageReader(im),x+(width-dw)/2,y+(height-dh)/2,dw,dh)
        entry['pdf_page']=c.getPageNumber()
    def label_pair(x,top,width,size=8):
        txt('1.20.1 FABRIC / MI 1.8.6',x,top,size,True,TEAL)
        txt('1.21.1 NEOFORGE / MI 2.5.11',x+width/2+7,top,size,True,TEAL)

    begin('All implemented windows','110 captured views / 4 native UI families / both supported version and loader targets / actual game screenshots')
    c.bookmarkPage('overview');c.addOutlineEntry('Overview','overview',level=0)
    label_pair(32,H-127,W-64,10)
    for mc,x in [('1.20.1',32),('1.21.1',610)]:
        loader='fabric' if mc=='1.20.1' else 'neoforge'
        path=OUT/'screenshots'/f'{mc}-{loader}-machine.png'
        im=Image.open(path);c.drawImage(ImageReader(im),x,H-537,549,386)
    y=para('Refined SIU windows retain MI\'s own slots, complete gauges and native controls. Exposed tray joins now have matching inner walls and corners. Sparse panels are more compact, with padding around their sprites; active multiblock and reactor space is preserved.',32,264,W-64,13,19,color=INK)
    y=para('Use the clickable index or PDF bookmarks to find a device. The atlas pairs the same view across the two releases. The final pages show crafting, filters, fluid transfer, expanded shape selection, movement and GUI scaling.',32,y-13,W-64,12,18)
    y=para('Scope: every one of the 110 approved captured views. Most atlas fixtures are empty or idle. "Shape Invalid" is MI\'s native state for an unformed multiblock. These images document UI coverage; a complete gameplay/production-chain suite was not run.',32,y-13,W-64,11,16)
    assert y>45
    end('Actual game captures, not mockups. SIU Compatibility 0.1.3; Core and the public API unchanged by this integration.')

    for start in [0,56]:
        batch=ids[start:start+56]
        begin('Window index',f'Click a title to jump to its paired screenshots / entries {start+1}-{start+len(batch)} of 110')
        key=f'index-{start}';c.bookmarkPage(key);c.addOutlineEntry('Window index '+str(start+1)+'-'+str(start+len(batch)),key,level=0)
        for i,ident in enumerate(batch):
            x=32+(i//28)*578;y=H-132-(i%28)*23
            text=f'{start+i+1:03d}  '+titles[ident]
            assert pdfmetrics.stringWidth(text,'Segoe',10.5)<549,(ident,text)
            txt(text,x,y,10.5,color=INK)
            c.linkAbsolute(text,'case-'+ident,Rect=(x,y-3,x+549,y+13),thickness=0)
        end('Index includes all machine variants, Forge Hammer, Item Pipe and Fluid Pipe. Guidebooks and non-inventory overlays stay native.')

    for start in range(0,len(ids),4):
        batch=ids[start:start+4]
        begin('Implemented window atlas',f'Views {start+1:03d}-{start+len(batch):03d} of 110 / same device on both targets / default or recorded native fixture states')
        key='atlas-'+str(start);c.bookmarkPage(key);c.addOutlineEntry(f'Window atlas {start+1}-{start+len(batch)}',key,level=0)
        cw=(W-64-16)/2;ch=321
        for i,ident in enumerate(batch):
            x=32+(i%2)*(cw+16);top=H-116-(i//2)*336;bottom=top-ch
            c.setFillColor(HexColor(PALE));c.roundRect(x,bottom,cw,ch,6,fill=1,stroke=0)
            anchor='case-'+ident;c.bookmarkPage(anchor,fit='FitH',top=top);c.addOutlineEntry(f'{start+i+1:03d} / '+titles[ident],anchor,level=1)
            yy=para(f'{start+i+1:03d} / '+titles[ident],x+11,top-21,cw-22,11.5,14,True,INK);assert yy>=top-49
            label_pair(x+11,top-51,cw-22,7.2)
            for mc,ix in [('1.20.1',x+9),('1.21.1',x+cw/2+4)]:
                image(pictures[mc][ident],ix,bottom+13,cw/2-17,ch-77)
        end('Entire SIU window and native attached trays retained. Multiblock warnings reflect unformed fixtures; no sprites were reconstructed for this PDF.')

    for start in range(0,len(extras),2):
        begin('Additional live states',f'Focused native UI actions / examples {start+1}-{start+2} of {len(extras)} / actual captures on both targets')
        key='states-'+str(start);c.bookmarkPage(key);c.addOutlineEntry('Live states '+str(start+1)+'-'+str(start+2),key,level=0)
        for i,(title,note,images) in enumerate(extras[start:start+2]):
            top=H-119-i*337
            txt(title,32,top,16,True);c.bookmarkPage('state-'+str(start+i),fit='FitH',top=top);c.addOutlineEntry(title,'state-'+str(start+i),level=1)
            label_pair(32,top-24,W-64,9)
            for mc,x in [('1.20.1',32),('1.21.1',610)]:
                c.setFillColor(HexColor(PALE));c.roundRect(x,top-264,549,224,6,fill=1,stroke=0)
                image(images[mc],x+10,top-256,529,208)
            assert para(note,32,top-285,W-64,10.5,15)>38
        end('Focused UI checks only. Formed multiblock production, external recipe viewers and multiplayer conditions were not exhaustively exercised.')
    c.save()
    assert total==36 and len(manifest)==240
    assert all('pdf_page' in entry for entry in manifest)
    result=dict(pdf=str(PDF.relative_to(ROOT)),sha256=sha(PDF),pages=total,approved_views=110,targets=2,atlas_images=220,additional_state_images=20,
        source_verification=str((OUT/'verification.json').relative_to(ROOT)),source_verification_sha256=sha(OUT/'verification.json'),images=manifest,
        provenance='Only rectangular crops of real game screenshots. All source pixels retained inside each crop; no UI pixels redrawn, repaired or generated.')
    (OUT/'pdf-manifest.json').write_text(json.dumps(result,indent=2))
    print(json.dumps({k:v for k,v in result.items() if k!='images'},indent=2))

if __name__=='__main__':main()
