"""Focused static-art comparison and genuine game-window crops for Powah."""
from pathlib import Path
from io import BytesIO
import hashlib
import json
import zipfile

from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/powah-implemented'
RUNS={'fabric':'1.20.1-fabric-siu-powah-implemented-02','forge':'1.20.1-forge-siu-powah-implemented-01'}
N=Image.Resampling.NEAREST
CASES=[
    ('reactor','own-powah-placed-reactor','reactor',75),('energy_cable','own-powah-connected-cable','cable',29),
    ('ender_cell','powah-ender-cell-basic','ender_cell',73),('ender_gate','powah-ender-gate-basic','ender_cell',73),
    ('energy_cell','powah-energy-cell-basic','energy_cell',50),('energy_discharger','powah-energy-discharger-basic','discharger',75),
    ('energy_hopper','powah-energy-hopper-basic','wide_energy',50),('furnator','powah-furnator-basic','furnator',75),
    ('magmator','powah-magmator-basic','magmator',75),('player_transmitter','powah-player-transmitter-basic','player_transmitter',50),
    ('solar_panel','powah-solar-panel-basic','wide_energy',50),('thermo_generator','powah-thermo-generator-basic','thermo',75),
]

# Exclude live fills, numbers and animations from static-art equality.
LIVE={
    'reactor':[(5,5,14,39),(103,13,5,48),(51,6,5,16),(51,52,5,16),(140,52,5,16),(114,28,4,18),(157,5,14,65)],
    'energy_cable':[(5,5,18,19),(40,9,85,11),(134,10,9,8)],
    'ender_cell':[(31,6,139,37),(4,54,168,14)],'ender_gate':[(31,6,139,37),(4,54,168,14)],
    'energy_cell':[(31,6,139,37)],'energy_discharger':[(6,6,164,37)],'energy_hopper':[(6,6,164,37)],
    'furnator':[(5,5,14,39),(110,18,4,16),(94,43,11,18)],
    'magmator':[(5,5,14,39),(83,29,11,18),(157,5,14,65)],
    'player_transmitter':[(31,6,139,37),(9,5,6,6)],'solar_panel':[(6,6,164,37)],
    'thermo_generator':[(5,5,14,39),(157,5,14,65),(34,10,115,10)],
}


def contains(b,x,y):
    return b['x']<=x<b['x']+b['width']and b['y']<=y<b['y']+b['height']


def main():
    refs={r['id']:r for r in json.loads((ROOT/'tmp/pdfs/powah/audit-cases.json').read_text())}
    fabric=ROOT/'build/ui-modpack-audit-20261025/jars/8885884da9e4470520faed11347507e3195e642d.jar'
    forge=OUT/'publisher-forge/Powah-5.0.11.jar'
    with zipfile.ZipFile(fabric)as z,zipfile.ZipFile(forge)as f:
        textures={}
        for asset in {r[2]for r in CASES}:
            path='assets/powah/textures/gui/container/'+asset+'.png'
            im=Image.open(BytesIO(z.read(path))).convert('RGBA');other=Image.open(BytesIO(f.read(path))).convert('RGBA')
            assert im.tobytes()==other.tobytes(),asset
            textures[asset]=im
    results=[];crops=[]
    for loader,name in RUNS.items():
        run=ROOT/'build/remaining-ui-audit/gameplay'/name;journal=json.loads((run/'powah-checks/journal.json').read_text())
        for block,rid,asset,h in CASES:
            record=next(r for r in journal['captures']if r['id']==block)
            client=json.loads((run/'powah-checks'/(block+'-client.json')).read_text())
            w=next(w for w in client['allWindows']if w.get('menuId')=='powah:'+record['menu'])
            assert not w['layoutError']
            shot=Image.open(ROOT/record['screenshot']).convert('RGBA');logical=shot.resize((640,450),N)
            b=w['bounds'];p=w['composition'];body=next(r for r in p['regions']if r['id']=='native-machine')
            dx=b['x']+7+body['x'];dy=b['y']+25+body['y']
            ref=refs[rid]['native'];reference=ROOT/ref['capture']
            assert hashlib.sha256(reference.read_bytes()).hexdigest()==ref['sha256']
            native=Image.open(reference).convert('RGBA').resize((640,450),N);nb=ref['bounds']
            width=153 if block=='energy_cable'else 176
            alpha=textures[asset].getchannel('A');count=0;diff=[]
            for y in range(h):
                for x in range(width):
                    if alpha.getpixel((x,y))!=255:continue
                    if any(l<=x<l+rw and t<=y<t+rh for l,t,rw,rh in LIVE[block]):continue
                    a=native.getpixel((nb['x']+x,nb['y']+y));v=logical.getpixel((dx+x,dy+y));count+=1
                    if a!=v:diff.append([x,y,list(a),list(v)])
            results.append(dict(loader=loader,view=block,compared_static_pixels=count,mismatches=len(diff),examples=diff[:8]))
            # Crop only; screenshots themselves remain immutable and unchanged.
            left=top=0;right=b['width'];bottom=b['height']
            for r in p['regions']:
                if r.get('exterior'):
                    left=min(left,r['x']);top=min(top,r['y']);right=max(right,r['x']+r['source']['width']);bottom=max(bottom,r['y']+r['source']['height'])
            crop=shot.crop(tuple(2*v for v in [b['x']+left-5,b['y']+top-5,b['x']+right+5,b['y']+bottom+5]))
            dest=OUT/(block+'-'+loader+'-window.png');crop.save(dest)
            crops.append(dict(loader=loader,view=block,source=str(ROOT/record['screenshot']),crop=str(dest),original_reference=rid))
    report=dict(scope='Stable opaque body pixels only; live numeric/fill/animation regions are excluded.',
                publisher_body_textures_identical_across_loaders=True,records=results,
                compared_static_pixels=sum(r['compared_static_pixels']for r in results),mismatches=sum(r['mismatches']for r in results),crops=crops)
    (OUT/'artwork-verification.json').write_text(json.dumps(report,indent=2)+'\n')
    for loader in RUNS:
        contact=Image.new('RGB',(1440,1040),'#eaf1f1');d=ImageDraw.Draw(contact)
        for i,row in enumerate(r for r in crops if r['loader']==loader):
            im=Image.open(row['crop']);im.thumbnail((456,215),N);x=i%3*480;y=i//3*260
            d.text((x+8,y+8),row['view']+' / '+loader,fill='#20373d');contact.paste(im,(x+(480-im.width)//2,y+32))
        contact.save(OUT/('live-contact-'+loader+'.png'))
    print(json.dumps({k:report[k]for k in ['compared_static_pixels','mismatches']},indent=2))
    print([(r['loader'],r['view'],r['mismatches'])for r in results])


if __name__=='__main__':main()
