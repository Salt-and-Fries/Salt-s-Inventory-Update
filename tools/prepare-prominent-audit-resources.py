"""Restore exact parent-provided resources for the isolated embedded Base audit.

The original GLOBAL initializer assumes the entire modpack. This disposable,
code-free fixture keeps two original data definitions available to Base without
editing any subject binary, UI or data definition. Base supplies its own assets.
It is not a mod release and must never enter dist/ or production source sets.
"""
import hashlib
import json
from pathlib import Path
import zipfile

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'build/remaining-ui-audit'


def main():
    fresh=json.loads((BASE/'fixtures/1.20.1-fabric-egs3lc8d.json').read_text(encoding='utf-8'))
    parent=next(a for a in fresh['artifacts'] if 'prominent' in a.get('mod_ids', []))
    original=ROOT/parent['path']
    data=original.read_bytes()
    assert hashlib.sha1(data).hexdigest()==parent['sha1']
    folder=BASE/'original-resources';folder.mkdir(parents=True,exist_ok=True)
    target=folder/'audit-original-prominent-data.jar'
    definitions={'data/prominent/prominent/lottery_data/resource_1_lottery.json',
                 'data/prominent/prominent/trade_data_list/past/herbalist.json'}
    rows=[]
    with zipfile.ZipFile(original) as source, zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as dest:
        # Parent assets also override minecraft:default with fonts depending
        # on unrelated resource packs. They are not needed by Base's UI; keep
        # this fixture strictly to the original missing data definitions.
        files=definitions
        for name in sorted(files):
            content=source.read(name)
            dest.writestr(zipfile.ZipInfo(name,(2020,1,1,0,0,0)),content,compress_type=zipfile.ZIP_DEFLATED)
            rows.append({'member':name,'sha256':hashlib.sha256(content).hexdigest()})
        meta={'schemaVersion':1,'id':'siu_ui_audit_original_resources','version':'0.0.0-qa',
              'name':'Disposable audit - original Prominent parent data',
              'description':'Unmodified parent lottery/trade definitions for isolated Base screenshot observation; no code, assets or adapters.',
              'environment':'*'}
        dest.writestr('fabric.mod.json',json.dumps(meta,sort_keys=True))
    with zipfile.ZipFile(target) as archive:
        assert not any(name.endswith('.class')for name in archive.namelist())
        assert all(hashlib.sha256(archive.read(row['member'])).hexdigest()==row['sha256'] for row in rows)
    lineage={'scope':'QA-only restoration of two original parent data definitions, byte-identical. No assets, class files, entrypoints or UI adapters.',
             'original_parent':parent['path'],'original_parent_sha256':hashlib.sha256(data).hexdigest(),
             'fixture_path':str(target.relative_to(ROOT)),'members':rows}
    (folder/'data-lineage.json').write_text(json.dumps(lineage,indent=2)+'\n',encoding='utf-8')
    fixture=json.loads((BASE/'fixtures/pr-native-base-runtime-libraries.json').read_text(encoding='utf-8'))
    raw=target.read_bytes()
    fixture['artifacts'].append({'path':str(target.relative_to(ROOT)),'filename':target.name,
        'sha1':hashlib.sha1(raw).hexdigest(),'sha512':hashlib.sha512(raw).hexdigest(),
        'mod_ids':[meta['id']],'required_mod_ids':[], 'family':'fixture-original-data',
        'lineage':str((folder/'data-lineage.json').relative_to(ROOT))})
    fixture['name']='pr-base-original-data'
    fixture['scope']='Isolated exact original embedded Base plus two byte-identical original parent data definitions. Base supplies its own UI assets. No GLOBAL initializer; no Compatibility or Controlify; no fabricated UI/data.'
    output=BASE/'fixtures/pr-base-original-data.json'
    output.write_text(json.dumps(fixture,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'fixture':str(output.relative_to(ROOT)),'artifacts':len(fixture['artifacts']),
                      'original_assets_and_definitions':len(rows),'definitions':sorted(definitions)}))


if __name__=='__main__':main()
