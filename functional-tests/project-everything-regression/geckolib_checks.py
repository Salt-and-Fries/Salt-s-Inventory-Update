"""Check the actual legacy publisher comparator and an idle inventory revision."""
from pathlib import Path
import hashlib,json
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
EXPECTED={'null-empty':True,'empty-null':True,'empty-empty':True,'zero-null':False,
          'zero-empty':False,'zero-zero':True,'zero-other':False,'other-other':True}


def applicable(mc,loader,installed,menus):
    return (mc=='1.20.1' and loader=='fabric' and 'geckolib' in installed
            and 'net.orcinus.galosphere.client.gui.CombustionTableMenu' in menus)

def exercise(row,profile,initial,act,expect,snapshot):
    row.setdefault('test_helper_sha256',{})['geckolib_checks']=SOURCE_SHA256
    act('regression-geckolib-id-comparisons',ticks=2)
    proof=json.loads((profile/'regression-geckolib-id-comparisons.json').read_text())
    for case in proof['cases']:
        expect('actual GeckoLib identity comparator '+case['name'],case['actual'] is EXPECTED[case['name']])
    expect('native GeckoLib comparison covers every identity edge case',{c['name'] for c in proof['cases']}==set(EXPECTED))
    before=snapshot();act('regression-world',ticks=12);after=snapshot()
    delta=(after['playerSynchronization']['stateId']-before['playerSynchronization']['stateId'])%32768
    elapsed=after['gameTime']-before['gameTime']
    expect('empty player inventory remains empty during native idle check',not any(s['count'] for s in before['inventory']+after['inventory']))
    expect('idle native menu does not churn the player inventory revision',elapsed>=12 and delta==0,elapsed_ticks=elapsed,revision_delta=delta)
    row['native_geckolib_comparison_checked']=proof
    row['native_idle_revision_checked']={'before':before['playerSynchronization']['stateId'],
        'after':after['playerSynchronization']['stateId'],'elapsed_ticks':elapsed,'delta':delta}
