"""Current-Core replay of the retained MI native input/geometry acceptance."""
from pathlib import Path
import hashlib, importlib.util, json, sys

ROOT=Path(__file__).resolve().parents[2]
INPUTS=[ROOT/'tools/check-modern-industrialization-refinement.py',
        ROOT/'tools/exercise-modern-industrialization.py',ROOT/'tools/check-modern-industrialization-windows.py',
        ROOT/'functional-tests/all-target-rollout/evidence.py']


def run(profile,mc,loader,*args,**kwargs):
    assert (mc,loader)in [('1.20.1','fabric'),('1.21.1','neoforge')]
    profile=Path(profile);output=profile/'mi-release';output.mkdir()
    spec=importlib.util.spec_from_file_location('mi_release_refinement',INPUTS[0])
    driver=importlib.util.module_from_spec(spec);spec.loader.exec_module(driver)
    inputs=[Path(__file__),*INPUTS]
    fingerprints={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest()for p in inputs}
    ui=driver.Ui(profile.name)
    ui.send('gui-scale',scale=2,width=1280,height=900,ticks=4)
    previous=sys.argv;sys.argv=['check-modern-industrialization-refinement.py',profile.name]
    try:driver.main()
    finally:sys.argv=previous
    journal=ui.out/'refinement-actions.json';actions=json.loads(journal.read_text())
    assert len(actions)==7 and all(v['status']=='pass'for v in actions)
    ui.send('close',ticks=3);assert ui.state()['sessions']==0
    states=sorted(ui.out.glob('*-server.json'))+sorted(ui.out.glob('*-client.json'))
    assert len(states)>=20
    for path in states:json.loads(path.read_text())
    assert fingerprints=={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest()for p in inputs}
    scope=('Fresh MI native pipe/tank bucket/shape, slot-lock and auto-eject controls, '
           'movement/scale and complete-cell/component geometry. Charged hatch state '
           'is a read-only synchronization fixture; not natural power generation or '
           'formed multiblock/long recipe-chain simulation. One aggregated scenario, seven named groups.')
    assert all(isinstance(v.get('check'),str)and v['check']for v in actions)
    result=dict(complete=True,scope=scope,actions=actions,
        checks=[dict(name=v['check'],passed=v['status']=='pass')for v in actions],qa_inputs=fingerprints,
        original_journal=dict(file=str(journal.relative_to(ROOT)),sha256=hashlib.sha256(journal.read_bytes()).hexdigest()),
        state_inputs=[dict(file=str(p.relative_to(ROOT)),sha256=hashlib.sha256(p.read_bytes()).hexdigest())for p in states])
    (output/'summary.json').write_text(json.dumps(result,indent=2)+'\n')
    return dict(cases=1,failed=0,checks=len(actions),mi_native_features_checked=True,scope=scope)
