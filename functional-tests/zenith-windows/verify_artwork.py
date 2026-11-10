"""Compare complete static working panels against unchanged native captures."""
from pathlib import Path
import hashlib,json,sys
from PIL import Image,ImageChops

ROOT=Path(__file__).resolve().parents[2]
run=Path(sys.argv[1]);checks=[]
for name,reference,end in [('gem-cutting-table','zenith-gem-cutting-table',93),('salvaging-table','zenith-salvaging-table',87),('simple-reforging-table','zenith-simple-reforging-table',179)]:
    state=json.loads((run/'zenith-checks'/(name+'-empty-client.json')).read_text())
    window=next(w for w in state['allWindows']if w.get('menuId','').startswith('zenith:'))
    b=window['bounds'];path=run/'screenshots'/('zenith-'+name+'-empty.png')
    live=Image.open(path).convert('RGB').crop(((b['x']+7)*2,(b['y']+26)*2,(b['x']+183)*2,(b['y']+26+end)*2))
    original=Image.open(ROOT/'output/zenith-window-design/original'/(reference+'.png')).convert('RGB').crop((0,0,352,end*2))
    assert live.size==original.size and ImageChops.difference(live,original).getbbox()is None,name
    checks.append(dict(panel=name,pixels=live.width*live.height,exact_native_match=True,screenshot_sha256=hashlib.sha256(path.read_bytes()).hexdigest()))
result=dict(scope='Three complete static native working panels at GUI scale 2',panels=checks,pixels=sum(r['pixels']for r in checks),failures=0)
(run/'zenith-checks/artwork-verification.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2))
