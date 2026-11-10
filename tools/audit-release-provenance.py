"""Check packaged notices against actual local source/assets and recorded origins."""
from pathlib import Path
from io import BytesIO
from zipfile import ZipFile
import argparse,hashlib,json
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
def sha(raw):return hashlib.sha256(raw).hexdigest()

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--id',required=True);a=p.parse_args()
    output=ROOT/'output/release-readiness'/('provenance-'+a.id+'.json');assert not output.exists(),'Preserve prior evidence'
    integrity=json.loads((ROOT/'output/release-readiness/candidate-integrity.json').read_text());assert integrity['status']=='passed'
    creative=ROOT/'artwork/shared-resources/core/assets/salts_inventory_update/textures/gui/creative_buttons.png'
    apache=(ROOT/'artwork/shared-resources/core/LICENSE-creative-buttons-fabric').read_bytes()
    origin=Path.home()/'.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-item-group-api-v1/4.0.14+1802ada577'
    publisher=next(v for v in sorted(origin.rglob('*.jar')) if not v.name.endswith('-sources.jar'))
    with ZipFile(publisher) as jar:
        assert jar.read('LICENSE-fabric-item-group-api-v1').decode().splitlines()==apache.decode().splitlines()
        source=jar.read('assets/fabric/textures/gui/creative_buttons.png')
        assert Image.open(BytesIO(source)).convert('RGBA').tobytes()==Image.open(creative).convert('RGBA').tobytes()
    cpu_path=ROOT/'compatibility/ae2/versions/1.20.1/common/src/main/resources/ae2-legacy-cpu-art.json'
    cpu=json.loads(cpu_path.read_text());ae2=ROOT/'build/ae2-port/appliedenergistics2-26.3.1-alpha.jar'
    assert sha(ae2.read_bytes())==cpu['sourceJarSha256']
    with ZipFile(ae2) as jar:
        for row in cpu['regions'].values():
            image=Image.open(BytesIO(jar.read(row['resource']))).convert('RGBA');x,y,w,h=row['sourceRect']
            assert sha(image.crop((x,y,x+w,y+h)).tobytes())==row['rgbaSha256']
    sounds=ROOT/'controlify/versions/26.3/common/src/main/resources'
    prefix='assets/salts_inventory_controlify/licenses/ui-sounds/'
    palette=json.loads((sounds/(prefix+'palette-manifest.json')).read_text(encoding='utf-8-sig'))
    rows=[];mit=(ROOT/'LICENSE').read_bytes();lgpl=(ROOT/'compatibility/ae2/src/main/resources/META-INF/LICENSE_ae2_desktop').read_bytes()
    ae2_notice=(ROOT/'compatibility/ae2/src/main/resources/META-INF/NOTICE_ae2_desktop.txt').read_bytes()
    for subject in integrity['artifacts']:
        file=ROOT/subject['file'];assert sha(file.read_bytes())==subject['sha256']
        checks=['root MIT notice exact']
        with ZipFile(file) as jar:
            assert jar.read('LICENSE_salts_inventory_update')==mit
            if subject['family']=='core':
                assert jar.read('LICENSE-creative-buttons-fabric')==apache
                assert jar.read('assets/salts_inventory_update/textures/gui/creative_buttons.png')==creative.read_bytes()
                checks+=['Fabric paging artwork and Apache-2.0 license exact']
            if subject['family']=='compatibility':
                assert jar.read('META-INF/LICENSE_ae2_desktop')==lgpl and jar.read('META-INF/NOTICE_ae2_desktop.txt')==ae2_notice
                if subject['minecraft']=='1.20.1':assert jar.read('ae2-legacy-cpu-art.json')==cpu_path.read_bytes()
                checks+=['adapted AE2 LGPL-3.0-or-later license/source notice exact','legacy CPU imported pixel provenance checked']
            if prefix+'palette-manifest.json' in jar.namelist():
                for name in ['ATTRIBUTION.md','kenney-interface-sounds-License.txt','kenney-ui-audio-License.txt','palette-manifest.json']:
                    assert jar.read(prefix+name)==(sounds/(prefix+name)).read_bytes()
                for asset in palette['assets']:assert sha(jar.read(asset['derived_resource']))==asset['derived_sha256']
                assert all(not v['redistributed_audio'] for v in palette['event_aliases'])
                assert len(palette['assets'])==7 and palette['license']=='CC0-1.0'
                checks+=['seven CC0 recordings match source manifest','original Kenney notices exact','Minecraft event aliases distinguished from redistributed recordings']
        rows.append(dict(file=subject['file'],sha256=subject['sha256'],checks=checks))
    result=dict(status='passed',scope='Packaged notice/asset provenance checks; no publication or legal opinion.',artifacts=rows,
        fabric_artwork=dict(publisher_file=str(publisher),publisher_sha256=sha(publisher.read_bytes()),resource='assets/fabric/textures/gui/creative_buttons.png',source_rgba_equal=True),
        ae2_legacy_cpu=dict(source_jar=str(ae2),source_sha256=cpu['sourceJarSha256'],regions=len(cpu['regions']),license=cpu['license']),
        cc0_audio=dict(manifest=str((sounds/(prefix+'palette-manifest.json')).relative_to(ROOT)),manifest_sha256=sha((sounds/(prefix+'palette-manifest.json')).read_bytes()),recordings=7),
        source_offer='Corresponding adapted source remains in compatibility/ae2, with build helpers included in the final candidate source snapshot. Source publication remains a separate user-authorized action.')
    output.write_text(json.dumps(result,indent=2)+'\n');print('Verified provenance/notices for',len(rows),'current archives;',output)

if __name__=='__main__':main()
