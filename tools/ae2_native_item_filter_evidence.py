"""Validate ordinary native Items-filter prerequisite observations."""
import json
from focused_release_evidence import reference
from ae2_settings_features import ledger

MENU='appeng.menu.me.common.MEStorageMenu'
CHILD='appeng.client.gui.implementations.KeyTypeSelectionScreen'

def derive(root, sealed, folder):
    refs=[]
    for path in sorted(folder.glob('item-key-type-selection-*.json')):
        ref=reference(root,path)
        assert ref in sealed['full_states']
        data=json.loads(path.read_text());sid=data['session']
        assert type(sid) is int and sid>0 and path.name=='item-key-type-selection-'+str(sid)+'.json'
        def selected(key):
            value=data[key]
            rows=[v for v in value['client'] if v['session']==sid and v['menu']==MENU]
            servers=[v for v in value['server'] if v['session']==sid and v['menu']==MENU]
            assert len(rows)==len(servers)==1
            return rows[0]
        before=selected('before_widgets');child=selected('child_widgets')
        picked=selected('selected_widgets');after=selected('after_widgets')
        assert before['screen']==after['screen']=='appeng.client.gui.me.common.MEStorageScreen'
        assert child['screen']==picked['screen']==CHILD
        assert data['button'] in before['widgets'] and data['checkbox'] in child['widgets'] and data['back'] in picked['widgets']
        for name in ['button','checkbox','back']:
            widget=data[name]
            assert widget['visible'] is True and widget['active'] is True
            assert all(type(widget[k]) is int for k in ['x','y','width','height']) and widget['width']>0 and widget['height']>0
        assert data['button']['type'].endswith('.KeyTypeSelectionButton')
        assert not any('Items' in text for text in data['button'].get('tooltips',[]))
        assert data['checkbox']['type'].endswith('.AECheckbox') and data['checkbox']['message']=='Items'
        assert data['back']['type'].endswith('.TabButton') and data['back']['icon']=='BACK'
        filters=[v for v in after['widgets'] if v['type'].endswith('.KeyTypeSelectionButton') and v['visible'] is True and v['active'] is True]
        assert len(filters)==1 and any('Items' in text for text in filters[0].get('tooltips',[]))
        assert ledger(data['before_ledger'])==ledger(data['after_ledger'])
        for key in ['before_ledger','after_ledger']:
            menus=[v for v in data[key]['menus'] if v['session']==sid and v['type']==MENU]
            assert len(menus)==1
        assert 'build/release-readiness/ae2_crafting_native_item_filter_1238.py' in sealed['archived_inputs']
        refs.append(ref)
    if sealed['target'].startswith('1.20.1/'):
        assert not refs
    return refs
