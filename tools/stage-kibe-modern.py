"""Verify focused Kibe evidence, then stage only its two additional targets."""
from pathlib import Path
import hashlib,json,shutil,zipfile
from PIL import Image,ImageDraw,ImageFont

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/kibe-all-targets';WORK=ROOT/'build/kibe-all-targets'
RUNS={'fabric':'1.21.1-fabric-siu-kibe-modern-01','neoforge':'1.21.1-neoforge-integrated-kibe-modern-01'}
read=lambda p:json.loads(p.read_text(encoding='utf-8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
rel=lambda p:p.relative_to(ROOT).as_posix()

def crop(cap,state):
    im=Image.open(cap['screenshot']).convert('RGB');b=cap['bounds'];sx=im.width/state['guiWidth'];sy=im.height/state['guiHeight']
    return im.crop(tuple(round(v)for v in [(b['x']-4)*sx,(b['y']-4)*sy,(b['x']+b['width']+4)*sx,(b['y']+b['height']+4)*sy]))

def gallery(folder,journal,ids,filename,height):
    chosen=[c for c in journal['captures']if c['id']in ids];assert len(chosen)==len(ids)
    im=Image.new('RGB',(1500,((len(chosen)+3)//4)*height),'#eaf1f1');draw=ImageDraw.Draw(im)
    font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',17)
    for i,c in enumerate(chosen):
        tile=crop(c,read(folder/(c['id']+'-client.json')));tile.thumbnail((355,height-48),Image.Resampling.NEAREST)
        x=i%4*375;y=i//4*height;im.paste(tile,(x+(375-tile.width)//2,y+38))
        label=c['id'].removeprefix('pr-').removeprefix('item-').removeprefix('kibe-').replace('_',' ').replace('-',' ')
        draw.text((x+10,y+10),label,font=font,fill='#20393b')
    path=folder/filename;im.save(path);return dict(path=rel(path),sha256=sha(path))

def main():
    before=read(WORK/'dist-before.json');releases=read(OUT/'releases.json');design=read(ROOT/'output/kibe-window-design/manifest.json')
    art=read(OUT/'artwork-verification.json');assert art['differing_pixels']==0 and art['compared_pixels']==80128
    ids=[c['id']for c in design['cases']];updates={};targets=[]
    for loader,run in RUNS.items():
        folder=OUT/run;runpath=ROOT/'build/remaining-ui-audit/gameplay'/run;j=read(folder/('journal-'+run+'.json'))
        assert len(j['checks'])==184 and all(c['passed']for c in j['checks']),run
        label='Fabric'if loader=='fabric'else'NeoForge';name=f'salts_inventory_compat-1.21.1-{label}-0.1.3.jar'
        built=ROOT/'compatibility/versions/1.21.1'/loader/'build/libs'/name;staged=read(runpath/'staged-artifacts.json')
        for a in staged['artifacts']:assert sha(Path(a['file']))==a['sha256'],a['file']
        tested=next(a for a in staged['artifacts']if a['filename']==name);assert sha(built)==tested['sha256']
        with zipfile.ZipFile(built)as z:
            assert z.testzip()is None
            assert 'com/salts_inventory_update/compat/kibe/KibeClient.class'in z.namelist()
            assert 'salts_inventory_compat.kibe.mixins.json'in z.namelist()
            assert not any(n.startswith(('com/siu_reference/','io/github/lucaargolo/','com/salts_inventory_update/api/'))for n in z.namelist())
        captures=[]
        for c in j['captures']:
            p=Path(c['screenshot']);captures.append(dict(**c,sha256=sha(p)))
        contact=gallery(folder,j,ids,'all-windows.png',245)
        active=gallery(folder,j,['vacuum-xp-conversion','entangled-colored-runes','entangled-bag-linked-colored','cobblestone_generator_mk1-working','basalt_generator_mk1-working','big-torch-supplied','drawbridge-covered','wither-builder-supplied'],'active-windows.png',300)
        v=next(v for v in releases if '1.21.1'in v['game_versions']and loader in v['loaders'])
        targets.append(dict(minecraft='1.21.1',loader=loader,kibe=v['version_number'],publisher_version=v['id'],focused_assertions=184,
            custom_views=22,screen_types=13,native_slot_envelopes=375,jar='dist/compatibility/'+name,sha256=sha(built),
            journal=rel(folder/('journal-'+run+'.json')),staged_artifacts=rel(runpath/'staged-artifacts.json'),gallery=contact,active_gallery=active,captures=captures))
        updates['1.21.1/'+loader+'/'+name]=built
    expected={'dist/compatibility/'+p.name for p in updates.values()}
    for path,digest in before.items():
        if path not in expected:assert sha(ROOT/path)==digest,('Unrelated release changed',path)
    sums={p.name:sha(p)for p in (ROOT/'dist/compatibility').glob('*.jar')};sums.update({p.name:sha(p)for p in updates.values()})
    checksum=''.join(f'{sums[name]}  {name}\n'for name in sorted(sums)).encode('utf-8')
    bundle=ROOT/'dist/salts-inventory-all-latest.zip';temporary=bundle.with_suffix('.kibe-ports.tmp.zip');checkpath='checksums/compatibility/SHA256SUMS.txt'
    with zipfile.ZipFile(bundle)as old:
        untouched={n:hashlib.sha256(old.read(n)).hexdigest()for n in old.namelist()if n not in updates and n!=checkpath}
        assert set(updates)<=set(old.namelist())
        with zipfile.ZipFile(temporary,'w')as z:
            z.comment=old.comment
            for info in old.infolist():z.writestr(info,updates[info.filename].read_bytes()if info.filename in updates else checksum if info.filename==checkpath else old.read(info))
    with zipfile.ZipFile(temporary)as z:
        assert z.testzip()is None
        assert all(hashlib.sha256(z.read(n)).hexdigest()==digest for n,digest in untouched.items())
        assert all(z.read(n)==p.read_bytes()for n,p in updates.items())
    for p in updates.values():shutil.copy2(p,ROOT/'dist/compatibility'/p.name)
    (ROOT/'dist/compatibility/SHA256SUMS.txt').write_bytes(checksum);temporary.replace(bundle)
    rows=[]
    for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
        for loader in ['fabric','forge']if mc=='1.20.1'else['fabric','neoforge']:
            v=next((v for v in releases if mc in v['game_versions']and loader in v['loaders']),None)
            rows.append(dict(minecraft=mc,loader=loader,release=v['version_number']if v else None,available=v is not None,
                coverage='Prior 184 focused checks retained'if v and mc=='1.20.1'else'184 focused live checks passed'if v else'No official Kibe release for this SIU target'))
    baseline=read(ROOT/'output/kibe-implemented/verification.json');assert baseline['distribution']['sha256']==before[baseline['distribution']['jar'].replace('\\','/')]
    table=['# Kibe - SIU target coverage','','Checked November 2, 2026 against the official publisher releases.','',
        '| Minecraft | Loader | Kibe release | Coverage |','| --- | --- | --- | --- |']
    table+=['| '+r['minecraft']+' | '+r['loader']+' | '+(r['release']or'—')+' | '+r['coverage']+' |'for r in rows]
    table+=['','All three available SIU targets support the thirteen custom container types and all ten generator views. Pocket Crafting uses existing vanilla support; Chunk Loader stays native.',
        '', 'Older Kibe releases outside SIU\'s supported Minecraft targets do not imply additional Core ports. The permissive Minecraft dependency range in a jar does not establish a release for later game APIs.',
        '', 'Sources: [Modrinth versions](https://modrinth.com/mod/kibe/versions), [CurseForge files](https://www.curseforge.com/minecraft/mc-mods/kibe/files/all).']
    (OUT/'compatibility-table.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
    report=dict(status='pass',checked_at='2026-11-02',available_targets=3,new_jars=2,new_focused_assertions=368,retained_baseline_assertions=184,
        targets=targets,availability=rows,baseline_evidence='output/kibe-implemented/verification.json',core_and_api_unchanged=True,
        unrelated_release_jars_unchanged=True,other_bundle_entries_preserved=True,bundle_entries_updated=list(updates)+[checkpath],bundle_sha256=sha(bundle),
        artwork='output/kibe-all-targets/artwork-verification.json',native_reference='output/kibe-all-targets/1.21.1-fabric-native-kibe-reference-01/native-captures.json',
        build_log='build/kibe-all-targets/build.log',
        limits='Focused Kibe window checks only; no complete gameplay suite. Dedicated multiplayer, every world automation/redstone configuration, all timing profiles, resource packs, controller input and optional mod combinations are not exhaustive.',
        fixture_corrections=['Modern item-entity stacks require lowercase count. The old Count field spawned one item; the corrected native fixture spawned and collected three.',
                             'Repeated same-block fixtures preserve their real inventory. The XP rerun explicitly removes the prior machine before creating a clean fixture.'])
    (OUT/'verification.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(dict(status='pass',new_focused_assertions=368,available_targets=3,new_jars=2),indent=2))
if __name__=='__main__':main()
