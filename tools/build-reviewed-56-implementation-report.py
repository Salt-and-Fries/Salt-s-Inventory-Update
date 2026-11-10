"""Actual 1.20.1 native/SIU evidence atlas. Never substitutes a design mockup.

The manifest links unchanged originals and their hashes to crop-only derivatives.
Missing evidence is fatal for PDF generation, and visible failures are excluded
explicitly rather than automatically classed as a successful opening.
"""
import argparse, hashlib, json, math
from collections import defaultdict
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/project-everything-56-implementation'
PDF=ROOT/'output/pdf/siu-project-everything-56-implemented-comparisons.pdf'
def read(p): return json.loads((ROOT/p).read_text(encoding='utf-8-sig'))
def sha(p): return hashlib.sha256((ROOT/p).read_bytes()).hexdigest()
EXCLUDED={'item-exposure-album','own-exposure-lectern','bf-waystone','own-bf-sharestone-real-aligned'}
RUNS=['ag04','pr05','pr08','bb02','bb04','charm-core01','bf01','bf02','bf03','bf04','bf05','pr10','bf08','bf09','bf10','pr13','pr14','pr17','pr18','ag05']
ALIASES={
 'charm-kiln':'charm-kiln-isolated-active',
 'shared-pr-dragon-mounted':'shared-pr-dragon-mounted-key',
 'shared-bb-accessorify-ender':'accessorify-equipped-ender',
 'shared-bb-accessorify-shulker':'accessorify-equipped-shulker',
 'cplus-ecologics-chest-boat':'ecologics-1201-chest-boat',
 'design-cooking-book-open':'cooking-search-typed',
}
TITLES={'item-candlelight-letter-open':'Letter','own-ae-part-cell-dock':'ME Cell Dock',
 'own-ae-part-requester-terminal':'ME Requester Terminal','merequester-requester':'ME Requester',
 'pr-tierify-reforge':'Reforge Gear','bf-waystone':'Waystone destinations',
 'own-bf-sharestone-real-aligned':'Sharestone destinations'}
def valid(r, excluded=False):
    c=r.get('observation',{}).get('client',{})
    if not r.get('capture') or not (ROOT/r['capture']).is_file():return False
    if r.get('sha256') and sha(r['capture'])!=r['sha256']:return False
    if excluded:return c.get('screen') not in (None,'none') and not c.get('allWindows')
    return bool(c.get('allWindows'))
def collect():
    db={}
    for suffix in RUNS:
        for d in sorted((ROOT/'build/remaining-ui-audit/gameplay').glob('*-integrated-reviewed-'+suffix)):
            for f in sorted(d.glob('*.json')):
                if 'records' not in f.name and f.name!='audit-core-cases.json':continue
                data=read(f)
                if not isinstance(data,list):continue
                for r in data:
                    if valid(r,r.get('id') in EXCLUDED):db[r['id']]={**r,'record_file':str(f.relative_to(ROOT))}
    return db
def capture_row(r):
    return {'capture':r['capture'],'sha256':r.get('sha256') or sha(r['capture']),
            'state':r['observation']['client'],'record_file':r.get('record_file')}
def native_override(case,run,ident):
    f=ROOT/'build/remaining-ui-audit/gameplay'/run/'manual-ui-records.json'
    rows=read(f)
    if f.parent.joinpath('audit-native-cases.json').exists():rows+=read(f.parent/'audit-native-cases.json')
    r=next(r for r in rows if r['id']==ident)
    case['native']=capture_row(r);case['native'].pop('record_file',None)
    case['native']['state']=r['observation']['client']
