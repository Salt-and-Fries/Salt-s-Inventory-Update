"""Join independently captured case observations without inferring success."""
import argparse
import json
from pathlib import Path
from PIL import Image

def crop_rect(image_path, state, bounds):
    if not image_path or not bounds:
        return None
    with Image.open(image_path) as image:
        width,height=image.size
    sx=width/state.get('guiWidth',width); sy=height/state.get('guiHeight',height)
    # World margin also retains usual external recipe-book controls.
    margin=64
    left,top=bounds['x'],bounds['y']
    right,bottom=left+bounds['width'],top+bounds['height']
    for widget in state.get('widgets',[]):
        if widget.get('visible',True) and all(k in widget for k in ('x','y','width','height')):
            left=min(left,widget['x']);top=min(top,widget['y'])
            right=max(right,widget['x']+widget['width']);bottom=max(bottom,widget['y']+widget['height'])
    return [max(0,round((left-margin)*sx)),max(0,round((top-margin)*sy)),
            min(width,round((right+margin)*sx)),min(height,round((bottom+margin)*sy))]

def join(cases_path, native_path, core_path, output_path):
    cases=json.loads(cases_path.read_text(encoding='utf-8'))
    native={r['id']:r for r in json.loads(native_path.read_text(encoding='utf-8'))}
    core={r['id']:r for r in json.loads(core_path.read_text(encoding='utf-8'))}
    entries={'candlelight':9,'farm_and_charm':10,'meadow':12,'nethervinery':13,
             'dungeonz':182,'exposure':219,'extended_drawers':221,'immersive_aircraft':297,
             'niftycarts':415,'revive':496,'scholar':506,'smallships':529}
    result=[]
    for case in cases['cases']:
        n=native.get(case['id'],{}); c=core.get(case['id'],{})
        nc=n.get('observation',{}).get('client',{}); cc=c.get('observation',{}).get('client',{})
        windows=cc.get('allWindows',cc.get('windows',[])); definition=windows[-1].get('definition') if windows else None
        if not nc.get('menuClass'): outcome='native opening not established'
        elif not c or c.get('error'): outcome='core capture failed'
        elif definition and definition.endswith('AutomaticNativeWindow'): outcome='automatic'
        elif windows and definition in (None,'none','builtin') and ('ChestMenu' in str(cc.get('menu')) or cc.get('menuId','').startswith('minecraft:generic_')): outcome='builtin'
        elif windows and definition in (None,'none','builtin'): outcome='builtin or unclassified; review'
        elif not windows and cc.get('screen')==nc.get('screen'): outcome='native preserved'
        elif not windows and cc.get('screen') in (None,'none'): outcome='core did not open a screen'
        else: outcome='requires review'
        result.append({'id':case['id'],'entry':case.get('entry',entries.get(case['family'])),
                       **({'workflow_id':case['workflow_id']} if case.get('workflow_id') else {}),
                       'family':case['family'],'minecraft':cases['minecraft'],'loader':cases['loader'],
                       'outcome':outcome,'native':{'capture':n.get('capture'),'sha256':n.get('sha256'),
                       'screen':nc.get('screen'),'menu_class':nc.get('menuClass'),'title':nc.get('title'),'bounds':nc.get('bounds'),
                       'crop_rect':crop_rect(n.get('capture'),nc,nc.get('bounds')),
                       'slots':nc.get('slots'),'observation':str(native_path)},
                       'core':{'capture':c.get('capture'),'sha256':c.get('sha256'),'screen':cc.get('screen'),
                       'menu_class':cc.get('menu') or c.get('observation',{}).get('server',{}).get('menu'),'menu_id':cc.get('menuId'),'windows':windows,
                       'crop_rect':crop_rect(c.get('capture'),cc,windows[-1].get('bounds') if windows else nc.get('bounds')),
                       'observation':str(core_path)}})
    output_path.parent.mkdir(parents=True,exist_ok=True)
    output_path.write_text(json.dumps({'scope':'opening and geometry observations; visual review pending',
                                      'cases':result},indent=2),encoding='utf-8')
    return result

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('cases',type=Path);p.add_argument('native',type=Path);p.add_argument('core',type=Path);p.add_argument('output',type=Path)
    a=p.parse_args(); join(a.cases,a.native,a.core,a.output)
