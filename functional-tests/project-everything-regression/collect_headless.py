"""Hash and summarize the current complete mapped JUnit result sets."""
from pathlib import Path
import hashlib,json,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/project-everything-regression'
rows=[]
for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
    for project in [f'versions/{mc}/fabric',f'compatibility/versions/{mc}/fabric']:
        files=sorted((ROOT/project/'build/test-results/test').glob('TEST-*.xml'));assert files,project
        result={'project':project,'tests':0,'failures':0,'skipped':0,'errors':0,'xml_files':[]}
        for file in files:
            suite=ET.parse(file).getroot()
            for key in ['tests','failures','skipped','errors']:result[key]+=int(suite.attrib.get(key,0))
            result['xml_files'].append({'file':file.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(file.read_bytes()).hexdigest()})
        rows.append(result)
result={'status':'passed' if all(r['tests'] and not any(r[k] for k in ['failures','skipped','errors']) for r in rows) else 'needs-attention',
        'tests':sum(r['tests'] for r in rows),'projects':rows}
(OUT/'headless-results-final.json').write_text(json.dumps(result,indent=2)+'\n')
print(result['tests'],result['status'])
