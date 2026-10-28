"""Capture affected windows in an already running, isolated target fixture.

This is an opening/geometry pass, not a full functional test. Additional
interaction checks are recorded separately against the same running client.
"""
from pathlib import Path
import importlib.util
import json

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('qa_send', ROOT / 'functional-tests/project-everything-ten/send.py')
sender = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sender)

CASES = {
    'extendeddrawersaddon': [('single-drawer', 'extended_drawers:single_drawer', True),
        ('double-drawer', 'extended_drawers:double_drawer', True),
        ('quad-drawer', 'extended_drawers:quad_drawer', True)],
    'the-graveyard-fabric': [('ossuary', 'graveyard:ossuary', False),
        ('urn', 'graveyard:red_urn', False), ('small-urn', 'graveyard:small_red_urn', False),
        ('coffin', 'graveyard:oak_coffin', False), ('sarcophagus', 'graveyard:sarcophagus', False)],
    'betterend': [('end-smelter', 'betterend:end_stone_smelter', False),
        ('end-furnace', 'betterend:end_stone_furnace', False)],
    'runes': [('rune-altar', 'runes:crafting_altar', False)],
    'deep-aether': [('combiner', 'deep_aether:combiner', False)],
    'tieredz': [('anvil-tabs', 'minecraft:anvil', False),
        ('grindstone-tabs', 'minecraft:grindstone', False)],
    'smitherz': [('smithing-tabs', 'minecraft:smithing_table', False)],
    'additionz': [('fletching', 'minecraft:fletching_table', False),
        ('chunk-loader', 'additionz:chunk_loader', False)],
    'chipped': [('chipped-'+name.replace('_','-'), 'chipped:'+name, False) for name in
        ('mason_table','carpenters_table','loom_table','glassblower','botanist_workbench',
         'alchemy_bench','tinkering_table')],
    'simply-swords': [('runic-forge', 'simplyswords:runic_forge', False)],
}

def check(output, only=()):
    mc, loader = output.name.split('-')[:2]
    availability = json.loads((ROOT / 'build/third-batch-ports/availability.json').read_text(encoding='utf-8'))
    target = next(t for t in availability['targets'] if t['minecraft']==mc and t['loader']==loader)
    families = [entry['project'] for entry in target['integrations']]
    rows = []
    def command(name, **kwargs): return sender.send(output, name, **kwargs)
    command('catalog')
    catalog = sender.read(output / 'catalog.json')
    for family in families:
        if family not in CASES:
            rows.append({'family':family, 'status':'not-configured'})
            continue
        for name, block, crouch in CASES[family]:
            if only and name not in only and family not in only:
                continue
            row = {'family':family, 'case':name, 'block':block}
            rows.append(row)
            try:
                if not block.startswith('minecraft:') and block not in catalog['blocks']:
                    raise AssertionError('Expected block absent from runtime registry; check upstream version')
                command('fixture',block=block,ticks=12)
                command('crouch-open' if crouch else 'open',realUse=True,ticks=30)
                result=command('capture',capture=loader+'-'+name+'-open',ticks=12)
                windows=result['client']['windows']
                row.update({'screen':result['client']['screen'], 'windows':windows,
                            'menu':result['server']['menu'], 'capture':result.get('screenshot')})
                if len(windows)!=1:
                    raise AssertionError('Expected one detached window; inspect screen and menu')
                window=windows[0]; bounds=window['bounds']
                if name in ('fletching','single-drawer','double-drawer','quad-drawer','ossuary','rune-altar','combiner','end-smelter','anvil-tabs','smithing-tabs','grindstone-tabs','chunk-loader') and not window.get('definition','').endswith('NativeTemplateWindow'):
                    raise AssertionError(name+' used a fallback instead of its native template')
                if name.startswith('chipped-') and not window.get('definition','').endswith('ChippedWindow'):
                    raise AssertionError('Chipped dedicated definition was not selected')
                if name=='runic-forge' and not window.get('definition','').endswith('SimplySwordsWindow'):
                    raise AssertionError('Runic Forge dedicated definition was not selected')
                if window.get('layoutError'):
                    raise AssertionError('Native layout error: '+str(window['layoutError']))
                if bounds['x']<0 or bounds['y']<0 or bounds['x']+bounds['width']>result['client']['guiWidth'] or bounds['y']+bounds['height']>result['client']['guiHeight']:
                    raise AssertionError('Window bounds exceed screen')
                row['status']='opened; visual review pending'
                if name == 'ossuary':
                    ready=command('seed',slot=0,item='minecraft:bone_block',count=8,ticks=35,capture=loader+'-ossuary-bone')
                    b=ready['client']['windows'][0]['bounds']
                    selected=command('xy-click',x=b['x']+47,y=b['y']+29,ticks=20,capture=loader+'-ossuary-selected')
                    product=selected['server']['slots'][1]
                    if product['count'] <= 0:
                        raise AssertionError('Ossuary recipe click produced no server result')
                    taken=command('click',slot=1,ticks=15,capture=loader+'-ossuary-crafted')
                    if taken['server']['slots'][0]['count']!=7 or taken['server']['cursor']['count']!=product['count']:
                        raise AssertionError('Ossuary result/input conservation failed')
                    row['operation']={'input_after':7,'result':product['item'],'cursor_count':taken['server']['cursor']['count']}
                elif name == 'rune-altar':
                    command('seed',slot=0,item='minecraft:cobblestone',count=3)
                    ready=command('seed',slot=1,item='minecraft:amethyst_shard',count=3,ticks=20,capture=loader+'-rune-ready')
                    product=ready['server']['slots'][2]
                    if product['item']!='runes:arcane_stone' or product['count']!=8:
                        raise AssertionError('Runes recipe did not produce eight arcane stones')
                    taken=command('click',slot=2,ticks=15)
                    if [s['count'] for s in taken['server']['slots'][:2]]!=[2,2] or taken['server']['cursor']['count']!=8:
                        raise AssertionError('Runes result/input conservation failed')
                    row['operation']={'inputs_after':[2,2],'cursor_item':taken['server']['cursor']['item'],'cursor_count':8}
                elif name == 'urn':
                    command('seed',slot=0,item='minecraft:diamond',count=3)
                    pickup=command('click',slot=0)
                    returned=command('click',slot=0,ticks=12,capture=loader+'-urn-returned')
                    if pickup['server']['cursor']['count']!=3 or returned['server']['cursor']['count']!=0 or returned['server']['slots'][0]['count']!=3:
                        raise AssertionError('Urn pickup and return failed')
                    removed=command('remove-source',ticks=30)
                    if removed['client']['sessions']!=0:
                        raise AssertionError('Urn window did not close on source removal')
                    row['operation']={'pickup_count':3,'returned_count':3,'closed_on_source_removal':True}
                command('close')
            except Exception as error:
                row.update({'status':'needs-investigation','error':str(error)})
                try: command('close')
                except Exception: pass
            (output/'opening-observations.json').write_text(json.dumps(rows,indent=2),encoding='utf-8')
            print(name, row['status'], flush=True)
    return rows
