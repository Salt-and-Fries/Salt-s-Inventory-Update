"""Pin the official Botania NeoForge snapshot needed by Applied Botanics.

This is a publisher Maven snapshot, distinct from a Modrinth listed release.
The exact timestamped binary and publisher SHA-512 remain in the evidence.
"""
from pathlib import Path
import argparse,hashlib,json,urllib.request,xml.etree.ElementTree as ET
from datetime import datetime,timezone

ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'build/all-target-rollout'
BASE='https://maven.blamejared.com/vazkii/botania/botania-neoforge-1.21.1/457-SNAPSHOT/'
HEADERS={'User-Agent':'SIU-Compatibility-Rollout/1.0'}
def fetch(url):
    with urllib.request.urlopen(urllib.request.Request(url,headers=HEADERS),timeout=60) as response:return response.read()

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--series',default='457-SNAPSHOT');parser.add_argument('--version');parser.add_argument('--applied-botanics-baseline',action='store_true');args=parser.parse_args()
    base=BASE.replace('457-SNAPSHOT/',args.series+'/')
    metadata=fetch(base+'maven-metadata.xml');(OUT/('metadata/botania-neoforge-1.21.1-'+args.series+'-metadata.xml')).write_bytes(metadata)
    root=ET.fromstring(metadata)
    version=args.version or next(v.findtext('value') for v in root.findall('.//snapshotVersion') if v.findtext('extension')=='jar' and not v.findtext('classifier'))
    filename='botania-neoforge-1.21.1-'+version+'.jar';url=base+filename
    checksum=fetch(url+'.sha512').decode().strip().split()[0];assert len(checksum)==128
    path=OUT/'upstream'/(checksum[:16]+'-'+filename)
    if not path.exists():path.write_bytes(fetch(url))
    data=path.read_bytes();assert hashlib.sha512(data).hexdigest()==checksum
    published=datetime.strptime(root.findtext('.//lastUpdated'),'%Y%m%d%H%M%S').replace(tzinfo=timezone.utc).isoformat()
    release={'version':version,'version_id':None,'date':published,'filename':filename,
             'sha512':checksum,'sha1':hashlib.sha1(data).hexdigest(),'download':url,'release_url':base,
             'dependencies':[],'source_kind':'official-maven-snapshot',
             'coordinate':'vazkii.botania:botania-neoforge-1.21.1:'+version,
             'publisher_repository_reference':'https://github.com/VazkiiMods/Botania#Maven-info'}
    record={'project':'botania','target':'1.21.1/neoforge','release':release}
    if args.applied_botanics_baseline:
        record['dependent']='applied-botanics'
        record['pin_reference']='https://github.com/ramidzkh/Applied-Botanics/blob/1.6.0-alpha.3/gradle.properties'
        (OUT/'publisher-dependency-pins.json').write_text(json.dumps([record],indent=2))
        print('Pinned the Applied Botanics publisher dependency',version,path)
        return
    supplemental=OUT/'publisher-supplemental-inputs.json'
    records=json.loads(supplemental.read_text()) if supplemental.exists() else []
    records=[r for r in records if (r['project'],r['target'])!=(record['project'],record['target'])]+[record]
    supplemental.write_text(json.dumps(records,indent=2))
    lock=json.loads((OUT/'availability.json').read_text());lock['projects']['botania']['targets'][record['target']]=release
    (OUT/'availability.json').write_text(json.dumps(lock,indent=2))
    print('Pinned official Botania Maven snapshot',version,path)

if __name__=='__main__':main()
