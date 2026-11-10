"""Stage the 24 verified local rollout JARs, preserving the Controlify companion."""
from pathlib import Path
import hashlib,json,shutil,zipfile
ROOT=Path(__file__).resolve().parents[1];WORK=ROOT/'build/all-target-rollout';DIST=ROOT/'dist'
digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
report=json.loads((WORK/'artifact-integrity.json').read_text())
assert report['failures']==0 and len(report['artifacts'])==24
artifacts=report['artifacts'];replacements={};baseline=WORK/'dist-before-rollout';baseline.mkdir(exist_ok=True)
controlify={p.name:digest(p) for p in (DIST/'controlify').iterdir() if p.is_file()}
for row in artifacts:
 source=Path(row['file']);target=DIST/row['family']/source.name
 assert row['status']=='pass' and digest(source)==row['sha256'] and target.is_file()
 replacements[f"{row['minecraft']}/{row['loader']}/{source.name}"]=source
checksums={family:''.join(f"{row['sha256']}  {Path(row['file']).name}\n" for row in sorted(artifacts,key=lambda r:Path(r['file']).name) if row['family']==family).encode() for family in ['core','compatibility']}
bundle=DIST/'salts-inventory-all-latest.zip';temporary=bundle.with_suffix('.tmp')
with zipfile.ZipFile(bundle) as old:
 entries=old.infolist();assert len(entries)==62
 assert set(replacements).issubset(old.namelist())
 untouched={info.filename:hashlib.sha256(old.read(info)).hexdigest() for info in entries if info.filename not in replacements and info.filename not in ['checksums/core/SHA256SUMS.txt','checksums/compatibility/SHA256SUMS.txt']}
 if not (baseline/bundle.name).exists():shutil.copy2(bundle,baseline/bundle.name)
 with zipfile.ZipFile(temporary,'w',zipfile.ZIP_DEFLATED) as new:
  for info in entries:
   if info.filename in replacements:data=replacements[info.filename].read_bytes()
   elif info.filename in ['checksums/core/SHA256SUMS.txt','checksums/compatibility/SHA256SUMS.txt']:data=checksums[info.filename.split('/')[1]]
   else:data=old.read(info)
   new.writestr(info,data)
with zipfile.ZipFile(temporary) as archive:
 assert archive.testzip() is None
 assert len(archive.infolist())==62
 for name,expected in untouched.items():assert hashlib.sha256(archive.read(name)).hexdigest()==expected
 for name,source in replacements.items():assert hashlib.sha256(archive.read(name)).hexdigest()==digest(source)
for row in artifacts:
 target=DIST/row['family']/Path(row['file']).name
 backup=baseline/row['family']/target.name;backup.parent.mkdir(exist_ok=True)
 if not backup.exists():shutil.copy2(target,backup)
 shutil.copy2(row['file'],target);assert digest(target)==row['sha256']
for family,data in checksums.items():(DIST/family/'SHA256SUMS.txt').write_bytes(data)
temporary.replace(bundle)
assert controlify=={p.name:digest(p) for p in (DIST/'controlify').iterdir() if p.is_file()}
(WORK/'dist-staging-verification.json').write_text(json.dumps({'scope':'Local release staging and exact-byte checks only; runtime UI coverage remains in its separate ledger','updated_artifacts':24,'bundle_entries':62,'controlify_unchanged':True,'bundle_sha256':digest(bundle),'artifacts':artifacts},indent=2))
print('Staged 24 verified Core/Compatibility JARs and the local bundle; Controlify unchanged.')