def crop(row,core=False):
    assert sha(row['capture'])==row['sha256'],row['capture']
    im=Image.open(ROOT/row['capture']).convert('RGB');state=row['state']
    if row.get('force_crop'):return im.crop(tuple(row['crop_rect']))
    # Old audit crops sometimes included an unrelated FTB sidebar at (0,0),
    # reducing the actual inventory to a tiny image. Recompute those from the
    # native screen geometry and its nearby controls instead.
    b=state.get('bounds');old=row.get('crop_rect');sx=im.width/state.get('guiWidth',640);sy=im.height/state.get('guiHeight',450)
    useful=old and (row.get('force_crop') or not b or (old[2]-old[0])*(old[3]-old[1]) < b['width']*b['height']*sx*sy*2.1)
    if not core and useful:
        rect=list(row['crop_rect'])
        if any(part in state.get('screen','') for part in ('megacells','merequester','appeng')):
            rect=[max(0,rect[0]-80),max(0,rect[1]-20),min(im.width,rect[2]+100),min(im.height,rect[3]+20)]
        return im.crop(tuple(rect))
    sx=im.width/state.get('guiWidth',640);sy=im.height/state.get('guiHeight',450)
    boxes=[]
    if core:
        for w in state.get('allWindows',[]):
            b=w['bounds'];x,y=b['x'],b['y'];boxes.append((x,y,x+b['width'],y+b['height']))
            for r in (w.get('composition')or{}).get('regions',[]):
                if r.get('exterior'):
                    s=r['source'];boxes.append((x+r['x'],y+r['y'],x+r['x']+s['width'],y+r['y']+s['height']))
    if not boxes and state.get('bounds'):
        b=state['bounds'];boxes.append((b['x'],b['y'],b['x']+b['width'],b['y']+b['height']))
        for w in state.get('widgets',[]):
            if w.get('visible',True) and w.get('width',0)>0 and w.get('height',0)>0:
                if w['x']+w['width']<b['x']-80 or w['x']>b['x']+b['width']+80 or w['y']+w['height']<b['y']-50 or w['y']>b['y']+b['height']+50:continue
                boxes.append((w['x'],w['y'],w['x']+w['width'],w['y']+w['height']))
    if boxes:
        # AE2 side tabs are native composite widgets rather than ordinary
        # Screen children, so the base container bounds omit their width.
        native_ae=not core and any(part in state.get('screen','') for part in ('megacells','merequester','appeng'))
        pad=40 if core and any('ae2.nativeview' in w.get('definition','') for w in state.get('allWindows',[])) else 12 if core else 8
        right_pad=50 if native_ae else pad
        rect=(max(0,int((min(b[0] for b in boxes)-pad)*sx)),max(0,int((min(b[1] for b in boxes)-pad)*sy)),
              min(im.width,math.ceil((max(b[2] for b in boxes)+right_pad)*sx)),min(im.height,math.ceil((max(b[3] for b in boxes)+pad)*sy)))
    else:rect=tuple(row.get('crop_rect',(0,0,im.width,im.height)))
    row['crop_rect']=list(rect)
    return im.crop(rect)
