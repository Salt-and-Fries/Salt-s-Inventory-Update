"""Exact supplemental native workflows; preserve original case identities and limits."""
from pathlib import Path
import hashlib,json,re
from focused_release_evidence import checked,reference
from native_publisher_index import index,matches

SCHEMA='additional-native-input-and-complete-resource-workflows-v1'
REQUESTER_TARGETS={'1.20.1/fabric','1.20.1/forge','1.21.1/neoforge','26.1.2/neoforge'}
MODULES={'requester':'requester_native_release_evidence','ae2_preferences':'ae2_preferences_native_evidence',
         'blue_arcs':'blue_arcs_native_evidence','mega_workbench':'mega_workbench_native_evidence',
         'requester_wireless':'requester_wireless_native_evidence','mega_bulk':'mega_bulk_native_evidence'}
COUNTS={'requester':32,'ae2_preferences':20,'blue_arcs':17,'mega_workbench':19,'requester_wireless':27,'mega_bulk':16}

def workflow(root,family,seal_path,expected):
    assert family in MODULES
    module=__import__(MODULES[family]);q=module.derive(root,seal_path,expected)
    count=q['validation']['assertions']if 'validation'in q else q['checks']
    assert count==COUNTS[family]
    return dict(family=family,qualification=q)

def contract(target,case,original_classes):
    alias=re.sub(r'^(?:(?:atlas|registry|machine|item)-)+','',case).replace('_','-')
    if alias in {'own-ae-part-requester-terminal','merequester-requester'}:
        assert target in REQUESTER_TARGETS
        terminal=alias=='own-ae-part-requester-terminal'
        menu='com.almostreliable.merequester.'+('terminal.RequesterTerminalMenu'if terminal else'requester.RequesterMenu')
        screen='com.almostreliable.merequester.client.'+('RequesterTerminalScreen'if terminal else'RequesterScreen')
        assert original_classes==[menu]
        return 'requester',[menu,screen]
    if alias=='ae2-terminal-settings':
        assert target=='1.21.1/neoforge'and original_classes==['appeng.client.gui.me.common.TerminalSettingsScreen']
        return 'ae2_preferences',['appeng.client.gui.me.common.TerminalSettingsScreen','appeng.client.gui.me.common.MEStorageScreen','appeng.menu.me.common.MEStorageMenu']
    if alias=='blue-skies-arcs':
        screen='com.legacy.blue_skies.client.gui.screen.ArcsScreen'
        assert target=='1.20.1/forge'and original_classes==[screen]
        return 'blue_arcs',[screen,'com.legacy.blue_skies.client.gui.menu.ArcsMenu']
    if alias=='megacells-portable-cell-workbench':
        assert target=='1.21.1/neoforge'and original_classes==[]
        return 'mega_workbench',['gripe._90.megacells.client.screen.PortableCellWorkbenchScreen','gripe._90.megacells.menu.PortableCellWorkbenchMenu']
    if alias=='merequester-wireless-terminal':
        assert target in {'1.21.1/neoforge','26.1.2/neoforge'}and original_classes==[]
        return 'requester_wireless',['com.almostreliable.merequester.compat.wtlib.ReqWirelessTerminalScreen','com.almostreliable.merequester.compat.wtlib.ReqWirelessTerminalMenu']
    raise AssertionError(('Unreviewed supplemental case',target,case,original_classes))

def publishers(root,seal_path,target,classes):
    root=Path(root);seal=json.loads(Path(seal_path).read_text());result=[];found=set()
    for artifact in json.loads(checked(root,seal['staged']).read_text())['artifacts']:
        if artifact['family']!='upstream':continue
        path=Path(artifact['file']);path=path if path.is_absolute()else root/path
        raw=path.read_bytes();assert hashlib.sha256(raw).hexdigest()==artifact['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if 'sha512'in artifact:assert digest==artifact['sha512']
        locations=matches(index(path,digest,target.split('/')[1]),classes)
        if locations:
            present={n[:-6].replace('/','.')for loc in locations for n in loc['classes']};found.update(present)
            result.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert found==set(classes)and result
    return result

def derive(root,seal_path,expected,target,case,original_classes,additional=()):
    root=Path(root).resolve();family,classes=contract(target,case,original_classes)
    primary=workflow(root,family,seal_path,expected);q=primary['qualification'];assert q['target']==target
    extras=[]
    if family=='mega_workbench':
        assert len(additional)==1;extras=[workflow(root,'mega_bulk',additional[0],expected)]
    else:assert not additional
    return dict(schema=SCHEMA,target=target,case=case,disposition='functional-pass',family=family,original_native_classes=original_classes,
                native_classes=classes,subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,seal_path),acceptance=q['acceptance'],
                validation=q.get('validation',dict(assertions=q.get('checks'),states=q.get('states'))),primary=primary,additional_workflows=extras,
                publishers=publishers(root,seal_path,target,classes),scope=q['scope'],
                limits='Only the exact sealed native publisher/fixture and the stated UI interactions. Additional variants share no invented gameplay executions. Nested dependencies and full QA/runtime artifacts remain pinned by the original seal. '+('The bulk compression workflow is separate from the ordinary fuzzy-cell workflow; neither claims storage throughput.'if extras else''))

def verify(root,record,expected):
    assert record['schema']==SCHEMA and record['disposition']=='functional-pass'
    extras=[checked(root,v['qualification']['seal'])for v in record['additional_workflows']]
    assert derive(root,checked(root,record['sealed_evidence']),expected,record['target'],record['case'],record['original_native_classes'],extras)==record
    return True
