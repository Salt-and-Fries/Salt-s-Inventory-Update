"""Fix desktop-only AE2 return tabs and draw joining strips before their icons.

Native full-screen controls and original publisher instructions are unchanged.
All supported named/runtime patch namespaces are updated together.
"""
from pathlib import Path
import copy, json

ROOT=Path(__file__).resolve().parents[1]
TAB='appeng/client/gui/widgets/TabButton'
STYLE=TAB+'$Style'
HELPER='com/salts_inventory_update/compat/ae2/nativeview/NativeTabJoins'
def field(owner,name,desc,op=180):return {'type':4,'op':op,'owner':owner,'name':name,'desc':desc}
def call(owner,name,desc,op=184):return {'type':5,'op':op,'owner':owner,'name':name,'desc':desc,'interface':False}
def load(var):return {'type':2,'op':25,'var':var}

def prepare_method(icon):
    return {'name':'desktopPrepareNativeReturn','desc':'()V','access':1,'exceptions':[],
        'visibleAnnotations':[],'invisibleAnnotations':[],'handlers':[],
        'maxStack':2,'maxLocals':1,'instructions':[
            load(0),field(TAB,'icon','L'+icon+';'),
            field(icon,'BACK','L'+icon+';',178),
            {'type':7,'op':166,'label':'return_tab_done'},
            load(0),field(TAB,'style','L'+STYLE+';'),
            field(STYLE,'CORNER','L'+STYLE+';',178),
            {'type':7,'op':166,'label':'return_tab_done'},
            load(0),field(STYLE,'BOX','L'+STYLE+';',178),
            field(TAB,'style','L'+STYLE+';',181),
            {'type':8,'op':-1,'label':'return_tab_done'},
            {'type':14,'op':-1,'frameType':-1,'locals':[{'kind':'string','value':TAB}],'stack':[]},
            {'type':0,'op':177}]}

def main():
    paths=list((ROOT/'compatibility/ae2/versions').glob('**/src/main/resources/ae2-native-patches*/appeng/client/gui/WidgetContainer.json'))
    assert len(paths)==8
    for p in paths:
        d=json.loads(p.read_text())
        if any(i.get('owner')==HELPER and i.get('name')=='draw'
                for m in d['methods'] for i in m['instructions']):continue
        # Native layout: select BOX only for the white BACK glyph that would
        # otherwise intersect CORNER's broad white highlight. Keep item tabs.
        legacy='1.20.1' in p.parts
        if not legacy:
            m=next(m for m in d['methods'] if m['name']=='lambda$populateAeDesktop$0')
            m['instructions'][:0]=[load(3),call(HELPER,'prepare','(Ljava/lang/Object;)V')]
            m['maxStack']=max(m['maxStack'],1)
        # Generic layout: replace the final post-icon seam fills with a shared
        # pass immediately before the external controls are painted.
        m=next(m for m in d['methods'] if m['name']=='drawDesktopAttachments')
        nodes=m['instructions']; assert len(nodes)==270
        assert nodes[151]==load(0) and nodes[178].get('label')=='o29'
        assert nodes[180]==load(0) and nodes[-2].get('op')==177
        graphics=m['desc'][1:m['desc'].index(';')+1]
        instructions=[load(1),load(0),copy.deepcopy(next(i for i in nodes if i.get('name')=='desktopPanels')),
            load(0),copy.deepcopy(next(i for i in nodes if i.get('name')=='desktopPanelTabs')),
            load(0),copy.deepcopy(next(i for i in nodes if i.get('name')=='desktopScreen')),
            call('appeng/client/gui/AEBaseScreen','desktopBackgroundColor','()I',182),
            call(HELPER,'draw',f'({graphics}Ljava/util/Map;Ljava/util/Map;I)V')]
        # Leave loop exit o29 intact, remove only the old mask loop thereafter.
        m['instructions']=nodes[:151]+instructions+nodes[151:180]+[{'type':0,'op':177}]
        m['maxStack']=max(m['maxStack'],4)
        p.write_text(json.dumps(d,separators=(',',':')))
        if not legacy:
            tab=p.parent/'widgets/TabButton.json';t=json.loads(tab.read_text())
            icon=next(i['desc'][1:-1] for m in t['changes'] for e in m['edits']
                for i in e['insert'] if i.get('name')=='icon')
            assert not any(m['name']=='desktopPrepareNativeReturn' for m in t['methods'])
            t['methods'].append(prepare_method(icon));tab.write_text(json.dumps(t,separators=(',',':')))
        print(p.relative_to(ROOT))
    # Point-release overrides can replace the entire TabButton delta.
    # Keep the same helper whenever such an override exists.
    for p in (ROOT/'compatibility/ae2/versions/1.21.1').glob('**/ae2-native-patches-*/appeng/client/gui/widgets/TabButton.json'):
        d=json.loads(p.read_text())
        if not any(m['name']=='desktopPrepareNativeReturn' for m in d['methods']):
            d['methods'].append(prepare_method('appeng/client/gui/Icon'))
            p.write_text(json.dumps(d,separators=(',',':')))
            print(p.relative_to(ROOT))

if __name__=='__main__':main()
