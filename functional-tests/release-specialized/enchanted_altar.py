"""Native read-only altar observation, movement and persisted power state.

The native altar has no editable inventory/action UI. This checks its actual
server-backed status values, rather than labelling an empty open as gameplay.
"""
from pathlib import Path
import hashlib,sys

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/enchanted-windows'))
from focused import Check
from modern import ModernCheck


class CurrentCheck(Check):
    def window(self,menu):
        w=next(w for w in self.state.get('allWindows',[]) if w.get('menuId')=='enchanted:'+menu)
        assert w.get('templateClass',w.get('template'))=='com.salts_inventory_update.compat.enchanted.EnchantedWindows$Template',w
        return w


class CurrentModern(CurrentCheck,ModernCheck):
    # Retain the publisher-version-specific powerData fixture and assertions.
    altar=ModernCheck.altar


def run(profile,mc,loader,*args,**kwargs):
    profile=Path(profile);assert not (profile/'enchanted-checks/journal.json').exists()
    c=(CurrentCheck if mc=='1.20.1' else CurrentModern)(profile)
    c.results.update(scope='Native read-only Enchanted altar: status synchronization, movement, close and reopen. Not machine processing or altar world-power generation.',
        source_inputs={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in
            [Path(__file__),ROOT/'functional-tests/enchanted-windows/focused.py',ROOT/'functional-tests/enchanted-windows/modern.py']})
    c.call('gui-scale',scale=2,width=1280,height=900,ticks=4)
    c.cmd('gamemode survival @a');c.altar()
    assert len(c.results['checks'])>=5 and all(v['passed'] for v in c.results['checks'])
    c.results['complete']=True
    c.journal.write_text(__import__('json').dumps(c.results,indent=2)+'\n')
    return dict(cases=1,failed=0,checks=len(c.results['checks']),native_read_only_altar_checked=True,
        scope=c.results['scope'])
