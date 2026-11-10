"""Translate approved native-frame geometry into Compatibility-only specifications.

Resources are referenced from the publisher; no Ad Astra artwork is redistributed.
"""
from pathlib import Path
from io import BytesIO
import json,zipfile
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/ad-astra-implemented'
read=lambda p:json.loads(p.read_text(encoding='utf-8'))
def transparent_boxes(im,ox=0,oy=0):
    active={};boxes=[]
    for y in range(im.height+1):
        runs=[]
        if y<im.height:
            x=0
            while x<im.width:
                if im.getpixel((x,y))[3]:x+=1;continue
                start=x
                while x<im.width and not im.getpixel((x,y))[3]:x+=1
                runs.append((start,x))
        for run in list(active):
            if run not in runs:
                start=active.pop(run);boxes.append([ox+run[0],oy+start,run[1]-run[0],y-start])
        for run in runs:active.setdefault(run,y)
    return boxes
def main():
    approved=read(ROOT/'output/ad-astra-window-design/manifest.json')['cases']
    native=read(ROOT/'tmp/pdfs/ad-astra/audit-cases.json')
    native+=read(ROOT/'output/remaining-ui-audit/pr-supp-pairs.json')['cases']
    rows={r['id']:r['native']for r in native}
    result=[]
    for mc in ['1.20.1','1.21.1']:
        fixture=read(OUT/f'fixture-{mc}-fabric.json')
        paths={a['filename']:ROOT/a['path']for a in fixture['artifacts']}
        primary=next(p for n,p in paths.items()if n.startswith(('ad_astra-','adastra-')))
        addon=next(p for n,p in paths.items()if n.startswith('Ad-Astra-Giselle'))
        with zipfile.ZipFile(primary)as publisher:
            toolbar_tile=Image.open(BytesIO(publisher.read('assets/ad_astra/textures/gui/sprites/options_bar.png'))).convert('RGBA')
        specs=[]
        for key in ['ad_astra','ad_astra_giselle_addon']:
            with zipfile.ZipFile(primary if key=='ad_astra' else addon)as jar:
                assets={n.split('/')[-1][:-4]:Image.open(BytesIO(jar.read(n))).convert('RGBA')for n in jar.namelist()if n.startswith('assets/'+key+'/textures/gui/container/')and n.endswith('.png')and '/slots/'not in n}
                cases=approved if key=='ad_astra'else [dict(id=id,asset=id.removeprefix('ad-astra-giselle-addon-').replace('-','_'),title=n['title'])for id,n in rows.items()if id.startswith('ad-astra-giselle-addon-')]
                for case in cases:
                    n=rows[case['id']];asset=case['asset'];atlas=assets[asset];w,h=atlas.size
                    players=[s for s in n['slots']if s['player']];own=[s for s in n['slots']if not s['player']]
                    inv=min([s['y']for s in players],default=h+1);plate=case.get('native_plate_start',0)
                    vehicle=asset in ['rocket','rover','lander'];battery=bool(not vehicle and asset not in ['nasa_workbench','rocket_sensor'])
                    if key=='ad_astra':
                        body=case['body_source'];head=case['body_destination'][1];height=case['root_size'][1];fragments=[]
                        for f in case['attachments']:
                            l,t,r,b=f['source'];mask=atlas.crop((l,t,r,b))if f['id']=='native-status-monitor'else Image.new('RGBA',(r-l,b-t),(0,0,0,255))
                            # Options-bar rounded corners are the only transparent pixels.
                            if f['id']=='native-options-toolbar':
                                tile=Image.open(BytesIO(jar.read('assets/ad_astra/textures/gui/sprites/options_bar.png'))).convert('RGBA').crop((0,0,30,30))
                                for y in range(6):
                                    for x in range(6):
                                        for xx,yy,sx,sy in [(x,y,x,y),(mask.width-6+x,y,24+x,y),(x,mask.height-6+y,x,24+y),(mask.width-6+x,mask.height-6+y,24+x,24+y)]:mask.putpixel((xx,yy),tile.getpixel((sx,sy)))
                            fragments.append(dict(id=f['id'],source=[l,t,r-l,b-t],at=f['destination'],cutouts=transparent_boxes(mask,l,t)))
                    else:
                        bg=atlas.getpixel((7,16));cap=min(h-6,inv-1);end=14
                        for y in range(14,cap):
                            if any(atlas.getpixel((x,y))!=bg for x in range(6,w-6)):end=y+5
                        end=max(end,max([s['y']+21 for s in own if s['y']>=0],default=0))
                        if asset=='gravity_normalizer':end=cap # Includes live energy/tick text.
                        if asset=='rocket_sensor':end=h-6 # This status screen has no player grid.
                        end=min(cap,end);body=[0,14,w,end];head=22;height=head+end-14+6
                        count=4 if asset=='gravity_normalizer' else 3;bw=count*18+(count-1)*3+12
                        mask=Image.new('RGBA',(bw,30),(0,0,0,255))
                        for y in range(6):
                            for x in range(6):
                                for xx,yy,sx,sy in [(x,y,x,y),(bw-6+x,y,24+x,y),(x,24+y,x,24+y),(bw-6+x,24+y,24+x,24+y)]:
                                    mask.putpixel((xx,yy),toolbar_tile.getpixel((sx,sy)))
                        fragments=[dict(id='native-options-toolbar',source=[w-bw,-32,bw,30],at=[w-bw,-32],cutouts=transparent_boxes(mask,w-bw,-32))]
                        battery=asset in ['automation_nasa_workbench','gravity_normalizer']
                    menu=key+':'+asset+('_menu'if key=='ad_astra'else'')
                    spec=dict(id=case['id'],menu=menu,screen=n['screen'],title=case['title'],texture=key+':textures/gui/container/'+asset+'.png',
                              width=w,height=height,textureHeight=h,header=head,plate=plate,body=body,battery=battery,vehicle=vehicle,
                              labelY=inv-12 if players else -100,titleY=9 if key=='ad_astra'and asset in ['compressor','etrionic_blast_furnace','gravity_normalizer'] else 6,
                              titleColor=0xff2a262b if asset=='coal_generator' else 0xffdfdbe3,reserve=14 if plate else 0,fragments=fragments)
                    spec['titleColor']-=0x100000000
                    specs.append(spec)
        dst=ROOT/f'compatibility/versions/{mc}/common/src/main/resources/salts_inventory_compat/ad-astra/native-windows.json';dst.parent.mkdir(parents=True,exist_ok=True)
        dst.write_text(json.dumps(specs,indent=2)+'\n',encoding='utf-8');result.append(dict(minecraft=mc,layouts=len(specs),resources=str(dst.relative_to(ROOT))))
    (OUT/'geometry.json').write_text(json.dumps(result,indent=2),encoding='utf-8');print(json.dumps(result,indent=2))
if __name__=='__main__':main()
