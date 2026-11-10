"""Publish the reviewed support pins without treating missing pins as upstream absence."""
from pathlib import Path
import csv
import collections
import hashlib
import json

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'output/all-target-rollout/availability-and-registration.json'
SUPPLEMENT=ROOT/'output/release-readiness/public-support-supplement.json'
TARGETS=[(v,l) for v in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']
         for l in ['fabric','forge' if v=='1.20.1' else 'neoforge']]
NATIVE={'dungeonz','elevatormod','sortilege','armor statues','things','wraith waystones'}
NATIVE_VERIFICATION=ROOT/'output/native-screen-preservation/verification.json'
NATIVE_CONTRACTS=ROOT/'output/release-readiness/native-only-contracts-02.json'


def mode(row):
    if row['project'].lower() in NATIVE:return 'Native screen'
    route=row['source_registration_route']
    if 'full-screen settings' in route:return 'Native settings'
    if 'plugin entry' in route:return 'Browser provider'
    if 'Inherited Create' in route:return 'Inherited Create'
    if 'Core vanilla' in route:return 'Core integration'
    assert row['source_registration'],'Unregistered advertised support pin: '+str(row)
    return 'Registered integration'


def native_only_pins():
    """Describe exact intentionally native pins without inheriting old acceptance."""
    old=json.loads(NATIVE_VERIFICATION.read_text());contracts=json.loads(NATIVE_CONTRACTS.read_text())
    assert old['status']=='passed' and contracts['status']=='native-bytecode-inspection'
    rows=[]
    for target in old['targets']:
        found=[v for v in contracts['records'] if v['loader']==target['loader'] and v['native_class'] in target['verified_screens']]
        assert found,(target['mod'],'Native class attribution is required')
        publishers={v['publisher']['sha512']:v['publisher'] for v in found};assert len(publishers)==1
        publisher=next(iter(publishers.values()));path=ROOT/publisher['path']
        assert hashlib.sha512(path.read_bytes()).hexdigest()==publisher['sha512']
        rows.append(dict(project=target['mod'],minecraft=target['minecraft'],loader=target['loader'],
            support='Intentionally native interface',upstream_version=target['upstream_version'],
            publisher_filename=publisher['filename'],upstream_sha512=publisher['sha512'],
            qualification='Final-artifact qualification pending; older navigation evidence has different SIU subject hashes',
            pin_evidence='output/release-readiness/native-only-contracts-02.json'))
    assert len(rows)==6 and len({v['project'] for v in rows})==6
    return rows


