"""Focused checks for controls whose native composition changed in the fidelity repair."""
from pathlib import Path
import argparse,json
from suite import Suite

def run(output,groups):
    s=Suite(output)
    s.call('command',command='gamemode survival @a',ticks=2)
    s.call('gui-scale',scale=2,width=1280,height=900,ticks=3)
    # Deliberately omit the complete gameplay, schedule-type and management
    # suites. Exercise the moved native controls and their save/transfer paths.
    for group in groups:
        getattr(s,group)()
    for name,item,key in [('timed','timed_fuze','FuzeTimer'),('proximity','proximity_fuze','DetonationDistance'),('inertia','delayed_inertia_fuze','FuzeTimer'),('impact','delayed_impact_fuze','FuzeTimer')]:
        s.fixture();s.open_item('createbigcannons:'+item);s.call('wait',ticks=30)
        before=s.held_tag(name+'-native-start',key)
        s.call('scroll',**s.native_point(80,38),amount=1,ticks=30)
        after=s.held_tag(name+'-native-changed',key)
        s.check('fidelity-'+name+'-native-wheel',lambda:s.assert_equal(after>before,True))
        s.close_native(155,68)
        s.check('fidelity-'+name+'-footer-confirm-save',lambda:s.assert_equal(s.held_tag(name+'-native-footer-saved',key),after))
    s.results['groups']['fidelity_fuzes']='one native scroll and original footer confirm/save per fuze; range endpoints not repeated'
    for scale in [3,4]:
        s.fixture();s.call('gui-scale',scale=scale,width=1280,height=960 if scale==4 else 900,ticks=4);s.open_item('createbigcannons:creative_autocannon_ammo_container')
        w=s.window();s.check('fidelity-gui-'+str(scale)+'-complete-preview-canvas',lambda:s.assert_equal(any(r['id']=='native-companion' for r in w['composition']['regions']),True))
        s.check('fidelity-gui-'+str(scale)+'-window-fits',lambda:s.assert_equal(w['bounds']['x']>=0 and w['bounds']['y']>=0 and w['bounds']['x']+w['bounds']['width']<=s.state['guiWidth'] and w['bounds']['y']+w['bounds']['height']<=s.state['guiHeight'],True))
        s.snapshot('fidelity-gui-'+str(scale));s.tap(s.controls()['CLOSE'])
    s.call('gui-scale',scale=2,width=1280,height=900,ticks=3)
    s.results['scope']='Focused controls affected by Create native-fidelity layout repairs; not a complete functional/gameplay suite'
    s.results['complete']=True;s.save()
    latest={c['id']:c for c in s.results['checks']}
    print(json.dumps({'latest_checks':len(latest),'passed':all(c['passed'] for c in latest.values()),'history_retained':True}))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('output',type=Path)
    p.add_argument('--groups',nargs='+',default=['real_inventories','configurations','processing','categories','stock','autocomplete','connected_port'])
    a=p.parse_args();run(a.output,a.groups)
