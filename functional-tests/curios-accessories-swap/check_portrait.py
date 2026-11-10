"""Focused native mouse/visual check of the inset Accessories launcher."""
from pathlib import Path
import argparse, json, re, sys

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/all-target-rollout'))
from evidence import send


def run(profile):
    checks=[]
    def act(kind,**args):return send(profile,kind,**args)
    def check(label,ok):
        checks.append({'check':label,'passed':bool(ok)})
        (profile/'portrait-inset-checks.json').write_text(json.dumps({'scope':'Accessories portrait containment, edge clicks, hover and Curios switching only; no complete gameplay suite.','checks':checks},indent=2)+'\n')
        assert ok,label
        print('PASS',label,flush=True)
    def menus():
        act('regression-state',ticks=3)
        return json.loads((profile/'regression-state.json').read_text())['menus']
    act('command',command='gamemode survival @a',ticks=4)
    act('regression-character',ticks=8)
    info=json.loads((profile/'regression-character.json').read_text())
    row=next(r for r in info['attachments'] if r['type'].endswith('.accessories.AccessoriesClient$Button'))
    x,y,w,h=map(int,re.search(r'x=(-?\d+), y=(-?\d+), width=(\d+), height=(\d+)',row['regions']).groups())
    # Character content starts below its 16-pixel title. Model frame is 25,7,51,72.
    check('entire button lies inside the portrait rim',x>25 and x+w<76 and y>16+7 and y+h<16+7+72)
    parent=info['bounds']
    act('xy-click',x=parent['x']+75,y=parent['y']+y+h/2,ticks=10)
    check('portrait border no longer belongs to the Accessories click area',not menus())
    act('xy-click',x=parent['x']+x+w-1,y=parent['y']+y+h/2,ticks=14)
    current=menus()
    check('inset button rightmost pixel opens Accessories',len(current)==1 and current[0]['type'].endswith('.AccessoriesDesktopMenu'))
    act('regression-attachment-click',attachment='.curios.CuriosCharacterButton',ticks=14)
    current=menus()
    check('Curios still replaces Accessories through its own button',len(current)==1 and current[0]['type'].endswith('.CuriosDesktopMenu'))
    act('regression-character-info',ticks=2)
    parent=json.loads((profile/'regression-character.json').read_text())['bounds']
    act('hover',x=parent['x']+x+w/2,y=parent['y']+y+h/2,ticks=4)
    act('capture',capture='accessories-portrait-inset-hover',ticks=8)
    act('hover',x=parent['x']+150,y=parent['y']+75,ticks=4)
    act('capture',capture='accessories-portrait-inset',ticks=8)
    check('both visual reference frames captured',all((profile/'screenshots'/(n+'.png')).exists() for n in ['accessories-portrait-inset','accessories-portrait-inset-hover']))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('profile',type=Path);run(p.parse_args().profile)
