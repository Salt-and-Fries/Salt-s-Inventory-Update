"""Let a queued native login control precede the next serial replay queue."""
from pathlib import Path
import datetime,json,time
import psutil

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/project-everything-regression'
proof=OUT/'native-login-queue-boundary-01.json'
report=OUT/'project-everything-native-login-01-native-login-control.json'
parent=psutil.Process(36704);control=psutil.Process(38568)
assert any(v.endswith('run_reconciled_followups.py') for v in parent.cmdline())
assert 'project-everything-retries-02' in parent.cmdline()
assert any(v.endswith('run_native_login_probe.py') for v in control.cmdline())
assert not parent.children() and not (OUT/'project-everything-retries-02-source-integration.json').exists()
assert not proof.exists()
created=parent.create_time()
record={'coordinator':parent.pid,'native_control':control.pid,'resumed':False,
        'started_at':datetime.datetime.now(datetime.timezone.utc).isoformat(),
        'scope':'Hold only the still-idle next-queue coordinator. The active fill client finishes normally; native control still acquires the shared exclusive runtime lock.'}
parent.suspend()
try:
    assert not parent.children() and not (OUT/'project-everything-retries-02-source-integration.json').exists()
    proof.write_text(json.dumps(record,indent=2)+'\n')
    deadline=time.monotonic()+1800
    while control.is_running():
        if time.monotonic()>deadline:raise TimeoutError('Native control boundary timed out')
        time.sleep(2)
    record['native_result']=json.loads(report.read_text())['status']
finally:
    if parent.is_running() and parent.create_time()==created:
        parent.resume();record['resumed']=True
    record['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat()
    proof.write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps(record),flush=True)
