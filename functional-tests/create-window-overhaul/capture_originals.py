"""Capture unchanged original release screens without SIU for matched comparison.

The observer opens real native menus. This adapter changes only how the capture
driver locates their original screen coordinates; it does not alter mod UI/code.
"""
from pathlib import Path
import argparse,json
from suite import Suite
from capture import capture_all

ROOT=Path(__file__).resolve().parents[2]
GALLERY=json.loads((ROOT/'output/create-window-implementation/acceptance-evidence.json').read_text(encoding='utf-8'))['gallery']
SCREEN_MENUS={}
for entry in GALLERY:
    data=json.loads((ROOT/entry['screens']['fabric']['evidence']).read_text(encoding='utf-8'))
    window=next(w for w in data['client']['allWindows'] if w['menuId']==entry['menu'])
    SCREEN_MENUS[window['nativeScreen']]=entry['menu']

class OriginalCapture(Suite):
    def __init__(self,output):
        super().__init__(output)
        self.results['scope']='Original Create-family UI screenshot capture only; SIU absent; not a functional suite'
    def window(self,menu=None):
        assert self.state.get('siu_loaded') is False,'SIU must be absent'
        assert self.state.get('menuClass'),'Expected an original container screen'
        screen=self.state['screen'];identity=SCREEN_MENUS[screen]
        if menu is not None:assert identity==menu,(identity,menu)
        return {'menuId':identity,'nativeScreen':screen,'bounds':self.state['bounds']}
    def native_point(self,x,y,menu=None):
        b=self.window(menu)['bounds'];return {'x':b['x']+x,'y':b['y']+y}

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);a=p.parse_args();capture_all(a.output,OriginalCapture)