def prepare():
    OUT.mkdir(parents=True,exist_ok=True);(OUT/'crops').mkdir(exist_ok=True)
    plan=read('output/project-everything-56-design/native-reference-plan.json')
    designs={r['id']:r for r in read('output/project-everything-56-design/design-manifest.json')['cases']}
    db=collect();rows=[];missing=[]
    for ident,run,label in (
        ('design-cooking-book-open','1.20.1-fabric-native-prod-design-ag01','Cooking Pot / recipe drawer'),
        ('design-wands-tools-request','1.20.1-fabric-native-prod-design-pr07','Wand tool picker')):
        r=next(r for r in read(Path('build/remaining-ui-audit/gameplay')/run/'manual-ui-records.json') if r['id']==ident)
        plan['cases'].append({'id':ident,'family':designs[ident]['family'],'loader':'fabric','label':label,
            'native_state':r['observation']['client'],'native':{'crop_rect':[242,268,982,630] if ident=='design-cooking-book-open' else None}})
    for base in plan['cases']:
        ident=base['id'];d=designs[ident]
        c={'id':ident,'family':base['family'].replace('Guard Villagers (Fabric/Quilt)','Guard Villagers'),'label':TITLES.get(ident,base.get('native_state',{}).get('title') or base['label'].replace('-',' ').title()),
           'minecraft':'1.20.1','loader':base['loader'],'release':d['reference_release'],
           'native':{'capture':d['native_capture'],'sha256':d['native_sha256'],
                     'state':base['native_state'],'crop_rect':base['native'].get('crop_rect')},
           'excluded':ident in EXCLUDED}
        if ident=='design-cooking-book-open':c['native']['force_crop']=True
        if ident=='cplus-ecologics-chest-boat':
            c.update(loader='fabric',release='2.2.2')
            native_override(c,'1.20.1-fabric-native-reviewed-bb03','ecologics-1201-chest-boat')
        if ident=='charm-kiln':native_override(c,'1.20.1-fabric-native-reviewed-charm-native01','charm-kiln-isolated')
        if ident.startswith('shared-bb-accessorify-'):
            native_override(c,'1.20.1-fabric-native-reviewed-bb03',ALIASES[ident])
        r=db.get(ALIASES.get(ident,ident))
        if not r or not valid(r,c['excluded']):missing.append(ident);continue
        c['core']=capture_row(r)
        if c['excluded']:
            c['label']=TITLES.get(ident,c['label'])
            c['core']['crop_rect']=c['native'].get('crop_rect')
            # These dialogs have no container geometry. Preserve the whole
            # central viewer and its navigation while omitting unrelated HUD.
            rect=[242,240,1038,718] if 'exposure' in ident else [400,240,880,662]
            for side in ('native','core'):c[side]['crop_rect']=rect;c[side]['force_crop']=True
        for side in ('native','core'):
            path=OUT/'crops'/f'{ident}-{side}.png';crop(c[side],side=='core').save(path);c[side]['crop']=str(path.relative_to(ROOT))
        rows.append(c)
    manifest={'minecraft':'1.20.1','core_version':'0.1.6','compatibility_version':'0.1.3','families':56,
              'comparisons':len(rows),'excluded_native_viewers':len(EXCLUDED),'missing':missing,'cases':rows}
    followup=db.get('umvuthi-replenish-compact')
    if followup:
        extra=capture_row(followup);path=OUT/'crops/umvuthi-replenish-followup.png';crop(extra,True).save(path);extra['crop']=str(path.relative_to(ROOT));manifest['trade_followup']=extra
    (OUT/'implementation-evidence.json').write_text(json.dumps(manifest,indent=2)+'\n')
    # Crops are for visual review only; every original PNG is preserved.
    font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',18)
    for start in range(0,len(rows),8):
        sheet=Image.new('RGB',(1440,1200),'#eef4f3');draw=ImageDraw.Draw(sheet)
        for k,r in enumerate(rows[start:start+8]):
            x=k%2*720;y=k//2*300
            draw.text((x+10,y+4),r['id'],fill='#143f43',font=font)
            for n,side in enumerate(('native','core')):
                im=Image.open(ROOT/r[side]['crop']);im.thumbnail((345,260),Image.Resampling.NEAREST)
                sheet.paste(im,(x+n*355+(355-im.width)//2,y+32+(260-im.height)//2))
        sheet.save(OUT/f'review-{start//8+1:02}.png')
    print(json.dumps({'comparisons':len(rows),'missing':missing,'manifest':str(OUT/'implementation-evidence.json')}))
    return manifest

PW,PH=840,594
TEAL='#177b80';INK='#203238';MUTED='#64767c';PAPER='#eef4f3'
def text(c,s,x,y,size=10,bold=False,color=INK):
    c.setFont('Helvetica-Bold' if bold else 'Helvetica',size);c.setFillColor(HexColor(color));c.drawString(x,y,s)
def wrap(c,s,width,size=10):
    words=s.split();lines=[];line=''
    for w in words:
        candidate=(line+' '+w).strip()
        if c.stringWidth(candidate,'Helvetica',size)>width and line:lines.append(line);line=w
        else:line=candidate
    return lines+[line]
def para(c,s,x,y,width=772,size=11):
    for line in wrap(c,s,width,size):text(c,line,x,y,size);y-=size*1.4
    return y
def header(c,title,sub):
    text(c,'PROJECT EVERYTHING / IMPLEMENTED UI REVIEW',32,PH-33,9,True,TEAL)
    size=24
    while c.stringWidth(title,'Helvetica-Bold',size)>772:size-=1
    text(c,title,32,PH-68,size,True);text(c,sub,32,PH-94,12)
def footer(c,page):
    c.setStrokeColor(HexColor('#cbd7d8'));c.line(32,31,808,31)
    text(c,'Actual in-game screenshots | Minecraft 1.20.1 | Core 0.1.6 + Compatibility 0.1.3',32,18,7,color=MUTED)
    text(c,str(page),790,18,8,color=MUTED)
def picture(c,path,box):
    im=Image.open(ROOT/path);x,y,w,h=box;s=min(w/im.width,h/im.height,1.1)
    c.drawImage(ImageReader(im),x+(w-im.width*s)/2,y+(h-im.height*s)/2,im.width*s,im.height*s,mask='auto')
def build(m):
    if m['missing']:raise ValueError('Complete genuine evidence before PDF: '+str(m['missing']))
    grouped=defaultdict(list)
    for r in m['cases']:
        if not r['excluded']:grouped[r['family']].append(r)
    families=sorted(grouped,key=str.casefold)
    first_pages={};number=2+math.ceil(len(families)/24)
    for f in families:first_pages[f]=number;number+=len(grouped[f])
    PDF.parent.mkdir(exist_ok=True);c=canvas.Canvas(str(PDF),pagesize=(PW,PH),pageCompression=1)
    c.setTitle('Project Everything - 56 mod families - Implemented 1.20.1 UI comparisons')
    page=1
    header(c,'Native UI to SIU windows','56 mod families / Minecraft 1.20.1 / Fabric and Forge references')
    y=PH-142
    for s in [
      f"{len(m['cases'])-4} inventory/container comparisons, plus four deliberately native viewers. Each pair shows an original native capture and an actual running SIU capture, rather than a mockup.",
      'The approved designs guide the result. Native artwork, complete controls and model previews are preserved; unused player-inventory bands become separate SIU inventory windows. Existing vanilla-shaped storage and merchant windows are reused.',
      'Book/album viewers and Waystone/Sharestone destination lists remain native. Warp Plate item slots are included. The Supplementaries Red Merchant uses the existing SIU trade implementation.',
      'The version and loader printed on each page describe that captured reference. This is focused UI and interaction evidence, not an exhaustive gameplay test of every recipe, progression state, loader combination or multiplayer scenario.',
      'The appendix records exercised controls, runtime provenance and the upstream Charm fixture conflict. Equipment, contents and environmental lighting may differ between the two disposable test worlds.'
    ]:y=para(c,s,34,y)-22
    footer(c,page);c.showPage();page+=1
    for start in range(0,len(families),24):
        header(c,'Contents','Inventory/container comparisons grouped by mod family')
        y=PH-130
        for f in families[start:start+24]:text(c,f,35,y,11);text(c,str(len(grouped[f]))+' views / p. '+str(first_pages[f]),700,y,10,color=MUTED);y-=17
        footer(c,page);c.showPage();page+=1
    pages=[]
    for family in families:
        c.bookmarkPage('family-'+str(len(pages)));c.addOutlineEntry(family,'family-'+str(len(pages)),0,False)
        for r in grouped[family]:
            header(c,family,r['label']);text(c,f"Minecraft 1.20.1 / {r['loader'].title()} / upstream {r['release']}",32,PH-116,9,color=MUTED)
            for x,side,label in ((32,'native','WITHOUT SIU / ORIGINAL MOD UI'),(438,'core','WITH SIU / IMPLEMENTED WINDOW')):
                text(c,label,x,PH-142,9,True,TEAL);c.setFillColor(HexColor(PAPER));c.roundRect(x,92,370,348,7,fill=1,stroke=0)
                picture(c,r[side]['crop'],(x+8,100,354,332))
            note='Native menus and artwork retained; SIU provides window placement, controls and the separate player inventory.'
            if 'merchant' in r['id']:note='Uses SIU\'s existing trading interface, including offer selection, payment and result slots.'
            if r['id']=='charm-kiln':note='Isolated native and SIU fixtures shown. The combined fixture also failed to open this kiln without SIU; see appendix.'
            para(c,note,34,76,772,9)
            text(c,'Evidence ID: '+r['id'],34,40,7,color=MUTED)
            footer(c,page);pages.append({'id':r['id'],'page':page});c.showPage();page+=1
    for r in m['cases']:
        if not r['excluded']:continue
        header(c,'Retained native interface',r['family']+' / '+r['label'])
        for x,side,label in ((32,'native','WITHOUT SIU'),(438,'core','WITH SIU / ORIGINAL VIEWER RETAINED')):
            text(c,label,x,PH-132,9,True,TEAL);c.setFillColor(HexColor(PAPER));c.roundRect(x,92,370,348,7,fill=1,stroke=0)
            picture(c,r[side]['crop'],(x+8,100,354,332))
        para(c,'This surface has no inventory workflow to convert. It deliberately keeps the mod\'s original screen and navigation.',34,76,772,10)
        footer(c,page);pages.append({'id':r['id'],'page':page});c.showPage();page+=1
    if m.get('trade_followup'):
        header(c,"Mowzie's Mobs",'Umvuthi / state after completing a trade')
        picture(c,m['trade_followup']['crop'],(70,145,700,300))
        para(c,'The original handler consumed seven gold blocks. Replenish stays in the compact trade body, and the state survives reopening. This is an additional SIU interaction-state capture.',50,110,740,11)
        footer(c,page);c.showPage();page+=1
    header(c,'Validation and limits','Focused checks for the interfaces changed in this phase')
    y=PH-135
    for s in [
      'Built both 1.20.1 Core and Compatibility targets. All 34 distinct focused regression tests passed across the relevant runs, covering complete cells and inverse pointer mapping, exact menu claims, exterior controls, dynamic ship rows, session payloads and the production AE2 input bridge.',
      'Exercised native openers rather than fabricated providers: recipe search, ship paging/cargo transfer, equipped Accessorify shortcuts, XP withdrawal/reopening, a completed Red Merchant trade and stable anvil/reforge transitions. Requester checkbox and batch-size edits persisted after reopening. Sculptor accepted payment and started its test; Umvuthi consumed payment and retained Replenish after reopening.',
      'Fabric references use production intermediary launches with original audited mod releases. The Forge screenshot fixture uses the official SRG-to-named remapper; original and derived JAR hashes and resource preservation are recorded. Forge screenshots are development-runtime evidence, not a separate production SRG launch.',
      'Charm Forked: the combined 1.20.1 fixture produced the same KILN enum failure without SIU. Its isolated original and SIU kiln openings are shown. Smelting output was not established by this focused UI check.',
      'Storage skins and dynamic states are represented by the captured variants. Other inherited containers, optional recipes, equipment combinations, external configuration dialogs, guidebooks, full progression and every possible model rotation are outside this focused validation.',
      'Original screenshots, action logs, staged JAR hashes and crop coordinates are linked in output/project-everything-56-implementation/implementation-evidence.json. No mockup pixels are presented as a running integration.'
    ]:y=para(c,s,34,y,772,10)-16
    footer(c,page);c.showPage();page+=1
    omitted=read('output/project-everything-56-design/native-reference-plan.json')['standalone_screens']
    header(c,'Other native screens','Accounting for guidebooks, settings and non-container dialogs')
    y=PH-135
    y=para(c,'These source-audit classes do not become SIU inventory windows in this phase. They retain their original presentation. The four reviewed album/destination viewers have explicit comparison pages above.',34,y,772,10)-18
    for family in omitted:
        names=', '.join(s.rsplit('/',1)[-1] for s in family['standalone_screens'])
        lines=wrap(c,names,744,9);needed=22+len(lines)*13
        if y-needed<58:
            footer(c,page);c.showPage();page+=1;header(c,'Other native screens','Continued');y=PH-135
        text(c,family['title'],34,y,10,True);y-=17
        for line in lines:text(c,line,45,y,9,color=MUTED);y-=13
        y-=12
    footer(c,page);c.showPage();page+=1
    c.save();m.update(pdf=str(PDF.relative_to(ROOT)),pages=page-1,page_index=pages)
    (OUT/'implementation-evidence.json').write_text(json.dumps(m,indent=2)+'\n')
    print(json.dumps({'pdf':str(PDF),'pages':page-1,'views':len(m['cases'])}))
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--prepare-only',action='store_true');a=p.parse_args();m=prepare()
    if not a.prepare_only:build(m)
