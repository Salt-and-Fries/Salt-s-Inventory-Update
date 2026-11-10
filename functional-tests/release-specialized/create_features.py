"""Replay retained native Create feature assertions on the current subjects."""
from pathlib import Path
import hashlib,json,sys

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/create-window-overhaul'))
from native_all import NativeAll
from native_five import NativeFive
from create_processing import CreateProcessing

GROUPS=['toolbox_frame','table_frame','filter_frame','attribute_frame','blueprint_frame','ports','remaining_configurations','ammo','mounted_gauge','fuze_controls',
        'categories','stock','stock_category_hiding','connected_port','schedules',
        'schedule_timeline','autocomplete','processing','management']


class CurrentCreate(CreateProcessing,NativeAll,NativeFive):
    def window(self,menu=None):
        # The current observer separates the template's class from its layout
        # JSON. Keep the original assertion and native coordinates; adapt only
        # this observation field's name.
        rows=self.state.get('allWindows',self.state.get('windows',[]))
        for row in rows:
            if isinstance(row.get('template'),dict) and row.get('templateClass'):
                row['template']=row['templateClass']
        self.state['allWindows']=rows
        return super().window(menu)


def run(profile,*args,**kwargs):
    profile=Path(profile)
    assert not (profile/'features/journal.json').exists(),'Never merge a previous failed feature attempt'
    mc=str(profile.name).split('-',1)[0]
    if mc=='1.21.1':
        from create_modern import ModernCreate
        suite=ModernCreate(profile)
    else:
        assert mc=='1.20.1',mc
        suite=CurrentCreate(profile)
    suite.results['scope']='Fresh current-subject native Create feature replay; named groups only, not every Create gameplay mechanic.'
    suite.results['planned_groups']=GROUPS
    suite.results['driver_sources']={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in
        [Path(__file__),ROOT/'functional-tests/create-window-overhaul/native_all.py',
         ROOT/'functional-tests/create-window-overhaul/suite.py',ROOT/'functional-tests/project-everything-ten/send.py']}
    suite.results['driver_sources'].update({str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in
        [ROOT/'functional-tests/create-window-overhaul/native_five.py',ROOT/'functional-tests/create-window-overhaul/fixtures.py',
         Path(__file__).with_name('create_processing.py')]})
    if mc=='1.21.1':suite.results['driver_sources'].update({str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in
        [Path(__file__).with_name('create_modern.py'),Path(__file__).with_name('create_codec_1211.py')]})
    suite.save();suite.call('gui-scale',scale=2,width=1280,height=900,ticks=4)
    suite.call('command',command='gamemode survival @a',ticks=3)
    completed=[]
    for group in GROUPS:
        getattr(suite,group)();completed.append(group)
        suite.results['completed_groups']=list(completed);suite.save()
    assert completed==GROUPS and suite.results['checks'] and all(c['passed'] for c in suite.results['checks'])
    suite.results['complete']=True;suite.save();suite.call('close',ticks=3)
    return dict(cases=1,failed=0,checks=len(suite.results['checks']),create_features_checked=True,
                feature_groups=completed,scope=suite.results['scope'])
