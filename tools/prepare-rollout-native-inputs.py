"""Hash-verified upstream binaries for exact release contract inspection.

Caches original publisher JAR bytes only. Never installs them into a live game.
"""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor,as_completed
import json,hashlib,urllib.request,shutil,os,time,zipfile
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/all-target-rollout'
DEST=OUT/'upstream';DEST.mkdir(exist_ok=True)

def sha(path):
    h=hashlib.sha512()
    with path.open('rb') as f:
        for chunk in iter(lambda:f.read(1024*1024),b''):h.update(chunk)
    return h.hexdigest()

def main():
    data=json.loads((OUT/'availability.json').read_text());wanted={}
    for p in data['projects'].values():
        for r in p['targets'].values():wanted[r['sha512']]=r
    names={r['filename'] for r in wanted.values()};existing={}
    for base in ['build','output']:
        for folder,dirs,files in os.walk(ROOT/base):
            if Path(folder)==DEST:dirs.clear();continue
            for name in names.intersection(files):existing.setdefault(name,[]).append(Path(folder)/name)
    def get(item):
        digest,r=item;dest=DEST/(digest[:16]+'-'+r['filename']);origin='cache'
        if not dest.exists():
            source=next((p for p in existing.get(r['filename'],[]) if sha(p)==digest),None)
            if source:shutil.copyfile(source,dest);origin='existing-verified'
            else:
                origin='publisher-download';temp=dest.with_suffix('.partial')
                for attempt in range(4):
                    try:
                        with urllib.request.urlopen(urllib.request.Request(r['download'],headers={'User-Agent':'SIU-Compatibility-Audit/1.0'}),timeout=60) as response,temp.open('wb') as f:shutil.copyfileobj(response,f)
                        if sha(temp)!=digest:raise ValueError('Publisher digest mismatch: '+r['filename'])
                        temp.replace(dest);break
                    except Exception:
                        if attempt==3:raise
                        time.sleep(attempt+1)
        if sha(dest)!=digest:raise ValueError('Cached input mismatch: '+str(dest))
        with zipfile.ZipFile(dest) as z:
            entries=z.namelist()
            mods=json.loads(z.read('fabric.mod.json'),strict=False) if 'fabric.mod.json' in entries else None
            inventory={'file':str(dest.relative_to(ROOT)),'sha512':digest,'size':dest.stat().st_size,'origin':origin,
                'fabric_mod':mods,'classes':[n for n in entries if n.endswith('.class')],
                'ui_assets':[n for n in entries if n.startswith('assets/') and any(k in n for k in ['/gui/','/screens/'])]}
        return digest,inventory
    results={};errors={}
    with ThreadPoolExecutor(max_workers=4) as pool:
        pending={pool.submit(get,x):x for x in wanted.items()}
        for i,f in enumerate(as_completed(pending),1):
            digest,r=pending[f]
            try:key,row=f.result();results[key]=row;print(i,'/',len(wanted),r['filename'],row['origin'],flush=True)
            except Exception as e:errors[digest]={'filename':r['filename'],'error':str(e)};print('ERROR',r['filename'],e,flush=True)
            if i%10==0:(OUT/'upstream-inputs-progress.json').write_text(json.dumps({'inputs':results,'errors':errors},indent=2))
    (OUT/'upstream-inputs.json').write_text(json.dumps({'inputs':results,'errors':errors},indent=2))
    print('Finished',len(results),'inputs;',len(errors),'errors',flush=True)

if __name__=='__main__':main()
