"""Focused final Schedule title-inset regression; native actions are unchanged."""
from pathlib import Path
import argparse
from native_all import NativeAll

def run(output):
    s=NativeAll(output)
    for scale in [2,4]:
        s.call('gui-scale',scale=scale,width=1280,height=900 if scale==2 else 960,ticks=5)
        s.fixture();s.open_item('create:schedule');w=s.window();b=w['bounds'];label=f'schedule-header-gui{scale}'
        def within_strip():
            assert (b['width'],b['height'])==(256,226),b
            for control,p in s.controls().items():
                if control=='RESIZE_GRIP':continue
                assert b['x']+14<=p['x'] and p['x']+10<=b['x']+242,(control,p,b)
                assert b['y']<=p['y']<=b['y']+14,(control,p,b)
        s.check(label+'-controls-inside-visible-strip',within_strip)
        s.photo(label)
        info=lambda:next(w for w in s.state['workspaceWindows'] if w['kind']=='CONTAINER')
        if info()['locked']=='true':s.tap(s.controls()['LOCK'])
        s.check(label+'-unlock',lambda:s.assert_equal(info()['locked'],'false'))
        if scale==2:
            a={'x':b['x']+30,'y':b['y']+7};z={'x':a['x']+16,'y':a['y']+8}
            s.call('click',**a,button=0,ticks=1);s.call('drag',**z,button=0,dx=16,dy=8,ticks=1);s.call('release',**z,button=0,ticks=1)
            s.check(label+'-title-drag',lambda:s.assert_equal((s.window()['bounds']['x'],s.window()['bounds']['y']),(b['x']+16,b['y']+8)))
        s.tap_native(50,32)
        s.check(label+'-native-editor',lambda:s.assert_equal(s.window()['templateState']['editing'],True))
        s.tap_native(110,97);s.call('text',text='Harbor',ticks=2);s.tap_native(233,96)
        s.check(label+'-native-confirm',lambda:s.assert_equal(s.window()['templateState']['editing'],False))
        s.tap(s.controls()['CLOSE'])
        s.check(label+'-close-save',lambda:s.assert_contains(s.saved_item(label+'-saved'),'Harbor'))
    s.call('gui-scale',scale=2,width=1280,height=900,ticks=4)
    s.results['groups']['final_header']='Final title-inset binary: visible native strip bounds, management unlock, title drag, native editor/confirm and SIU close persistence at GUI scales 2 and 4';s.save()

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);a=p.parse_args();run(a.output)
