"""Actual modern publisher UI beside the tested shipping SIU window."""
from pathlib import Path
import hashlib,json,math
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from pypdf import PdfReader

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'output/extended-ae-modern-comparisons'
PDF=ROOT/'output/pdf/siu-extended-ae-modern-comparisons.pdf'
W,H=1200,850
read=lambda p:json.loads(p.read_text())
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
TITLES={
 'pattern-modifier-multiply':'Pattern Modifier / Multiply',
 'pattern-modifier-replace':'Pattern Modifier / Replace',
 'pattern-modifier-properties':'Pattern Modifier / Properties',
 'pattern-modifier-clone':'Pattern Modifier / Clone',
 'config_modifier':'Configuration Modifier','void_cell':'Void Cell Configuration',
 'ex_pattern_provider':'Extended Pattern Provider','ex_interface':'Extended Interface',
 'oversize_interface':'Oversize Interface','wireless_connect':'Wireless Connector',
 'wireless_hub':'Wireless Hub','ingredient_buffer':'Ingredient Buffer',
 'ex_drive':'Extended ME Drive','ex_molecular_assembler':'Extended Molecular Assembler',
 'ex_inscriber':'Extended Inscriber','caner':'ME Caner','ex_io_port':'Extended ME IO Port',
 'circuit_cutter':'Circuit Slicer','crystal_assembler':'Crystal Assembler',
 'ex_export_bus_part':'Extended Export Bus','ex_import_bus_part':'Extended Import Bus',
 'active_formation_plane':'Active Formation Plane','mod_export_bus':'Mod Export Bus',
 'mod_storage_bus':'Mod Storage Bus','precise_export_bus':'Precise Export Bus',
 'precise_storage_bus':'Precise Storage Bus','tag_export_bus':'Tag Export Bus',
 'tag_storage_bus':'Tag Storage Bus','threshold_export_bus':'Threshold Export Bus',
 'threshold_level_emitter':'Threshold Level Emitter','smart_annihilation_plane':'Smart Annihilation Plane',
 'renamer':'Renamer','output-side-selection':'Output-Side Selection',
 'precise-amount-editor':'Precise Amount Entry','wireless-pattern-access':'Wireless Extended Pattern Access Terminal',
 'pattern-access-populated':'Extended Pattern Access Terminal','assembler-matrix-populated':'Assembler Matrix',
 'ex-emc-interface':'Extended Transmutation Interface (EMC)',
 'ex-emc-import-bus-part':'Extended Transmutation Import Bus (EMC)','ex-emc-export-bus-part':'Extended Transmutation Export Bus (EMC)',
}
ORDER=list(TITLES)

def crop(source,state,bounds,widgets,slots,dest,native=False):
    """Crop only; preserve complete main frames, native side tabs and trays."""
    with Image.open(source)as raw:im=raw.convert('RGB')
    scale=im.width/state['guiWidth'];b=bounds;x,y,w,h=[b[k]for k in ['x','y','width','height']]
    rects=[(x-4,y-4,x+w+4,y+h+4)]
    for a in widgets:
        if not a.get('visible',True):continue
        xx,yy,ww,hh=[a.get(k,0)for k in ['x','y','width','height']]
        if ww<=0 or hh<=0 or xx<-100 or yy<-100:continue
        rects.append((xx-5,yy-5,xx+ww+5,yy+hh+5))
    origin=state.get('nativeBounds',b)
    if not native:origin=state['nativeBounds']
    visible={s['index']for s in state.get('slotVisibility',[])if s['visible']}
    for s in slots:
        if s.get('player')or s['x']<-100 or s['y']<-100:continue
        if not native and s['index']not in visible:continue
        xx=origin['x']+s['x'];yy=origin['y']+s['y']
        if xx<x or yy<y or xx+16>x+w or yy+16>y+h:rects.append((xx-8,yy-8,xx+24,yy+24))
    box=(max(0,math.floor(min(r[0]for r in rects)*scale)),max(0,math.floor(min(r[1]for r in rects)*scale)),min(im.width,math.ceil(max(r[2]for r in rects)*scale)),min(im.height,math.ceil(max(r[3]for r in rects)*scale)))
    assert box[2]>box[0]and box[3]>box[1]
    im.crop(box).save(dest);return box

