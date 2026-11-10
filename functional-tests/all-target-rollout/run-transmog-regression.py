"""Wait for one disposable world, run four inventory checks, then finish it."""
from pathlib import Path
import argparse,json,subprocess,sys,time
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();deadline=time.monotonic()+300
while True:
 try:
  if json.loads((a.run/'ready.json').read_text()).get('ready'):break
 except (FileNotFoundError,json.JSONDecodeError):pass
 if time.monotonic()>deadline:raise TimeoutError('Disposable world did not become ready')
 time.sleep(1)
subprocess.run([sys.executable,'-X','utf8',str(Path(__file__).parent/'focused-transmog-physical-inventory.py'),str(a.run)],check=True)
sequence=max(json.loads((a.run/'command.json').read_text()).get('sequence',0),json.loads((a.run/'reply.json').read_text()).get('sequence',0))+1
temporary=a.run/'command.tmp';temporary.write_text(json.dumps({'sequence':sequence,'type':'finish','ticks':1}));temporary.replace(a.run/'command.json')
print('Finished four focused inventory checks',sequence,flush=True)
