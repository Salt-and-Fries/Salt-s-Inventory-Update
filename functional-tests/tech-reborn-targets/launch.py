"""Keep the production test process supervised and record its exit status."""
from pathlib import Path
import argparse,json,os,subprocess
ROOT=Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser();p.add_argument('run');a=p.parse_args()
out=ROOT/'build/remaining-ui-audit/gameplay'/a.run;launch=json.loads((out/'launch.json').read_text())
assert out.resolve().is_relative_to(ROOT/'build/remaining-ui-audit/gameplay')and not(out/'pid.txt').exists()
with Path(launch['log']).open('w')as log:
 process=subprocess.Popen([launch['java'],'@'+launch['arguments']],cwd=launch['cwd'],env={**os.environ,**launch['environment']},stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
(out/'pid.txt').write_text(str(process.pid));print('Started',a.run,process.pid,flush=True)
code=process.wait();(out/'exit.json').write_text(json.dumps({'exit_code':code}));print('Java exit',code,flush=True)
