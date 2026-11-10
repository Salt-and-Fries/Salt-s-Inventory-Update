"""Verify the complete normal-build candidate; retain the old scoped-history audit."""
from pathlib import Path
import hashlib,json,zipfile
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/project-everything-regression'
digest=lambda data:hashlib.sha256(data).hexdigest()

def historical_main():
    before=json.loads((OUT/'release-before.json').read_text())
    stages=[json.loads((OUT/name).read_text()) for name in ['staged-fixes.json','staged-sort-fix.json','staged-toms-sidebar-fix.json','staged-header-slot-fix.json','staged-provider-bridge-fix.json','staged-provider-sessions-fix.json','staged-provider-fallback-fix.json','staged-aether-launcher-fix.json','staged-opening-priority-fix.json','staged-native-writer-provenance-fix.json','staged-registered-native-writer-fix.json','staged-native-selector-fix.json','staged-scoped-target-fix.json'] if (OUT/name).exists()]
    refmap_stage=OUT/'staged-scoped-target-refmap-fix.json'
    if refmap_stage.exists():stages.append(json.loads(refmap_stage.read_text()))
    geckolib_stage=OUT/'staged-geckolib-absent-ids-fix.json'
    if geckolib_stage.exists():stages.append(json.loads(geckolib_stage.read_text()))
    equipment_stage=OUT/'staged-curios-accessories-swap.json'
    if equipment_stage.exists():stages.append(json.loads(equipment_stage.read_text()))
    portrait_stage=OUT/'staged-accessories-portrait-inset.json'
    if portrait_stage.exists():stages.append(json.loads(portrait_stage.read_text()))
    expected={};additions={}
    for stage in stages:
        for row in stage['artifacts']:
            expected.setdefault(row['file'],set()).update(row['changed_entries'])
            additions.setdefault(row['file'],set()).update(row.get('added_entries',[]))
    rows=[]
    with zipfile.ZipFile(ROOT/'dist/salts-inventory-all-latest.zip') as bundle:
        assert bundle.testzip() is None
        for name,oldhash in before.items():
            if not name.endswith('.jar'):continue
            current=ROOT/name;old=next((p for folder in ['release-backup','release-before-sort','release-before-toms-sidebar','release-before-header-slot','release-before-provider-bridge','release-before-provider-sessions']
                if (p:=ROOT/'build/project-everything-regression'/folder/current.name).exists() and digest(p.read_bytes())==oldhash),None)
            assert old is not None,'Missing preserved original: '+name
            with zipfile.ZipFile(old) as a,zipfile.ZipFile(current) as b:
                assert b.testzip() is None and set(b.namelist())==set(a.namelist())|additions.get(name,set())
                assert not any('/api/' in n for n in additions.get(name,set()))
                changed={n for n in a.namelist() if a.read(n)!=b.read(n)}
                assert changed==expected[name],(name,changed,expected[name])
                api=[n for n in a.namelist() if '/api/' in n]
                assert all(a.read(n)==b.read(n) for n in api)
                assert not any('/functionaltest/' in n or n.startswith('com/siu_reference/') for n in b.namelist())
            module,mc,loader,version=current.stem.split('-')
            assert bundle.read(f'{mc}/{loader.lower()}/{current.name}')==current.read_bytes()
            rows.append({'file':name,'sha256':digest(current.read_bytes()),'changed_entries':sorted(changed),'added_entries':sorted(additions.get(name,set())),'preserved_api_entries':len(api)})
        for family in ['core','compatibility']:
            sums=ROOT/'dist'/family/'SHA256SUMS.txt'
            assert bundle.read(f'checksums/{family}/SHA256SUMS.txt')==sums.read_bytes()
            for line in sums.read_text().splitlines():
                expected_hash,filename=line.split(None,1)
                assert digest((sums.parent/filename.strip()).read_bytes())==expected_hash
    result={'status':'passed','artifacts':rows,'public_api_byte_identical':True,'qa_classes_excluded':True,'checksums_and_bundle_consistent':True}
    (OUT/'release-integrity-final.json').write_text(json.dumps(result,indent=2)+'\n')
    print('Verified',len(rows),'release jars, public API bytes, test-code exclusion, checksums and bundle.')

