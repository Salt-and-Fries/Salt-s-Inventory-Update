"""Inventory actual native references for the user-selected window mockups.

No production mod files are touched. Native screenshots remain unmodified.
Analysis crops and candidate content bounds are not final approved designs.
"""
from collections import Counter
import hashlib,json
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/project-everything-56-design'
TMP=ROOT/'build/project-everything-56-design'
IDS={7,9,10,11,12,13,14,16,18,24,27,44,49,52,55,85,86,88,97,101,105,169,187,189,192,216,219,275,282,283,285,320,329,364,371,375,398,405,407,408,410,415,425,441,486,495,529,537,552,554,558,568,577,584,589,605,607,613}

def main():
    OUT.mkdir(parents=True,exist_ok=True);TMP.mkdir(parents=True,exist_ok=True)
    j=json.loads((ROOT/'output/remaining-ui-audit/coverage.json').read_text())
    inv=json.loads((ROOT/'output/remaining-ui-audit/inventory.json').read_text())
    entries={r['entry']:r for r in inv['entries'] if r['entry'] in IDS}
    records={}
    for f in (ROOT/'build/remaining-ui-audit/gameplay').rglob('*.json'):
        if not ((f.name.startswith('audit-') and f.name.endswith('-cases.json')) or 'records' in f.name):continue
        try:rs=json.loads(f.read_text(encoding='utf8'))
        except (ValueError,UnicodeError):continue
        if not isinstance(rs,list):continue
        for r in rs:
            if isinstance(r,dict) and r.get('capture') and r.get('observation',{}).get('client'):
                records[r['capture'].replace('\\','/')]=dict(r,__record_file=str(f.relative_to(ROOT)))
    rows=[]
    for c in j['cases']:
        if c['entry'] not in IDS:continue
        s=c['native'];p=ROOT/s['capture'];im=Image.open(p).convert('RGB')
        assert hashlib.sha256(p.read_bytes()).hexdigest()==s['sha256']
        rec=records.get(s['capture'].replace('\\','/'))
        if rec is None:
            history=p.parent.parent/'observations.json'
            if history.exists():
                for o in json.loads(history.read_text(encoding='utf8')):
                    if o.get('command',{}).get('capture')==p.stem:
                        rec={'observation':o,'__record_file':str(history.relative_to(ROOT))}
        state=rec['observation']['client'] if rec else None
        if state is None:
            staged=json.loads((p.parent.parent/'staged-artifacts.json').read_text(encoding='utf-8-sig'))
            if staged.get('siu_loaded') is not False:raise ValueError('Missing native isolation evidence: '+c['id'])
            # The audit's earlier batch file was overwritten by a later batch,
            # but its original bounds/slots and raw hash remain in the ledger.
            # The native harness uses guiScale:2 at this 1280x900 viewport.
            state={'bounds':s.get('bounds'),'slots':s.get('slots',[]),'screen':s['screen'],
                   'guiWidth':im.width/2,'guiHeight':im.height/2,'siu_loaded':False}
            rec={'__record_file':'output/remaining-ui-audit/coverage.json',
                 '__record_note':'Retained original capture geometry; earlier batch observation overwritten. Native QA configured GUI scale 2.'}
        if state.get('siu_loaded') is True:raise ValueError('SIU present in native reference')
        sx=im.width/state['guiWidth'];sy=im.height/state['guiHeight'];bounds=state.get('bounds') or s.get('bounds')
        slots=state.get('slots',s.get('slots',[]));players=[v for v in slots if v.get('player')];nonplayers=[v for v in slots if not v.get('player')]
        ys=Counter(round(v['y']) for v in players);grid_ys=[y for y,n in ys.items() if n>=9]
        inventory_y=min(grid_ys) if grid_ys else None
        if bounds:
            w,h=round(bounds['width']),round(bounds['height'])
            box=[round(bounds['x']*sx),round(bounds['y']*sy),round((bounds['x']+w)*sx),round((bounds['y']+h)*sy)]
            native=im.crop(box).resize((w,h),Image.Resampling.NEAREST)
            top=16;bottom=inventory_y-12 if inventory_y is not None else h-7
            bottom=min(h-7,max(top+18,bottom));left,right=7,w-7
            strip=native.crop((6,5,w-6,14));bg=Counter(strip.getdata()).most_common(1)[0][0]
            area=native.crop((left,top,right,bottom));mask=Image.new('L',area.size);pix=area.load();mp=mask.load()
            for y in range(area.height):
                for x in range(area.width):
                    col=pix[x,y]
                    if max(abs(col[k]-bg[k]) for k in range(3))>3:mp[x,y]=255
            bb=mask.getbbox()
            rect=[left+bb[0],top+bb[1],left+bb[2],top+bb[3]] if bb else [left,top,right,bottom]
            content=native.crop(tuple(rect))
        else:
            box=s['crop_rect'];native=im.crop(box);content=native;rect=None;bg=(198,198,198)
        label=s.get('title') or c['id'].replace('-',' ')
        row={'id':c['id'],'entry':c['entry'],'family':entries[c['entry']]['title'],'label':label,'minecraft':c['minecraft'],'loader':c['loader'],
             'native':s,'native_observation_file':rec.get('__record_file'),'geometry_note':rec.get('__record_note','Original native observation'),
             'native_state':state,'scale':[sx,sy],'bounds':bounds,'source_rect_gui':rect,'background_rgb':list(bg),
             'inventory_y':inventory_y,'player_slots':len(players),'owned_slots':len(nonplayers),'review':'pending individual design review'}
        rows.append(row)
        preview=Image.new('RGB',(700,330),'#e7eef0');d=ImageDraw.Draw(preview)
        d.text((8,6),c['id'],fill='black');d.text((8,24),row['family']+' | '+str(rect),fill='black')
        for image,x in [(native,8),(content,358)]:
            image.thumbnail((334,272),Image.Resampling.NEAREST);preview.paste(image,(x,52))
        preview.save(TMP/(c['id']+'-analysis.png'))
    plan={'scope':'56 selected mod families; representative releases from actual native-only gameplay audit. Original pixels are reference material, not implementation proof.',
          'selected_entries':sorted(IDS),'families':56,'native_cases':len(rows),'cases':rows,
          'standalone_screens':[{k:r[k] for k in ('entry','title','standalone_screens')} for r in entries.values() if r.get('standalone_screens')]}
    (OUT/'native-reference-plan.json').write_text(json.dumps(plan,indent=2),encoding='utf8')
    for start in range(0,len(rows),6):
        sheet=Image.new('RGB',(1400,990),'white')
        for k,r in enumerate(rows[start:start+6]):
            with Image.open(TMP/(r['id']+'-analysis.png')) as im:sheet.paste(im,((k%2)*700,(k//2)*330))
        sheet.save(TMP/f'analysis-{start//6+1:02}.png')
    print(json.dumps({'families':56,'audit_entries':len(IDS),'native_cases':len(rows),'analysis_sheets':(len(rows)+5)//6,'plan':str(OUT/'native-reference-plan.json')}))

if __name__=='__main__':main()
