"""Sign off only observed native screens, preserving failed setup/gameplay probes."""
from pathlib import Path
import hashlib,json,shutil

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/native-screen-preservation'
MODELS={
    'dungeonz':('DungeonZ','1.20.1','fabric','1.0.6','net.dungeonz.'),
    'elevatormod':('ElevatorMod','1.20.1','forge','1.9.1','xyz.vsngamer.elevatorid.'),
    'sortilege':('Sortilege','1.20.1','fabric','9.0','net.lyof.sortilege.'),
    'armor-statues':('Armor Statues','1.21.1','neoforge','21.1.0','fuzs.'),
    'things':('Things','1.20.1','fabric','0.3.3+1.20','com.glisco.things.'),
    'wraith-waystones':('Wraith Waystones','1.20.1','fabric','3.3.3+mc1.20.1','wraith.fwaystones.'),
}
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()

def main():
    rows=[]
    for key,(mod,mc,loader,version,prefix)in MODELS.items():
        data=json.loads((OUT/key/'checks.json').read_text())
        assert data['status']=='passed'and all(c['passed']for c in data['checks']),key
        verified=[];unverified=[]
        for c in data['captures']:
            s=c['state'];path=ROOT/c['file']
            if path.exists()and sha(path)==c['sha256']and s['screen'].startswith(prefix)and not s.get('allWindows')and not s.get('workspaceWindows')and s.get('sessions',0)==0:verified.append(c)
            else:unverified.append({'file':c['file'],'recorded_sha256':c['sha256'],'screen':s['screen'],'reason':'Setup/non-target state, or the capture filename was superseded by a later fixture attempt. Not counted as verified native-screen evidence.'})
        screens=sorted({c['state']['screen']for c in verified})
        assert screens and verified,key
        run=ROOT/data['runtime'];staged=json.loads((run/'staged-artifacts.json').read_text())
        for r in staged['artifacts']:
            p=Path(r['file']);assert sha(p)==r['sha256'],p
        core=next(r for r in staged['artifacts']if r['filename'].startswith('salts_inventory_update-'))
        compat=next(r for r in staged['artifacts']if r['filename'].startswith('salts_inventory_compat-'))
        assert sha(ROOT/'dist/core'/core['filename'])==core['sha256']
        assert sha(ROOT/'dist/compatibility'/compat['filename'])==compat['sha256']
        row={'key':key,'mod':mod,'minecraft':mc,'loader':loader,'upstream_version':version,
             'status':'passed: native-screen preservation','runtime':data['runtime'],
             'focused_checks':len(data['checks']),'verified_screens':screens,
             'verified_captures':verified,'unverified_setup_captures':unverified,
             'core_sha256':core['sha256'],'compatibility_sha256':compat['sha256'],
             'runtime_mode':'Original publisher and shipping SIU jars; '+('named NeoForge launcher'if loader=='neoforge'else'production runtime')}
        rows.append(row)
    previous=json.loads((ROOT/'output/tech-reborn-all-targets/verification.json').read_text())
    assert sha(ROOT/'dist/salts-inventory-all-latest.zip')==previous['bundle_sha256']
    for family in ['core','compatibility']:
        for line in (ROOT/f'dist/{family}/SHA256SUMS.txt').read_text().splitlines():
            expected,name=line.split('  ');assert sha(ROOT/'dist'/family/name)==expected
    for key in ['armor-statues','things']:
        row=next(r for r in rows if r['key']==key)
        source=ROOT/row['runtime']/'server-state.json'
        shutil.copy2(source,OUT/key/'server-state-evidence.json')
    summary={'status':'passed','scope':'The six audited native-only shortlist mods. No complete functional/gameplay suite or untested version/loader coverage is claimed.',
             'focused_checks':sum(r['focused_checks']for r in rows),
             'verified_screen_types':sum(len(r['verified_screens'])for r in rows),
             'production_changes_required':False,'release_bundle_unchanged':True,
             'targets':rows,'limits':[
                 'DungeonZ Join was clicked, but the empty test-world attempt later reached its native death screen. Successful dungeon generation, party entry and completion are not certified.',
                 'Dungeon Compass selection was checked; locating/generating a real dungeon with Calibrate was outside this UI check.',
                 'Armor Statues conditional Vanilla Tweaks datapack view was not activated. Its native Screen inheritance is documented separately.',
                 'Only the pinned Minecraft/loader releases listed in targets were opened live.']}
    (OUT/'verification.json').write_text(json.dumps(summary,indent=2)+'\n')
    path=ROOT/'output/project-everything-remaining/native-preservation-checklist.json'
    checklist=json.loads(path.read_text())
    checklist['evidence_limits']=summary['scope']+' Fresh shipping-jar checks are linked per entry; conditional and gameplay limits remain explicit.'
    for entry in checklist['entries']:
        row=next(r for r in rows if r['mod']==entry['mod'])
        entry['status']='passed-current-build-native-preservation'
        entry['current_build_evidence']={k:row[k]for k in ['minecraft','loader','upstream_version','runtime','focused_checks','verified_screens','core_sha256','compatibility_sha256']}
        entry['current_build_evidence']['report']='output/native-screen-preservation/verification.json'
    path.write_text(json.dumps(checklist,indent=2)+'\n')
    print(json.dumps({k:summary[k]for k in ['status','focused_checks','verified_screen_types','production_changes_required','release_bundle_unchanged']},indent=2))

if __name__=='__main__':main()