def main():
    rows=json.loads(SOURCE.read_text())
    supplemental=json.loads(SUPPLEMENT.read_text());assert supplemental['status']=='exact-local-pins-registration-only'
    for file,digest in supplemental['source_inputs'].items():assert hashlib.sha256((ROOT/file).read_bytes()).hexdigest()==digest,file
    for row in supplemental['rows']:
        path=Path(row['publisher_file']);path=path if path.is_absolute()else ROOT/path
        assert hashlib.sha512(path.read_bytes()).hexdigest()==row['upstream_sha512'],path
    rows+=supplemental['rows'];projects=sorted({r['project'] for r in rows},key=str.casefold)
    lookup=collections.defaultdict(list);unique=set()
    for row in rows:
        key=(row['project'],row['minecraft'],row['loader']);pin=(*key,row['upstream_sha512'])
        assert pin not in unique,'Duplicate exact support pin: '+str(pin);unique.add(pin)
        lookup[key].append(row)
    folder=ROOT/'docs/release';folder.mkdir(parents=True,exist_ok=True)
    csv_path=folder/'compatibility-pins.csv'
    with csv_path.open('w',newline='',encoding='utf-8') as stream:
        fields=['project','minecraft','loader','support','upstream_version','upstream_sha512','release_url',
                'publisher_filename','pin_evidence']
        writer=csv.DictWriter(stream,fieldnames=fields);writer.writeheader()
        for project in projects:
            for minecraft,loader in TARGETS:
                candidates=lookup.get((project,minecraft,loader),[])
                for row in candidates or [None]:
                    writer.writerow(dict(project=project,minecraft=minecraft,loader=loader,
                        support=mode(row) if row else 'No reviewed support pin',
                        **{key:row[key] if row else '' for key in ['upstream_version','upstream_sha512','release_url']},
                        publisher_filename=row.get('publisher_filename','')if row else '',
                        pin_evidence=row.get('integration_guide','output/all-target-rollout/availability-and-registration.json')if row else ''))
    lines=['# Reviewed compatibility pins','',
        'Release qualification is pending in [RELEASE_READINESS.md](../../RELEASE_READINESS.md). '
        'This table records registration and upstream pins; it is not a gameplay test result.','',
        'A dash means **no reviewed support pin in this inventory**, rather than a claim that the publisher '
        'has never released that target. Use the exact releases recorded in the '
        '[machine-readable table](compatibility-pins.csv); additional fixture variants are recorded in accepted runtime manifests.','',
        'Registered integrations adapt inventory/container interfaces. Other books, selectors and settings '
        'remain native where specified by the integration. Browser providers and native settings are labelled '
        'separately. Unregistered conventional containers may use Core\'s automatic fallback; that does not '
        'constitute dedicated compatibility or qualification.','',
        'Later integrations are included from exact local publisher fixtures and their integration guides. '
        'Multiple versions in one cell are distinct reviewed binary pins, not a claim that every intervening '
        'patch was tested. A link opens the exact publisher release when cached release metadata is available; '
        'otherwise it opens that publisher\'s release index. Exact hashes remain in the CSV. '
        'Fixture presence and registration do not establish final gameplay acceptance.','',
        'Required base mods, libraries, version constraints and declared side requirements are recorded in the [pinned dependency guide](UPSTREAM_DEPENDENCIES.md) and its complete CSV. These declarations do not grant gameplay acceptance.','',
        '| Mod | '+' | '.join(v+' '+('Fabric' if l=='fabric' else 'Forge' if l=='forge' else 'NeoForge') for v,l in TARGETS)+' |',
        '| --- | '+' | '.join('---' for _ in TARGETS)+' |']
    for project in projects:
        cells=[]
        for minecraft,loader in TARGETS:
            candidates=lookup.get((project,minecraft,loader),[])
            cells.append(', '.join(mode(row)+': ['+row['upstream_version'].replace('|','\\|')+']('+row['release_url']+')'for row in candidates) if candidates else '-')
        lines.append('| '+project.replace('|','\\|')+' | '+' | '.join(cells)+' |')
    lines+=['','Sources: `output/all-target-rollout/availability-and-registration.json`, SHA-256 `'+hashlib.sha256(SOURCE.read_bytes()).hexdigest()+
        '`; `output/release-readiness/public-support-supplement.json`, SHA-256 `'+hashlib.sha256(SUPPLEMENT.read_bytes()).hexdigest()+'`.','',
        'This inventory contains '+str(len(rows))+' reviewed mod/target pins across '+str(len(projects))+' projects. '
        'The wider Project Everything appendix includes mods without inventoried inventory UIs; '
        'they are not silently advertised here as additional tested window integrations.','']
    native=native_only_pins()
    with (folder/'native-screen-pins.csv').open('w',newline='',encoding='utf-8') as stream:
        writer=csv.DictWriter(stream,fieldnames=list(native[0]));writer.writeheader();writer.writerows(native)
    lines+=['## Intentionally native interfaces','',
        'These six Project Everything entries keep their original full-screen interfaces and input. '
        'They are separate from the window integrations above. Their exact retained publisher bytes are '
        'recorded in [native-screen-pins.csv](native-screen-pins.csv); dependencies remain in the linked fixture records.','',
        'Fresh final-artifact ownership/input checks are '
        '[prepared and queued](../../output/release-readiness/native-only-queued-plan-01.json). '
        'They have not passed yet. The [older native-screen guide](../../compatibility/native-screen-preservation.md) '
        'records its own subjects and limits; different candidate jar hashes do not inherit that acceptance.','',
        '| Mod | Retained native release | Recorded target |', '| --- | --- | --- |']
    for row in native:lines.append('| '+row['project']+' | '+row['upstream_version']+' | '+row['minecraft']+' '+row['loader']+' |')
    lines+=['','Native pin attribution: `output/release-readiness/native-only-contracts-02.json`, SHA-256 `'+
        hashlib.sha256(NATIVE_CONTRACTS.read_bytes()).hexdigest()+'`. '
        'The current refresh covers native screen ownership, appropriate UI input and cleanup; '
        'it does not certify unrelated dungeon generation, every teleport condition or untested releases.','']
    (folder/'compatibility-pins.md').write_text('\n'.join(lines),encoding='utf-8')
    print('Prepared reviewed support table:',len(rows),'pins;',len(projects),'projects; all 12 target columns.')


if __name__=='__main__':main()