def main():
    import argparse, datetime, importlib.util
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--historical-scoped',action='store_true',help='Old exact-entry delta audit; not a normal-build verifier')
    parser.add_argument('--baseline',type=Path,default=ROOT/'build/release-readiness/initial-dist')
    parser.add_argument('--output',type=Path,default=ROOT/'output/release-readiness/candidate-integrity.json')
    args=parser.parse_args()
    if args.historical_scoped:return historical_main()
    targets=json.loads((ROOT/'controlify/targets.json').read_text())['targets']
    assert len(targets)==12
    versions={'core':('salts_inventory_update','0.1.6'),
              'compatibility':('salts_inventory_compat','0.1.3'),
              'controlify':('salts_inventory_controlify','0.1.1')}
    rows=[];expected_bundle=set();license_inventory={}
    with zipfile.ZipFile(ROOT/'dist/salts-inventory-all-latest.zip') as bundle:
        assert bundle.testzip() is None and len(bundle.namelist())==len(set(bundle.namelist()))
        for family,(module,version) in versions.items():
            folder=ROOT/'dist'/family
            jars=sorted(folder.glob('*.jar'));assert len(jars)==12,(family,len(jars))
            sums=folder/'SHA256SUMS.txt';checksum_rows={}
            for line in sums.read_text().splitlines():
                value,name=line.split(None,1);assert name.strip() not in checksum_rows
                checksum_rows[name.strip()]=value
            assert set(checksum_rows)=={p.name for p in jars}
            checksum_entry=f'checksums/{family}/SHA256SUMS.txt';expected_bundle.add(checksum_entry)
            assert bundle.read(checksum_entry)==sums.read_bytes()
            for target in targets:
                mc,loader=target['minecraft'],target['loader']
                label={'fabric':'Fabric','forge':'Forge','neoforge':'NeoForge'}[loader]
                p=folder/f'{module}-{mc}-{label}-{version}.jar';raw=p.read_bytes();sha=digest(raw)
                assert checksum_rows[p.name]==sha
                entry=f'{mc}/{loader}/{p.name}';expected_bundle.add(entry)
                assert bundle.read(entry)==raw
                with zipfile.ZipFile(p) as z:
                    names=z.namelist();assert z.testzip() is None and len(names)==len(set(names)),p
                    assert not any(n.startswith(('com/siu_reference/','example/')) or '/functionaltest/' in n
                        or 'UiClickProbe' in n or 'native-ui-reference' in n or 'salts_gameplay_qa' in n
                        or n.endswith(('.java','.pdn')) for n in names),p
                    assert not any(n.startswith(('appeng/','com/tom/storagemod/','net/p3pp3rf1y/',
                        'top/theillusivec4/curios/','io/wispforest/accessories/')) and n.endswith('.class') for n in names),p
                    metadata=z.read('fabric.mod.json' if loader=='fabric' else
                        'META-INF/mods.toml' if loader=='forge' else 'META-INF/neoforge.mods.toml').decode()
                    assert module in metadata and version in metadata and mc in metadata,p
                    licenses=sorted(n for n in names if 'license' in n.lower() or 'notice' in n.lower())
                    assert licenses,p
                    license_inventory[str(p.relative_to(ROOT))]=licenses
                    if family=='compatibility':
                        assert not any(n.startswith('com/salts_inventory_update/api/') and n.endswith('.class') for n in names),p
                        reference=json.loads(z.read('META-INF/ae2-desktop-reference.json'))
                        assert len(reference['styles'])==42
                        assert 'META-INF/LICENSE_ae2_desktop' in names and 'META-INF/NOTICE_ae2_desktop.txt' in names
                    rows.append(dict(family=family,minecraft=mc,loader=loader,file=str(p.relative_to(ROOT)),sha256=sha,
                                     bundle_entry=entry,license_files=licenses))
        expected_bundle.add('CONTROLIFY-README.md')
        assert {n for n in bundle.namelist() if not n.endswith('/')}==expected_bundle,'Unexpected/missing bundle file'
    spec=importlib.util.spec_from_file_location('release_abi',ROOT/'tools/verify-public-api-compatibility.py')
    abi=importlib.util.module_from_spec(spec);spec.loader.exec_module(abi)
    errors=[]
    for row in rows:
        p=ROOT/row['file'];old=args.baseline/p.relative_to(ROOT/'dist')
        with zipfile.ZipFile(old) as a,zipfile.ZipFile(p) as b:
            for name in a.namelist():
                if not name.startswith(abi.PREFIX) or not name.endswith('.class'):continue
                declaration=abi.declarations(a.read(name))
                if not declaration['access']&1:continue
                if name not in b.namelist():errors.append(p.name+': missing API '+name);continue
                errors+=abi.compare(declaration,abi.declarations(b.read(name)),p.name+'!'+name)
    result=dict(status='failed' if errors else 'passed',generated_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        artifacts=rows,errors=errors,public_api_binary_compatible=not errors,
        public_api_byte_identical=False,api_change='Additive built-in subview and control-policy contracts; existing signatures preserved.',
        qa_classes_excluded=True,checksums_and_bundle_consistent=True,
        qualification='Artifact integrity only. Runtime qualification and independent review remain separate gates.',
        baseline=str(args.baseline),superseded_scoped_stages=sorted(str(p.relative_to(ROOT)) for p in OUT.glob('staged-*.json')),
        ae2_click_fix_evidence='output/ae2-click-investigation/passive-source-fix.json')
    args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(result,indent=2)+'\n')
    assert not errors,'\n'.join(errors)
    print('Verified all 36 normal-build jars, existing public ABI, QA exclusion, licenses, checksums and combined archive.')


if __name__=='__main__':main()
