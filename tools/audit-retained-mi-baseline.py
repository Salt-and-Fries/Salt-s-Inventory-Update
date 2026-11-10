"""Compare retained MI feature subjects, without silently promoting old gameplay.

This is an equivalence input for explicit coverage review. Core changed; fresh
shared-path and matching publisher checks remain necessary before acceptance.
"""
from pathlib import Path
import datetime, hashlib, json, sys, zipfile

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from controlify_classfile import read_class


def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()


def compare(old, current, roots=None):
    prefix='com/salts_inventory_update/compat/'
    roots=roots or ('com/salts_inventory_update/compat/modernindustrialization/',
                    'com/salts_inventory_update/compat/nativewindows/NativeMachineLayouts')
    with zipfile.ZipFile(old) as before,zipfile.ZipFile(current) as after:
        names=set(before.namelist())
        pending=[n for n in names if n.endswith('.class') and n.startswith(roots)]
        assert pending
        checked={};external=set();changed=[]
        while pending:
            name=pending.pop()
            if name in checked: continue
            raw=before.read(name);same=name in after.namelist() and raw==after.read(name)
            checked[name]=dict(old_sha256=hashlib.sha256(raw).hexdigest(),byte_identical=same)
            if not same: changed.append(name)
            clazz=read_class(raw)
            for dependency in clazz.classes:
                if dependency.startswith(prefix) and dependency+'.class' in names:
                    pending.append(dependency+'.class')
                elif dependency.startswith('com/salts_inventory_update/'):
                    external.add(dependency)
        return dict(classes=checked,changed=changed,external_core_contracts=sorted(external))


def main():
    verification=ROOT/'output/modern-industrialization-implementation/verification.json'
    original=json.loads(verification.read_text())
    profiles=[('1.20.1','fabric','1.20.1-fabric-siu-mi-refine03','1.20.1-fabric-siu-mi-refine03'),
              ('1.21.1','neoforge','1.21.1-neoforge-integrated-mi-refine03','1.21.1-neoforge-mi-refine03')]
    rows=[]
    for mc,loader,profile,runtime in profiles:
        p=ROOT/'build/remaining-ui-audit/gameplay'/profile
        staged=p/'staged-artifacts.json';data=json.loads(staged.read_text())
        name=f'salts_inventory_compat-{mc}-'+('Fabric' if loader=='fabric' else 'NeoForge')+'-0.1.3.jar'
        old=Path('C:/Users/texca/.cache/siu-rollout-runtime')/runtime/'mods'/name
        wanted=next(a['sha256'] for a in data['artifacts'] if a['filename']==name)
        assert digest(old)==wanted,'Retained bytes no longer match the original observation'
        target=next(v for v in original['targets'] if v['minecraft']==mc and v['loader']==loader)
        assert target['sha256']==wanted
        current=ROOT/'dist/compatibility'/name
        result=compare(old,current)
        publisher=next(v for v in data['artifacts'] if v['filename'].startswith('Modern-Industrialization-'))
        native=Path('C:/Users/texca/.cache/siu-rollout-runtime')/runtime/'mods'/publisher['filename']
        assert digest(native)==publisher['sha256']
        actions=ROOT/'build/mi-window-refinement'/(profile+'-final-actions.log')
        assert 'REFINEMENT ACTIONS PASS' in actions.read_text()
        rows.append(dict(target=mc+'/'+loader,baseline_sha256=wanted,current_sha256=digest(current),
            old_archive=str(old),current_archive=str(current.relative_to(ROOT)),
            publisher=dict(filename=publisher['filename'],sha256=digest(native)),
            baseline_manifest=dict(file=str(staged.relative_to(ROOT)),sha256=digest(staged)),
            final_actions=dict(file=str(actions.relative_to(ROOT)),sha256=digest(actions)),
            scoped_geometry_cases=len(target['cases']),comparison=result))
    out=ROOT/'output/release-readiness/retained-mi-baseline-build13-01.json'
    assert not out.exists(),'Preserve previous equivalence audits'
    out.write_text(json.dumps(dict(status='comparison-complete-explicit-acceptance-pending',
        generated_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),targets=rows,
        baseline_verification=dict(file=str(verification.relative_to(ROOT)),sha256=digest(verification)),
        limits='These are retained historical scoped geometry/action baselines on exact preserved binaries, with a transitive Compatibility class comparison. Core is not byte-identical. This record is not a fresh current-artifact gameplay pass. Shared Core/API paths, matching current publisher state and the particular required feature must be qualified explicitly before accepting an equivalence disposition.'),indent=2)+'\n')
    for row in rows: print(row['target'],len(row['comparison']['classes']),'transitive Compatibility classes;',len(row['comparison']['changed']),'changed; acceptance remains explicit')


if __name__=='__main__':main()
