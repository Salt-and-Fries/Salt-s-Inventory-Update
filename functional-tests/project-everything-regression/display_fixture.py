"""Keep unattended QA responsive using ordinary, disposable display options."""
from pathlib import Path
import argparse,hashlib,json

SETTINGS={'maxFps':'60','inactivityFpsLimit':'"minimized"','pauseOnLostFocus':'false'}

def configure(runtime,output):
    runtime=Path(runtime);output=Path(output)
    assert runtime.is_dir() and output.is_dir()
    options=runtime/'options.txt'
    old=options.read_bytes() if options.exists() else b''
    lines=old.decode('utf-8').splitlines()
    retained=[line for line in lines if line.split(':',1)[0] not in SETTINGS]
    new=('\n'.join(retained+[k+':'+v for k,v in SETTINGS.items()])+'\n').encode()
    options.write_bytes(new)
    proof={'scope':'Disposable test profile display settings only; no publisher or shipping jar changes.',
        'settings':SETTINGS,'before_options_sha256':hashlib.sha256(old).hexdigest(),
        'options_sha256':hashlib.sha256(new).hexdigest(),
        'reason':'Avoid the native AFK frame cap during callback-driven tests. Normal game ticks, action waits and network barriers are unchanged. Older versions ignore the unavailable idle-limit option.'}
    (output/'qa-display-settings.json').write_text(json.dumps(proof,indent=2)+'\n')
    return proof

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--runtime',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
    configure(a.runtime,a.output);print('Configured disposable QA display options; game clock unchanged.')
