"""Launch one already prepared disposable production profile, once."""
from pathlib import Path
import argparse,json,os,subprocess
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('output',type=Path);a=p.parse_args();out=a.output.resolve()
assert out.is_relative_to(ROOT/'build/remaining-ui-audit/gameplay')
assert not (out/'process.json').exists(),'This profile was already launched; preserve its evidence.'
launch=json.loads((out/'launch.json').read_text());argfile=Path(launch['arguments']);runtime=Path(launch['cwd']);log=Path(launch['log'])
assert argfile.is_relative_to(out) and log.is_relative_to(out) and not log.exists()
assert runtime.is_relative_to(Path.home()/'.cache/siu-rollout-runtime')
with log.open('w') as stream:
 process=subprocess.Popen([launch['java'],'@'+str(argfile)],cwd=runtime,env={**os.environ,**launch['environment']},stdout=stream,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
(out/'process.json').write_text(json.dumps({'pid':process.pid,'launch':launch},indent=2));print('STARTED',process.pid,out)
