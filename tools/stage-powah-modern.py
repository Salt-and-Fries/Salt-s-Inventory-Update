"""Verify and stage only Powah's two additional official SIU targets."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile
from PIL import Image,ImageDraw

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/powah-all-targets';WORK=ROOT/'build/powah-all-targets'
RUNS={'1.21.1':'1.21.1-neoforge-integrated-powah-modern-01','26.1.2':'26.1.2-neoforge-integrated-powah-modern-02'}
BLOCKS=['reactor','energy_cable','ender_cell','ender_gate','energy_cell','energy_discharger','energy_hopper','furnator','magmator','player_transmitter','solar_panel','thermo_generator']
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
read=lambda p:json.loads(p.read_text())
rel=lambda p:p.relative_to(ROOT).as_posix()


def crop(row):
    im=Image.open(ROOT/row['screenshot']).convert('RGB');b=row['bounds'];p=row['composition'];l=t=0;r=b['width'];d=b['height']
    for region in p['regions']:
        if region.get('exterior'):
            l=min(l,region['x']);t=min(t,region['y']);r=max(r,region['x']+region['source']['width']);d=max(d,region['y']+region['source']['height'])
    return im.crop(tuple(2*v for v in [b['x']+l-5,b['y']+t-5,b['x']+r+5,b['y']+d+5]))


def main():
    before=read(WORK/'dist-before.json');publisher=read(OUT/'versions.json');updates={};targets=[];total=0
    for mc,name in RUNS.items():
        run=ROOT/'build/remaining-ui-audit/gameplay'/name;journal=read(run/'powah-checks/journal.json')
        assert len(journal['checks'])>=120 and all(c['passed']for c in journal['checks']),name
        stage=read(run/'staged-artifacts.json')
        filename=f'salts_inventory_compat-{mc}-NeoForge-0.1.3.jar';built=ROOT/f'compatibility/versions/{mc}/neoforge/build/libs'/filename
        tested=next(a for a in stage['artifacts']if a['filename']==filename);assert sha(built)==tested['sha256']
        with zipfile.ZipFile(built)as z:
            assert z.testzip()is None
            assert 'com/salts_inventory_update/compat/powah/PowahClient.class'in z.namelist()
            assert 'salts_inventory_compat.powah.mixins.json'in z.namelist()
            assert not any(p.startswith(('com/siu_reference/','owmii/'))for p in z.namelist())
        pixels=[]
        for block in ['magmator','thermo_generator','reactor']:
            row=next(r for r in journal['captures']if r['id']==block+'-tank-ui-filled')
            im=Image.open(ROOT/row['screenshot']).convert('RGB').resize((640,450),Image.Resampling.NEAREST)
            b=row['bounds'];body=next(r for r in row['composition']['regions']if r['id']=='native-machine')
            dx=b['x']+7+body['x'];dy=b['y']+25+body['y']
            def colored(x,y):
                r,g,b=im.getpixel((dx+x,dy+y))
                return r>g+25 and r>b+50 if block=='magmator'else b>r+20
            count=sum(colored(x,y)for x in range(157,171)for y in range(5,70));bottom=sum(colored(x,69)for x in range(157,171))
            assert count>=112 and bottom==14,(mc,block,count,bottom)
            pixels.append(dict(view=block,colored_pixels=count,bottom_colored_pixels=bottom,source=row['screenshot']))
        contact=Image.new('RGB',(1440,1040),'#eaf1f1');draw=ImageDraw.Draw(contact)
        for i,block in enumerate(BLOCKS):
            row=next(r for r in journal['captures']if r['id']==block)
            state=read(run/'powah-checks'/(block+'-client.json'))
            w=next(w for w in state['allWindows']if w.get('menuId')=='powah:'+row['menu'])
            assert not w['layoutError']and w['templateClass'].endswith('PowahClient$Template')
            im=crop(row);im.save(OUT/(mc+'-'+block+'.png'));im.thumbnail((456,215),Image.Resampling.NEAREST)
            x=i%3*480;y=i//3*260;draw.text((x+8,y+8),block+' / '+mc+' NeoForge',fill='#20373d');contact.paste(im,(x+(480-im.width)//2,y+32))
        contact.save(OUT/(mc+'-all-windows.png'))
        shutil.copy2(run/'screenshots/powah-moved-window-with-inventory.png',OUT/(mc+'-fullscreen.png'))
        shutil.copy2(run/'powah-checks/journal.json',OUT/(mc+'-journal.json'))
        latest=next(v for v in publisher if mc in v['game_versions']and'neoforge'in v['loaders'])
        total+=len(journal['checks'])
        targets.append(dict(minecraft=mc,loader='neoforge',powah=latest['version_number'],publisher_version_id=latest['id'],
            release_type=latest['version_type'],assertions=len(journal['checks']),run=rel(run),jar='dist/compatibility/'+filename,
            sha256=sha(built),fluid_pixels=pixels,contact_sheet=rel(OUT/(mc+'-all-windows.png'))))
        updates[mc+'/neoforge/'+filename]=built
    # Include the previously delivered 1.20.1 Powah jars in the combined bundle;
    # their individual release bytes remain unchanged in this follow-up.
    legacy=read(ROOT/'output/powah-implemented/verification.json')
    for target in legacy['targets']:
        path=ROOT/target['jar'];assert sha(path)==target['sha256']==before[target['jar']]
        updates['1.20.1/'+target['loader']+'/'+path.name]=path
    expected={rel(p):sha(p)for p in updates.values()}
    for path,digest in before.items():
        if path not in expected:assert sha(ROOT/path)==digest,('Unrelated artifact changed',path)
    sums={p.name:sha(p)for p in (ROOT/'dist/compatibility').glob('*.jar')};sums.update({p.name:sha(p)for p in updates.values()})
    checksums=''.join(f'{sums[name]}  {name}\n'for name in sorted(sums)).encode()
    bundle=ROOT/'dist/salts-inventory-all-latest.zip';temporary=bundle.with_suffix('.powah-tmp');checkpath='checksums/compatibility/SHA256SUMS.txt'
    with zipfile.ZipFile(bundle)as old:
        assert set(updates)<=set(old.namelist())
        untouched={info.filename:hashlib.sha256(old.read(info)).hexdigest()for info in old.infolist()if info.filename not in updates and info.filename!=checkpath}
        with zipfile.ZipFile(temporary,'w',zipfile.ZIP_DEFLATED)as z:
            for info in old.infolist():z.writestr(info,updates[info.filename].read_bytes()if info.filename in updates else checksums if info.filename==checkpath else old.read(info))
    with zipfile.ZipFile(temporary)as z:
        assert z.testzip()is None
        assert all(hashlib.sha256(z.read(name)).hexdigest()==digest for name,digest in untouched.items())
        assert all(hashlib.sha256(z.read(name)).hexdigest()==sha(path)for name,path in updates.items())
    for p in updates.values():
        dest=ROOT/'dist/compatibility'/p.name
        if p.resolve()!=dest.resolve():shutil.copy2(p,dest)
    (ROOT/'dist/compatibility/SHA256SUMS.txt').write_bytes(checksums);temporary.replace(bundle)
    rows=[]
    for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
        for loader in ['fabric','forge']if mc=='1.20.1'else['fabric','neoforge']:
            release=next((v for v in publisher if mc in v['game_versions']and loader in v['loaders']),None)
            rows.append(dict(minecraft=mc,loader=loader,available=release is not None,powah=release['version_number']if release else None,
                coverage='Prior 1.20.1 focused live checks retained'if release and mc=='1.20.1'else'Focused live checks passed'if release else'No official publisher release for this SIU target'))
    report=dict(status='pass',checked_at='2026-11-02',new_live_assertions=total,targets=targets,availability=rows,
        available_targets=4,new_jars=2,core_and_api_unchanged=True,legacy_jars_unchanged=True,
        bundle_entries_updated=list(updates),other_bundle_entries_preserved=True,bundle_sha256=sha(bundle),
        limits='Focused Powah window checks only; no complete gameplay suite, dedicated multiplayer, every tier or optional resource-pack/mod combination.')
    (OUT/'verification.json').write_text(json.dumps(report,indent=2)+'\n')
    lines=['# Powah! — SIU target coverage','','Checked November 2, 2026 against the official publisher releases.','',
        '| Minecraft | Loader | Powah | Coverage |','| --- | --- | --- | --- |']
    for row in rows:lines.append('| '+row['minecraft']+' | '+row['loader']+' | '+(row['powah']or'—')+' | '+row['coverage']+' |')
    lines+=['','All four available targets have eleven native menu types covering twelve opening routes. The 26.1.2 publisher release is alpha.','Older Powah Minecraft releases outside SIU\'s supported targets are not new Core ports.']
    (OUT/'compatibility-table.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    print(json.dumps(dict(status='pass',new_live_assertions=total,available_targets=4,new_jars=2),indent=2))


if __name__=='__main__':main()