def main():
    for p in [WORK/'original',WORK/'siu',PDF.parent]:p.mkdir(parents=True,exist_ok=True)
    release=read(ROOT/'output/extended-ae-all-targets/targets.json');pages=[]
    for mc in ['1.21.1','26.1.2']:
        originals={r['name']:r for r in read(WORK/f'native-{mc}.json')}
        records={r['name']:r for r in read(ROOT/f'output/extended-ae-all-targets/{mc}/extended-ae-port-13/captures.json')}
        if mc=='1.21.1':records.update({r['name']:r for r in read(ROOT/f'output/extended-ae-all-targets/{mc}/extended-ae-emc-pdf-01/captures.json')})
        ids=[name for name in ORDER if name in records]
        assert len(ids)==(40 if mc=='1.21.1'else 36),(mc,len(ids))
        assert set(ids)<=set(originals),(mc,set(ids)-set(originals))
        for name in ids:
            left=originals[name];right=records[name];screen=next(w for w in right['state']['allWindows']if '.extendedae.'in w.get('nativeScreen',''))
            assert left['state']['screen']==screen['nativeScreen']
            assert not left['state']['siu_loaded']
            lp=Path(left['capture']);rp=Path(right['capture']);assert sha(lp)==left['sha256']
            # The SIU image belongs to the immutable shipping-jar verification.
            if not name.startswith('ex-emc-'):
                verified=next(c for c in read(ROOT/'output/extended-ae-all-targets/verification.json')['captures']if c['minecraft']==mc and c['name']==name)
                assert sha(rp)==verified['sha256']
            lc=WORK/'original'/f'{mc}-{name}.png';rc=WORK/'siu'/f'{mc}-{name}.png'
            lbox=crop(lp,left['state'],left['state']['bounds'],left['state'].get('widgets',[]),left['state'].get('slots',[]),lc,True)
            rstate={**right['state'],**screen}
            rbox=crop(rp,rstate,screen['bounds'],screen.get('nativeWidgets',[]),screen.get('slots',[]),rc)
            pages.append(dict(id=mc+'-'+name,name=name,title=TITLES[name],minecraft=mc,extended_ae=next(t['release']for t in release if t['minecraft']==mc),screen=screen['nativeScreen'],original=str(lc.relative_to(ROOT)),siu=str(rc.relative_to(ROOT)),original_source=str(lp.relative_to(ROOT)),siu_source=str(rp.relative_to(ROOT)),original_sha256=sha(lp),siu_sha256=sha(rp),original_crop=list(lbox),siu_crop=list(rbox)))
    for name,font in [('UI','segoeui.ttf'),('UIB','segoeuib.ttf')]:pdfmetrics.registerFont(TTFont(name,'C:/Windows/Fonts/'+font))
    c=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1);c.setTitle('Extended AE - Modern UI and SIU Window Comparisons');c.setAuthor('Salts Inventory Update')
    def text(s,x,y,size=12,bold=False,color='#253649'):
        font='UIB'if bold else'UI';assert x+pdfmetrics.stringWidth(s,font,size)<=W-28,(s,size)
        c.setFont(font,size);c.setFillColor(HexColor(color));c.drawString(x,y,s)
    def para(s,x,y,width,size=12,leading=18):
        line='';lines=[]
        for word in s.split():
            candidate=(line+' '+word).strip()
            if line and pdfmetrics.stringWidth(candidate,'UI',size)>width:lines.append(line);line=word
            else:line=candidate
        if line:lines.append(line)
        for line in lines:text(line,x,y,size);y-=leading
        return y
    def frame(x,y,w,h):c.setFillColor(HexColor('#EEF1F7'));c.roundRect(x,y,w,h,9,fill=1,stroke=0)
    def picture(path,x,y,w,h,scale):
        with Image.open(path)as im:
            dw,dh=im.width*scale,im.height*scale;assert dw<=w+.001 and dh<=h+.001
            c.drawImage(ImageReader(im),x+(w-dw)/2,y+(h-dh)/2,dw,dh)
    def footer(i,s):
        c.setStrokeColor(HexColor('#D7DEEA'));c.line(34,49,W-34,49);text(s,34,29,9,color='#63748A');text(str(i),W-57,29,10,color='#63748A');c.showPage()
    text('PROJECT EVERYTHING / MODERN EXTENDED AE',34,815,10,True,'#526C99')
    text('Extended AE',34,750,38,True);text('Original UI beside the implemented window',34,703,26)
    text('76 side-by-side comparisons',34,641,23,True)
    for x,mc,count,version in [(34,'1.21.1',40,'Extended AE 2.2.35 / AE2 19.2.18'),(614,'26.1.2',36,'Extended AE 1.0.3 / AE2 26.1.13-beta')]:
        frame(x,451,552,151);text(mc+' / NeoForge',x+22,565,23,True);text(str(count)+' window designs',x+22,527,18);text(version,x+22,491,12)
    para('Every comparison uses actual game captures: the publisher UI with SIU absent on the left, and the final implemented SIU window on the right. Both modern releases keep their native palette, artwork and controls.',34,408,1090,15,23)
    para('Where the original embeds the player inventory, SIU removes it from the work area and provides a separate Inventory window. Images within each pair use the same pixel scale; crops include the complete visible sidebars and trays.',34,302,1090,13,21)
    para('The 1.21.1 section also includes the three optional AppliedE/ProjectE EMC interfaces. Repeated edits and identical job pages are omitted. Read-only pattern inspectors remain native and are outside this window comparison.',34,206,1090,12,20)
    text('1.21.1 begins on page 2.  26.1.2 begins on page 42.',34,95,13,True)
    footer(1,'Actual screenshots | Visual comparison catalog | No mockups or reconstructed UI pixels')
    last=None
    for i,p in enumerate(pages,2):
        mc=p['minecraft'];c.bookmarkPage(p['id'])
        if mc!=last:c.addOutlineEntry(mc+' NeoForge',p['id'],0);last=mc
        c.addOutlineEntry(p['title'],p['id'],1)
        text('EXTENDED AE / MODERN WINDOW COMPARISON',34,815,10,True,'#526C99')
        text(p['title'],34,774,26,True)
        optional=' | AppliedE + ProjectE installed'if p['name'].startswith('ex-emc-')else''
        text('Minecraft '+mc+' / NeoForge / '+p['extended_ae']+optional,34,746,11,color='#63748A')
        text('ORIGINAL UI / SIU ABSENT',34,711,11,True,'#526C99');text('IMPLEMENTED SIU WINDOW',614,711,11,True,'#526C99')
        lp=ROOT/p['original'];rp=ROOT/p['siu'];li=Image.open(lp);ri=Image.open(rp);scale=min(1.2,528/max(li.width,ri.width),496/max(li.height,ri.height));li.close();ri.close()
        for x,path in [(34,lp),(614,rp)]:frame(x,177,552,521);picture(path,x+12,189,528,497,scale)
        note='Native work area retained, with the embedded player inventory moved into SIU\'s separate Inventory window.'
        if p['name'].startswith('pattern-modifier-'):note='Original mode controls, native colors and complete slot rims are retained beneath SIU\'s title controls.'
        if p['name']=='pattern-access-populated':note='Native search and provider workflow in a compact SIU window. The reference is unpowered; SIU shows an active provider.'
        if p['name']=='wireless-pattern-access':note='Native wireless search and provider-list workflow, presented as a compact SIU window. Both pictured lists are empty.'
        if p['name']=='assembler-matrix-populated':note='Actual formed Assembler Matrices. Native search, job status and pattern actions stay in the SIU work area; pictured contents differ.'
        if p['name']in ['config_modifier','void_cell']:note='The native standalone editor is retained as a compact SIU window, using the modern AE2 palette.'
        if p['name']in ['renamer','output-side-selection','precise-amount-editor']:note='The original editor or subview is hosted in SIU, retaining its native controls and return workflow.'
        end=para(note,34,139,1118,12,18);assert end>=88
        text('Same pixel scale within this pair. Images are cropped from full-screen captures; UI artwork is not retouched.',34,70,10,color='#63748A')
        p.update(page=i,display_scale=scale)
        footer(i,mc+' NeoForge | '+p['screen'].split('.')[-1]+' | Original at left, actual SIU implementation at right')
    text('CAPTURE NOTES / SOURCES',34,815,10,True,'#526C99');text('What is included',34,760,30,True)
    y=para('76 comparisons: 40 designs on Minecraft 1.21.1 and 36 on 26.1.2. Separate import/export views, Oversize Interface and all four Pattern Modifier modes are shown. The repeated paging and edited-value captures from focused testing are intentionally not duplicated.',34,706,1100,15,23)
    y=para('Originals were captured in disposable native clients with SIU Core, Compatibility and Controlify absent. SIU images use the published distribution jars from the completed rollout. The optional EMC images use the same shipping 1.21.1 Compatibility jar with AppliedE 1.0.8-beta and ProjectE 1.1.0 installed.',34,y-36,1100,14,22)
    y=para('This is a visual comparison, not a new complete functional or gameplay test. Progress bars, network status, item contents and cursor state can differ between the two disposable worlds. Read-only pattern inspectors retain their original native UI and are not new windows.',34,y-34,1100,13,21)
    sources=[('Extended AE publisher releases','https://modrinth.com/mod/extended-ae/versions'),('AppliedE 1.0.8-beta','https://modrinth.com/mod/appliede/version/R5MXisky'),('ProjectE 1.21.1-PE1.1.0','https://www.curseforge.com/minecraft/mc-mods/projecte/files/6611984')]
    y-=42
    for label,url in sources:
        text(label,34,y,13,True,'#526C99');c.linkURL(url,(34,y-3,550,y+15),relative=0);y-=31
    text('Capture paths, image hashes and crop rectangles are retained in the companion manifest.',34,94,11,color='#63748A')
    footer(78,'Native publisher screenshots + actual shipping SIU screenshots | Visual review complete after render verification')
    c.save();reader=PdfReader(PDF);assert len(reader.pages)==78
    for p in pages:
        page=reader.pages[p['page']-1];assert p['title']in page.extract_text()
        assert sum(op==b'Do'for _,op in page.get_contents().operations)==2
    manifest=dict(pdf=str(PDF.relative_to(ROOT)),pages=78,comparisons=len(pages),counts={'1.21.1':40,'26.1.2':36},all_images_in_game=True,crops_only=True,modern=True,pairs=pages,pdf_sha256=sha(PDF),visual_review='pending')
    (WORK/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print(json.dumps(dict(pdf=str(PDF),pages=78,comparisons=76,bytes=PDF.stat().st_size),indent=2))

if __name__=='__main__':main()
