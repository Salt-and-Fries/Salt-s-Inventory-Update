"""Build class-scoped Core/AE2 candidates; do not write the shipping distribution."""
from pathlib import Path
import hashlib, io, json, zipfile

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/ae2-click-investigation'
SHA=lambda b:hashlib.sha256(b).hexdigest()
ADDED=['com/salts_inventory_update/server/PlayerMenuClickSnapshots.class',
       'com/salts_inventory_update/server/PlayerMenuClickSnapshots$Snapshot.class']


def main():
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--run',required=True);a=p.parse_args()
    folder=ROOT/'build/ae2-click-investigation'/a.run;assert not folder.exists();folder.mkdir(parents=True)
    before={p.relative_to(ROOT).as_posix():SHA(p.read_bytes())for family in ['core','compatibility']for p in (ROOT/'dist'/family).glob('*.jar')}
    assert len(before)==24
    records=[]
    for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
        for loader in ['Fabric','Forge'if mc=='1.20.1'else'NeoForge']:
            filename=f'salts_inventory_update-{mc}-{loader}-0.1.6.jar'
            source=ROOT/'dist/core'/filename;built=ROOT/f'versions/{mc}/{loader.lower()}/build/libs'/filename
            changed=['com/salts_inventory_update/server/DesktopContainerSessions.class','com/salts_inventory_update/server/DesktopContainerSessions$PlayerSessions.class']
            if mc=='1.20.1':changed+=['com/salts_inventory_update/server/DesktopContainerSessions$MutationRegistration.class']
            classes=changed+ADDED
            buffer=io.BytesIO()
            with zipfile.ZipFile(source)as old,zipfile.ZipFile(built)as new,zipfile.ZipFile(buffer,'w')as candidate:
                assert all(n not in old.namelist()for n in ADDED)
                assert all(old.read(n)!=new.read(n)for n in changed)
                candidate.comment=old.comment
                for info in old.infolist():candidate.writestr(info,new.read(info.filename)if info.filename in changed else old.read(info))
                for name in sorted(ADDED):candidate.writestr(new.getinfo(name),new.read(name))
            data=buffer.getvalue()
            with zipfile.ZipFile(io.BytesIO(data))as candidate,zipfile.ZipFile(source)as old:
                assert candidate.testzip()is None
                assert set(candidate.namelist())==set(old.namelist())|set(ADDED)
                differences=[n for n in old.namelist()if candidate.read(n)!=old.read(n)]
                assert set(differences)==set(changed)
                api=[n for n in old.namelist()if n.startswith('com/salts_inventory_update/api/')]
                assert all(candidate.read(n)==old.read(n)for n in api)
                assert not any('UiClickProbe'in n or '/functionaltest/'in n for n in candidate.namelist())
            destination=folder/filename;destination.write_bytes(data)
            records.append({'minecraft':mc,'loader':loader.lower(),'file':str(destination.relative_to(ROOT)),
                'family':'core',
                'sha256':SHA(data),'shipping_sha256':before[source.relative_to(ROOT).as_posix()],
                'changed_entries':changed,'added_entries':ADDED,'public_api_entries_preserved':len(api)})
    for mc,loader in [('1.20.1','Fabric'),('1.20.1','Forge'),('1.21.1','NeoForge'),('26.1.2','NeoForge'),('26.2','NeoForge'),('26.3','NeoForge')]:
        filename=f'salts_inventory_compat-{mc}-{loader}-0.1.3.jar'
        source=ROOT/'dist/compatibility'/filename
        built=ROOT/f'compatibility/versions/{mc}/{loader.lower()}/build/libs'/filename
        changed=['com/salts_inventory_update/compat/ae2/server/AE2ServerIntegration.class']
        buffer=io.BytesIO()
        with zipfile.ZipFile(source)as old,zipfile.ZipFile(built)as new,zipfile.ZipFile(buffer,'w')as candidate:
            assert all(old.read(n)!=new.read(n)for n in changed)
            candidate.comment=old.comment
            for info in old.infolist():candidate.writestr(info,new.read(info.filename)if info.filename in changed else old.read(info))
        data=buffer.getvalue()
        with zipfile.ZipFile(io.BytesIO(data))as candidate,zipfile.ZipFile(source)as old:
            assert candidate.testzip()is None and set(candidate.namelist())==set(old.namelist())
            assert {n for n in old.namelist()if candidate.read(n)!=old.read(n)}==set(changed)
            api=[n for n in old.namelist()if '/api/'in n]
            assert all(candidate.read(n)==old.read(n)for n in api)
            assert not any('UiClickProbe'in n or '/functionaltest/'in n for n in candidate.namelist())
        destination=folder/filename;destination.write_bytes(data)
        records.append({'family':'compatibility','minecraft':mc,'loader':loader.lower(),
            'file':str(destination.relative_to(ROOT)),'sha256':SHA(data),
            'shipping_sha256':before[source.relative_to(ROOT).as_posix()],
            'changed_entries':changed,'added_entries':[],'public_api_entries_preserved':len(api)})
    report={'scope':'Class-scoped test candidates only. Shipping jars/checksums/bundle unchanged.','baseline':before,'candidates':records}
    (OUT/(a.run+'.json')).write_text(json.dumps(report,indent=2)+'\n')
    print('Prepared 12 Core and 6 AE2 compatibility candidates; shipping/API/publisher bytes preserved.')


if __name__=='__main__':main()
