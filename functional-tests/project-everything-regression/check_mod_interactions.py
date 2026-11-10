"""Real inventory round trips through shipping SIU, without computer use.

The excluded observer seeds only disposable world/player fixtures. All tested
transfers use production screen callbacks, networking and publisher-owned slots.
Ghost/result/restricted slots are explicitly excluded from generic insertion;
their specialized actions require separate named scenarios.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, re, sys, traceback

ROOT = Path(__file__).resolve().parents[2]
DRIVER_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
sys.path.insert(0, str(ROOT / 'functional-tests/all-target-rollout'))
from evidence import send
from inventory_ledger import tagged_total as count_tagged,after_seed,with_closed_source,SOURCE_SHA256 as LEDGER_SHA256

def native_barrel_menu(menu):
    # Publisher menu registry IDs survive intermediary/official class naming.
    return menu.get('menuType')=='minecraft:generic_9x3'


def portable_energy(stack):
    match=re.search(r'(?:internalCurrentPower|"?ae2:stored_energy"?)\s*[:=]\s*([0-9.eE+-]+)',stack.get('signature',''))
    if not match:raise AssertionError('Native portable source has no observable stored-energy value')
    return float(match.group(1))

def block_cases():
    # Read the declarative cases without importing a CLI driver with side effects.
    import ast
    source = (ROOT / 'functional-tests/all-target-rollout/capture-container-batch.py').read_text()
    tree = ast.parse(source)
    assignment = next(n for n in tree.body if isinstance(n, ast.Assign) and any(isinstance(t, ast.Name) and t.id == 'CASES' for t in n.targets))
    return eval(compile(ast.Expression(assignment.value), '<reviewed-block-cases>', 'eval'), {'range': range})

def run(profile, mc, loader, only=(), max_cases=None, mods=(), *, capture_debug=None, click_ticks=(), network_fence=False, require_portable_lock=False):
    rows, assertions, exercised, rejected_inputs = [], [], set(), set()
    discovered=[];selected_cases=[];rejected=[];native_specimens={};requirements_checker_sha256=None;native_entry_translator_sha256=None
    report = profile / 'mod-interaction-checks.json'
    from single_clicks import SingleClickCadence,SOURCE_SHA256 as CLICK_CADENCE_SHA256
    click_cadence=SingleClickCadence(lambda command_type,**kw:send(profile,command_type,**kw))
    from itertools import cycle
    timing_pattern=tuple(click_ticks)
    if timing_pattern and any(t not in range(1,9) for t in timing_pattern):raise ValueError('Click waits must be 1..8 normal game ticks')
    timings=cycle(timing_pattern) if timing_pattern else None
    if report.exists():
        import datetime, shutil
        archive = profile / 'regression-attempts'; archive.mkdir(exist_ok=True)
        shutil.copy2(report, archive / (datetime.datetime.now().strftime('%Y%m%d-%H%M%S') + '-checks.json'))
    def save():
        scroll=profile/'regression-scroll-evidence.json'
        if rows and scroll.exists():
            proof=json.loads(scroll.read_text())
            if rows[-1].get('session')==proof.get('session'):rows[-1]['native_scroll_check']=proof
        report.write_text(json.dumps({'minecraft': mc, 'loader': loader, 'protocol': 8, 'test_driver_sha256':DRIVER_SHA256,'inventory_ledger_sha256':LEDGER_SHA256,'entry_requirements_checker_sha256':requirements_checker_sha256,'native_entry_translator_sha256':native_entry_translator_sha256,'interaction_settle_ticks':4,'cases': rows,
            'discovered_case_ids':discovered,'selected_case_ids':selected_cases,'selection':{'only':list(only),'max_cases':max_cases,'mods':list(mods)},
            'debug_capture_override':capture_debug,
            'click_wait_pattern':list(timing_pattern),
            'observational_network_fence':network_fence,
            'require_portable_lock':require_portable_lock,
            'rejected_reviewed_workflows':rejected,
            'checks': assertions, 'coverage_limit': 'Named transfers and controls only; not every recipe, machine cycle or possible mod combination.'}, indent=2) + '\n')
    def act(command_type, **kw):
        kind=command_type
        if kind=='fixture' and loader=='neoforge':
            # These native observer fixtures build the block but leave the
            # player at a previous case's location. Use ordinary teleport setup
            # before native input; later authored camera/position steps win.
            from fixture_isolation import approach,SOURCE_SHA256 as APPROACH_SHA256
            result=send(profile,kind,**kw)
            command=approach(origin)
            if rows:rows[-1]['fixture_approach']={'command':command,'helper_sha256':APPROACH_SHA256,
                'scope':'Native world fixture placement only; ordinary block-use packets remain unchanged.'}
            send(profile,'command',command=command,ticks=6)
            return result
        if kind in {'client-use-item','use-item'} and 'shift' in kw:
            return send(profile,'regression-use-held-item',**kw)
        if kind=='regression-seed-player':
            prepared=[]
            for entry in kw['items']:
                entry=dict(entry);specimen=native_specimens.get(entry.get('item'))
                if entry.get('item')=='bountiful:decree':
                    entry['native_decree']=True
                    if rows:rows[-1]['native_fixture_factory']='Bountiful public DecreeItem.Companion factory; player fixture only.'
                if specimen and entry.get('snbt')=='{siu_regression_marker:42}':
                    entry['snbt']=specimen['snbt'];entry['fixture_specimen']=specimen['native_evidence']
                    if rows:rows[-1]['native_item_specimen']={k:specimen[k] for k in ['item','publisher_sha256','native_evidence','reason']}
                prepared.append(entry)
            kw['items']=prepared
        if kind=='regression-template-region-tap':
            state=send(profile,'wait',ticks=3)['client']
            peers=[w for w in state.get('windows',[]) if w.get('menuId')==kw['parent_menu']]
            expect('native gear tab has exactly one matching parent window',len(peers)==1,parent=kw['parent_menu'])
            return send(profile,kind,session=peers[0]['sessionId'],**kw)
        if kind=='regression-native-widget':
            state=send(profile,'wait',ticks=3)['client']
            native=not state.get('windows') and state.get('screen') not in ['none',None,'com.salts_inventory_update.client.InventoryDesktopScreen']
            pool=state.get('widgets',[]) if native else [{**v,'source_session':w.get('sessionId'),'source_definition':w.get('definition','')} for w in state.get('windows',[]) for v in w.get('nativeWidgets',[])]
            peers=[v for v in pool if v.get('visible',True) and v.get('type')==kw['widget_type'] and v.get('message','')==kw.get('message','')]
            if not peers and not native:
                send(profile,'regression-native-widget-catalog',ticks=2)
                pool=json.loads((profile/'regression-native-widget-catalog.json').read_text())
                peers=[v for v in pool if v.get('visible',True) and v.get('type')==kw['widget_type'] and v.get('message','')==kw.get('message','')]
            index=kw.get('ordinal',0)
            if index>=len(peers):
                missing=snapshot()
                if rows:rows[-1]['missing_child_widget_entry']={'screen':state.get('screen'),'host':missing.get('nativeHost',{}),'selector':kw}
            expect('publisher child menu has the recorded semantic widget',index<len(peers),selector=kw)
            widget=peers[index]
            expect('publisher transition widget is active',widget.get('active',True),selector=kw)
            source={'session':widget['source_session']} if widget.get('source_session') is not None else {}
            if widget.get('source_definition','').endswith('.NativeTemplateWindow'):
                return send(profile,'regression-template-widget-tap',widget_type=kw['widget_type'],message=kw.get('message',''),ordinal=index,
                            button=kw.get('button',0),ticks=kw.get('ticks',16),**source)
            return send(profile,'regression-native-tap' if native else 'regression-tap',x=widget['x']+widget['width']/2,y=widget['y']+widget['height']/2,
                        button=kw.get('button',0),ticks=kw.get('ticks',16),**source)
        if kind=='client-use-entity':
            return send(profile,'regression-use-entity',**kw)
        if kind=='regression-open' and kw.get('emptyHand'):
            # Modern useWithoutItem needs the main hand. Callers can also
            # request normal main-hand use for legacy mod blocks that ignore
            # offhand activation, after releasing the held-source lock.
            kw.setdefault('allowHeldMainHand',mc!='1.20.1')
            if mc!='1.20.1' and rows and any(str(v).endswith('.NetworkToolScreen') for v in rows[-1].get('native_screens',[])):
                # NeoForge's native AE2 tool consumes onItemUseFirst even on a
                # barrel. Select an empty hotbar cell without moving its bound
                # host item, so this remains a real block interaction.
                send(profile,'regression-select-empty-hand',ticks=4)
                rows[-1]['secondary_entry_selection']='Empty hotbar selection; original AE2 tool stays in its bound inventory cell.'
        if kind in ['client-use-item','client-use-block','open-ray','use-block-ray'] and 'shift' in kw:
            send(profile,'sneak',down=bool(kw['shift']),ticks=2)
            try:return send(profile,kind,**kw)
            finally:send(profile,'sneak',down=False,ticks=2)
        # Normal rapid clicks, followed by an authoritative server snapshot.
        # Preserve longer publisher entry/processing waits; do not accelerate
        # the game clock or bypass the client/server packet path.
        if kind in {'regression-player-click','regression-slot-click','regression-control','regression-close','regression-seed-player'}:
            kw['ticks']=min(4,kw.get('ticks',4))
        if timings is not None and kind in {'regression-player-click','regression-slot-click'}:
            kw['ticks']=next(timings)
            if rows:rows[-1].setdefault('measured_click_waits',[]).append({'type':kind,'ticks':kw['ticks'],'slot':kw['slot'],'session':kw.get('session',0)})
        if kind in {'regression-player-click','regression-slot-click'} and rows:
            rows[-1].setdefault('test_helper_sha256',{})['single_clicks']=CLICK_CADENCE_SHA256
            rows[-1]['single_click_timing_contract']='Ordinary repeated left clicks wait beyond the native 250 ms pickup-all window; intentional Core double-click tests use their separate input path.'
        return click_cadence.send(kind,**kw)
    def expect(label, condition, **detail):
        assertions.append({'case': current, 'check': label, 'passed': bool(condition), **detail}); save()
        if not condition:
            import shutil
            evidence = profile / 'regression-failures' / current; evidence.mkdir(parents=True, exist_ok=True)
            for name in ['regression-geckolib-id-comparisons.json','regression-bountiful-decree-ledger.json', 'regression-mutation-state.json', 'regression-click-inputs.json', 'regression-server-block-use.json', 'regression-native-browser.json', 'regression-block-use.json', 'regression-state.json', 'reply.json', 'observations.json','regression-ray-state.json','observer-policy-side-effects.json','regression-native-container-ledger.json','regression-point.json','regression-ae2-cell-ledger.json','regression-ae2-repository-state.json','regression-native-settings.json','regression-character.json','regression-builtin.json']:
                if (profile / name).exists(): shutil.copy2(profile / name, evidence / name)
            raise AssertionError(label)
    def snapshot(item='minecraft:diamond'):
        if network_fence:
            import time
            deadline=time.monotonic()+15
            delivery_barrier=None
            while True:
                act('regression-network-status',ticks=1)
                status=json.loads((profile/'regression-network-status.json').read_text())
                if delivery_barrier is None:delivery_barrier=status['outboundSubmitted']
                if status['outboundDelivered']>=delivery_barrier and not status['clicks']['outstanding'] and not status['pendingGesture']:break
                if time.monotonic()>deadline:
                    expect('submitted clicks receive their original server decision before observation',False,network_status=status)
            if rows:rows[-1]['network_observation_contract']='Wait for actual outbound delivery and original server decisions before authoritative snapshots; never retry input or require acceptance.'
        act('regression-state', item=item, snbt='{siu_regression_marker:42}', ticks=2,
            syncDiagnostics=mc=='1.20.1' and ('galosphere' in current or current in {
                'registry-meadow-cheese_form','registry-meadow-cooking_cauldron',
                'appbot-portable-mana-cell','megacells-portable-mana-cell'}))
        return json.loads((profile / 'regression-state.json').read_text())
    def management(session):
        act('regression-management',session=session,ticks=2)
        return json.loads((profile/'regression-management.json').read_text())
    def total(data, item):
        cells = list(data['physical'].values()) + data['inventory'] + [data['cursor']]
        return sum(s['count'] for s in cells if s['item'] == item)
    def cursor(data, item, count):
        return data['cursor']['count'] == count and (not count or data['cursor']['item'] == item)
    def tagged_total(data, item, signature):
        return count_tagged(data,item,signature)
    def rendered_windows(state):
        windows=list(state.get('windows',[]));seen={w.get('sessionId') for w in windows}
        for window in state.get('workspaceWindows',[]):
            match=re.search(r'session:(\d+)',window.get('name',''))
            if window.get('kind')!='CONTAINER' or not match or int(match[1]) in seen:continue
            locked=window.get('locked',False)
            windows.append({'sessionId':int(match[1]),'bounds':window['bounds'],'locked':locked=='true' if isinstance(locked,str) else bool(locked),
                            'core_builtin':True,'definition':'Core built-in container'})
        return windows
    act('catalog', ticks=1)
    catalog = json.loads((profile / 'catalog.json').read_text())
    blocks, items = set(catalog['blocks']), set(catalog['items'])
    act('regression-catalog',ticks=1)
    extra_catalog=json.loads((profile/'regression-catalog.json').read_text());entities=set(extra_catalog['entities']);origin=extra_catalog['origin']
    staged = (profile / 'staged-artifacts.json').read_text().lower()
    staged_data=json.loads((profile/'staged-artifacts.json').read_text())
    specimen_path=Path(__file__).with_name('fixtures')/'native-item-specimens.json'
    if specimen_path.exists():
        binaries={r.get('sha256') for r in staged_data['artifacts']}
        for specimen in json.loads(specimen_path.read_text())['specimens']:
            if binaries.intersection(specimen['publisher_sha256']):
                assert specimen['item'] not in native_specimens,'Ambiguous native fixture specimen'
                proof=ROOT/specimen['native_evidence']
                assert hashlib.sha256(proof.read_bytes()).hexdigest()==specimen['native_evidence_sha256'],'Native initialized specimen evidence changed'
                native_specimens[specimen['item']]=specimen
    from collect_results import mod_ids
    installed_mods=set().union(*(mod_ids(Path(a['file'])) for a in staged_data['artifacts'] if a.get('file')))
    entry_policies=json.loads((ROOT/'output/project-everything-regression/native-entry-policies.json').read_text())['policies']
    publisher_hashes={v.get('sha256') for v in staged_data['artifacts']}
    canonical=lambda value:re.sub(r'[^a-z0-9]','',value.lower())
    families={}
    for entry in json.loads((ROOT/'output/all-target-rollout/availability-and-registration.json').read_text()):
        if (entry['minecraft'],entry['loader'])!=(mc,loader):continue
        ids=set(entry['mod_ids'].replace(',',' ').replace(';',' ').split())
        for name in [entry['project'],entry.get('slug',''),*ids]:families[canonical(name)]=ids
    cases = []
    for label, block in block_cases():
        owner = {'easy-anvils': 'easyanvils', 'easy-magic': 'easymagic', 'visual-workbench': 'visualworkbench'}.get(label)
        if owner and owner not in re.sub(r'[-_ ]', '', staged): continue
        if owner in {'easyanvils', 'easymagic','visualworkbench'} and owner + ':' + block.replace(':', '/') in blocks:
            block = owner + ':' + block.replace(':', '/')
        if block not in blocks and block.startswith('toms_storage:'):
            block = block.replace('toms_storage:', 'toms_storage:ts.')
        if block not in blocks: continue
        actions = [{'type': 'fixture', 'block': block, 'ticks': 12}, {'type': 'client-use-block', 'ticks': 20}]
        if label in {'travelers-block', 'alloy-forge', 'alloyed-forge', 'alloyed-oven', 'toms-inventory-link'}:
            continue  # Require their native placement/multiblock/channel fixtures below.
        cases.append({'id': label, 'actions': actions})
    if mc in ['26.2','26.3'] and 'ae2:wireless_crafting_terminal' in items:
        x,y,z=origin
        cases.append({'id':'ae2-wireless-crafting-terminal',
            'expected_native_menu':'appeng.menu.me.items.WirelessCraftingTermMenu',
            'entry_mode':'charged-native-wireless-item-and-powered-access-point',
            'actions':[{'type':'fixture','block':'ae2:wireless_access_point','ticks':12},
                {'type':'command','command':f'setblock {x} {y} {z+1} ae2:creative_energy_cell','ticks':40},
                {'type':'command','command':'item replace entity @a weapon.mainhand with ae2:wireless_crafting_terminal[ae2:stored_energy=1000000.0d,ae2:wireless_link_target={dimension:"minecraft:overworld",pos:[I;'+f'{x},{y},{z}'+']}]','ticks':8},
                {'type':'client-use-item','ticks':24}]})
    # The original reviewed manifests retain actual item/part/entity entry paths.
    for path in (ROOT / 'build/all-target-rollout').glob('*cases*.json'):
        data = json.loads(path.read_text())
        if data.get('loader') != loader or data.get('minecraft', mc) != mc: continue
        if '1201' in path.name and mc != '1.20.1' or '1211' in path.name and mc != '1.21.1': continue
        for case in data.get('cases', []):
            if case.get('loader',loader)!=loader or case.get('minecraft',mc)!=mc:
                rejected.append({'id':case['id'],'reason':'Workflow belongs to another version/loader','manifest':str(path.relative_to(ROOT))});continue
            owner=families.get(canonical(case.get('family','')))
            if owner and not owner.intersection(installed_mods):
                rejected.append({'id':case['id'],'reason':'Publisher is not installed in this exact cohort','required_mod_ids':sorted(owner),'manifest':str(path.relative_to(ROOT))});continue
            actions = case.get('core_actions', [])
            if not actions: continue
            requirements = [(k, a[k]) for a in actions for k in ('item', 'block') if k in a]
            entity_requirements={a['entity'] for a in actions if ':' in a.get('entity','')}
            for action in actions:
                command=action.get('command','')
                entity_requirements.update(re.findall(r'(?:^|\brun\s+)summon\s+([a-z0-9_.-]+:[a-z0-9_./-]+)',command))
                requirements.extend(('item',v) for v in re.findall(r'\bwith\s+([a-z0-9_.-]+:[a-z0-9_./-]+)',command))
            if not requirements and not entity_requirements or not entity_requirements.issubset(entities) or any(v not in (blocks if k == 'block' else items) for k, v in requirements):
                rejected.append({'id':case['id'],'reason':'Required item, block or entity is absent from the runtime registry',
                    'missing_entities':sorted(entity_requirements-entities),'missing_items_or_blocks':[(k,v) for k,v in requirements if v not in (blocks if k=='block' else items)],'manifest':str(path.relative_to(ROOT))});continue
            from native_entries import translate
            old=data.get('origin','8,65,8');old=[int(v) for v in (old.split(',') if isinstance(old,str) else old)]
            actions=[{**a,**({'command':translate(a['command'],old,origin)} if a.get('type') in ['command','server-command'] else {})} for a in actions]
            cases.append({'id': case['id'], 'actions': actions, 'manifest': str(path.relative_to(ROOT))})
    for item, label in [('sophisticatedbackpacks:backpack', 'sophisticated-backpack'), ('travelersbackpack:standard', 'travelers-backpack'),
                        ('toms_storage:item_filter', 'toms-item-filter'), ('toms_storage:tag_item_filter', 'toms-tag-filter')]:
        if item not in items: item = item.replace('toms_storage:', 'toms_storage:ts.')
        if item in items:
            cases.append({'id': label, 'actions': [{'type': 'fixture', 'block': 'minecraft:stone'},
                {'type': 'command', 'command': 'item replace entity @a weapon.mainhand with ' + item, 'ticks': 6},
                {'type': 'client-use-item', 'ticks': 20}]})
    if 'sophisticatedbackpacks:backpack' in items:
        cases.append({'id':'sophisticated-placed-backpack','actions':[
            {'type':'fixture','block':'minecraft:stone','ticks':6},
            {'type':'command','command':'setblock %d %d %d minecraft:air' % tuple(origin),'ticks':4},
            {'type':'command','command':'item replace entity @a weapon.mainhand with sophisticatedbackpacks:backpack','ticks':4},
            {'type':'regression-open','dx':0,'dy':-1,'dz':0,'shift':True,'ticks':8},
            {'type':'command','command':'item replace entity @a weapon.mainhand with minecraft:air','ticks':4},
            {'type':'regression-open','dx':0,'dy':0,'dz':0,'ticks':20}]})
    already = {a.get('block') for c in cases for a in c['actions'] if a.get('type') == 'fixture'}
    # Ordinary registry-matched menus: no reflective provider/screen creation.
    # Publisher-specific factories still run through the normal item/block use.
    for menu in sorted(catalog['menus']):
        namespace, name = menu.split(':', 1)
        if namespace in {'minecraft', 'salts_inventory_update', 'salts_inventory_compat'}: continue
        if namespace=='sophisticatedbackpacks': continue  # Both genuine item and placement workflows are explicit above.
        stem = re.sub(r'(_menu|_container|_screen)$', '', name)
        block = namespace + ':' + stem
        if namespace == 'powah':
            aliases = {'cable': 'energy_cable', 'solar': 'solar_panel', 'thermo': 'thermo_generator', 'discharger': 'energy_discharger'}
            base = namespace + ':' + aliases.get(stem, stem)
            block = next((base + '_' + tier for tier in ['starter', 'basic', 'hardened'] if base + '_' + tier in blocks), base)
        if block in blocks and block not in already:
            actions=[{'type':'fixture','block':block,'ticks':10}]
            if block=='ae2:chest':
                # The upper cell face is insertion/storage interaction. Its
                # ordinary configuration UI is opened from the casing side.
                actions.append({'type':'command','command':'tp @a %s %s %s 180 30' % (origin[0]+.5,origin[1],origin[2]+2.5),'ticks':8})
                actions.append({'type':'open-ray','ticks':18})
            else:actions.append({'type':'client-use-block','ticks':18})
            cases.append({'id': 'registry-' + block.replace(':', '-'), 'actions': actions})
            already.add(block)
        elif menu in items and menu not in blocks and namespace in {'create', 'createbigcannons', 'kibe', 'wands', 'botania', 'toms_storage'}:
            cases.append({'id': 'registry-item-' + menu.replace(':', '-'), 'actions': [
                {'type': 'fixture', 'block': 'minecraft:stone', 'ticks': 8},
                {'type': 'command', 'command': 'item replace entity @a weapon.mainhand with ' + menu, 'ticks': 4},
                {'type': 'client-use-item', 'ticks': 18}]})
    # Dynamic machine menus share a menu type across different native slot maps.
    tr_path = ROOT / 'output/tech-reborn-window-design/manifest.json'
    tr_blocks = {'techreborn:' + c['id'].removeprefix('techreborn-').replace('-', '_')
                 for c in json.loads(tr_path.read_text())['pages']}
    mi_path = ROOT / 'output/modern-industrialization-design/mockup-manifest.json'
    mi_blocks = {'modern_industrialization:' + c['id'].removeprefix('modern-industrialization-').replace('-', '_')
                 for c in json.loads(mi_path.read_text())['cases'] if c['id'].startswith('modern-industrialization-')}
    for block in sorted(blocks):
        if block in already: continue
        if block.startswith('techreborn:') and any(word in block for word in ['_ore', '_block', '_cable', '_wire', 'rubber', 'reinforced_', 'fluid_pipe']): continue
        dynamic = block in mi_blocks or block in tr_blocks
        if dynamic:
            cases.append({'id': 'machine-' + block.replace(':', '-'), 'actions': [
                {'type': 'fixture', 'block': block, 'ticks': 10}, {'type': 'client-use-block', 'ticks': 18}]})
            already.add(block)
    # Deduplicate across historical manifests; do not silently substitute missing UI.
    atlas=ROOT/'output/project-everything-regression/native-entry-cases.json'
    staged_data=json.loads((profile/'staged-artifacts.json').read_text())
    hashes={r.get('sha256') for r in staged_data.get('artifacts',[])}
    if atlas.exists():
        from native_entries import translate,SOURCE_SHA256 as TRANSLATOR_SHA256
        native_entry_translator_sha256=TRANSLATOR_SHA256
        from entry_requirements import missing as missing_entry_requirements,SOURCE_SHA256
        from registry_entry_adaptations import adapt as adapt_registry_entry
        requirements_checker_sha256=SOURCE_SHA256
        for original in json.loads(atlas.read_text())['cases']:
            if (original['minecraft'],original['loader'])!=(mc,loader) or not hashes.intersection(original['publisher_sha256']):continue
            entry_actions,registry_correction=adapt_registry_entry(original,mc,loader,blocks)
            unavailable=missing_entry_requirements(entry_actions,items=items,blocks=blocks,entities=entities)
            old=[int(v) for v in original['captured_origin'].split(',')]
            actions=[]
            for action in entry_actions:
                action=dict(action)
                if action['type']=='command':action['command']=translate(action['command'],old,origin)
                action['type']={'use-item':'client-use-item','use-entity':'client-use-entity'}.get(action['type'],action['type'])
                actions.append(action)
            cases.append({**original,'actions':actions,'source':'original-native-atlas',
                **({'registry_entry_adaptation':registry_correction,'original_entry_actions':original['actions']} if registry_correction else {}),
                **({'missing_registry_requirements':unavailable} if unavailable else {})})
    from equipment_checks import cases as equipment_cases
    cases.extend(equipment_cases(installed_mods,items))
    from entity_inventory_cases import cases as entity_inventory_cases
    cases.extend(entity_inventory_cases(mc,loader,installed_mods,items,set(catalog['menus']),origin,
        json.loads(atlas.read_text()).get('specialized_entry_gaps',[]),hashes,entities))
    from reconcile_results import identity
    unique={}
    for case in cases:
        key=identity(case['id'])
        if key in unique and case.get('source')=='original-native-atlas':case['id']=unique[key]['id']
        unique[key]=case
    cases=list(unique.values())
    from ae2_fixture import normalize as normalize_ae2_fixture,SOURCE_SHA256 as AE2_FIXTURE_SHA256
    for case in cases:
        original=case['actions'];normalized=[]
        entry_block=next((v.get('block') for v in original if v['type']=='fixture'),None)
        for action in original:
            action=dict(action)
            if action['type'] in {'command','server-command'}:
                if (any(v.get('item')=='botania:hand_of_ender' for v in original)
                        and action['command'].startswith('item replace entity @a inventory.0 with botania:mana_tablet')):
                    # Native reopening spends mana again. Keep the authored
                    # charged tablet out of cells used by transfer fixtures.
                    action['command']=action['command'].replace('inventory.0 with','inventory.26 with',1)
                prepared=normalize_ae2_fixture(action['command'],mc)
                if prepared!=action['command']:
                    action['command']=prepared
                    case['native_key_type_fixture']={'normalizer_sha256':AE2_FIXTURE_SHA256,
                        'contract':'AE2 native terminal saved state explicitly enables item/fluid key types; id-only modern part NBT is not default state.'}
            if action['type']=='fixture':
                block=action['block']
                if block=='spawn:pigment_shifter':action['water']=True
                keep_fletching=(block=='minecraft:fletching_table' and case.get('expected_native_screen')=='net.additionz.misc.FletchingScreen')
                for owner in ['easyanvils','easymagic','visualworkbench'] if block.startswith('minecraft:') and not keep_fletching else []:
                    replacement=owner+':'+block.replace(':','/')
                    if replacement in blocks:action['block']=replacement;break
            # The legacy screenshot collector's `open` shortcut could invoke
            # MenuProvider directly. Exercise the actual block interaction here.
            if action['type']=='open':
                action.pop('realUse',None);action['type']='client-use-block'
            if action['type']=='open-ray' and entry_block in {'create:brown_toolbox','create:package_frogport','create:redstone_requester'}:
                # The UI test targets the same native block-use callback as
                # registry-discovered menus, independent of the screenshot
                # collector's cached render-camera ray. Native menu identity
                # remains checked against the original reference.
                action['type']='client-use-block'
            action['type']={'use-item':'client-use-item','use-entity':'client-use-entity'}.get(action['type'],action['type'])
            if action['type'] in ['client-use-entity-ray','use-entity-ray'] and action.get('entity')=='smallships:galley':
                action['type']='client-use-entity'
            if normalized and action['type']=='client-use-block' and normalized[-1]['type']=='client-use-block':
                normalized[-1]['ticks']=max(action.get('ticks',16),normalized[-1].get('ticks',16))
            else:normalized.append(action)
        if normalized!=original:
            case['original_entry_actions']=original;case['entry_mode']='ordinary-client-interaction';case['actions']=normalized
        actions=case['actions']
        if case.get('expected_native_menu')=='com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSetItemMenu':
            # Create's short-interaction handling participates in loader input
            # events. A direct gameMode.useItemOn callback skips that part of
            # the real use-key gesture. Keep the original aimed world target.
            case.setdefault('original_entry_actions',original)
            case['entry_mode']='native-world-use-key-press-and-release'
            case['actions']=[step for a in actions for step in (
                [{'type':'regression-world-use-button','down':True,'ticks':2},
                 {'type':'regression-world-use-button','down':False,'ticks':12}]
                if a['type'] in {'open-ray','client-use-block-ray'} else [a])]
        if entry_block=='crabbersdelight:crab_trap' and actions[-1]['type']=='client-use-block':
            # Water flow can otherwise move the test player out of the native
            # menu's valid distance during later clicks. Keep all of the
            # publisher's water prerequisites; stand on an ordinary dry ledge.
            from fixture_isolation import water_walkway
            case.setdefault('original_entry_actions',original)
            case['entry_mode']='native-water-prerequisites-with-dry-approach'
            case['actions']=actions[:-1]+[{'type':'command','command':v,'ticks':4} for v in water_walkway(origin)]+actions[-1:]
        if entry_block=='spawn:pigment_shifter' and len(actions)==2:
            # The native block intentionally refuses to open while dry. The
            # legacy screenshot fixture's water flag was not implemented by
            # every observer. Seed its valid public waterlogged block state;
            # opening still goes through the ordinary native block-use path.
            case['original_entry_actions']=original
            case['entry_mode']='waterlogged-world-fixture-and-native-block-use'
            case['actions']=[actions[0],
                {'type':'command','command':'setblock %d %d %d spawn:pigment_shifter[waterlogged=true]' % tuple(origin),'ticks':6},actions[1]]
        if entry_block=='tradingpost:trading_post' and len(actions)==2:
            # A post only opens when a real nearby merchant has offers. Seed
            # a disposable native villager, not a fake Trading Post provider.
            count='Count:1b' if mc=='1.20.1' else 'count:1'
            offer='{buy:{id:"minecraft:emerald",'+count+'},sell:{id:"minecraft:bread",'+count+'},uses:0,maxUses:9999}'
            npc='{NoAI:1b,NoGravity:1b,Invulnerable:1b,VillagerData:{profession:"minecraft:farmer",level:1,type:"minecraft:plains"},Offers:{Recipes:['+offer+']}}'
            summon='summon minecraft:villager %s %s %s %s' % (origin[0]+1.5,origin[1],origin[2]+1.5,npc)
            case['original_entry_actions']=original
            case['entry_mode']='native-nearby-villager-and-block-use'
            case['actions']=[actions[0],{'type':'command','command':summon,'ticks':12},actions[1]]
        if entry_block=='prominent:primordial_forge' and len(actions)==2:
            # The publisher refuses fresh-player access until its progression
            # component is unlocked. Its public admin command supplies only
            # that player fixture; actual block use still creates the menu.
            case['original_entry_actions']=original
            case['entry_mode']='native-progression-command-and-block-use'
            case['actions']=[actions[0],{'type':'command','command':'prominent:set_primordial_forge_unlocked @a true','ticks':8},actions[1]]
        if entry_block and entry_block.startswith('powah:reactor_') and entry_block in items and len(actions)==2:
            # A default state creates an unformed shell part. ReactorItem's
            # real placement consumes 36 blocks and creates/builds its core.
            # Never force core/built NBT or invoke its menu provider directly.
            case['original_entry_actions']=original
            case['entry_mode']='native-36-block-reactor-item-placement'
            low=[origin[0]-1,origin[1],origin[2]-1];high=[origin[0]+1,origin[1]+3,origin[2]+1]
            case['actions']=[actions[0],
                {'type':'command','command':'fill %d %d %d %d %d %d minecraft:air' % tuple(low+high),'ticks':4},
                {'type':'regression-place','block':'minecraft:stone','dx':0,'dy':-1,'dz':0,'ticks':3},
                {'type':'command','command':'gamemode survival @a','ticks':4},
                {'type':'command','command':'item replace entity @a weapon.mainhand with '+entry_block+' 36','ticks':4},
                # Powah's native builder places one part every six ticks. All
                # 35 surrounding parts must finish before processing checks;
                # an early-opened core exposes its input while still building.
                {'type':'regression-open','dx':0,'dy':-1,'dz':0,'ticks':240},
                {'type':'command','command':'item replace entity @a weapon.mainhand with minecraft:air','ticks':4},
                {'type':'regression-open','dx':0,'dy':0,'dz':0,'ticks':20}]
        if entry_block=='enchanted:altar' and len(actions)==2 and 'enchanted:altar' in items:
            case['original_entry_actions']=original
            case['entry_mode']='native-multiblock-item-placement'
            assembled=[actions[0]]
            neighbours=[(x,z) for x in range(3) for z in range(2) if (x,z)!=(0,0)]
            for x,z in neighbours:assembled.append({'type':'regression-place','block':'minecraft:air','dx':x,'dz':z,'ticks':2})
            assembled.append({'type':'command','command':'item replace entity @a weapon.mainhand with enchanted:altar','ticks':4})
            for x,z in neighbours:
                assembled += [{'type':'regression-place','block':'minecraft:stone','dx':x,'dy':-1,'dz':z,'ticks':2},
                              {'type':'regression-open','dx':x,'dy':-1,'dz':z,'ticks':5}]
            assembled += [{'type':'command','command':'item replace entity @a weapon.mainhand with minecraft:air','ticks':4},
                          {'type':'regression-open','dx':0,'dy':0,'dz':0,'ticks':18}]
            case['actions']=assembled
        if entry_block in {'storagenetwork:inventory','storagenetwork:request','storagenetwork:request_expanded'} and len(actions)==2 and 'storagenetwork:master' in blocks:
            # Native SSN deliberately refuses to open an unconnected terminal.
            # Let its real master discover an adjacent cable and empty barrel.
            case['original_entry_actions']=original
            case['entry_mode']='native-connected-storage-network'
            case['actions']=[actions[0],
                {'type':'regression-place','block':'storagenetwork:master','dx':1,'ticks':4},
                {'type':'regression-place','block':'storagenetwork:storage_kabel','dx':2,'ticks':4},
                {'type':'regression-place','block':'minecraft:barrel','dx':3,'ticks':80},actions[1]]
        if entry_block=='ae2:cable_bus' and case.get('expected_native_screen') in {
                'appeng.client.gui.implementations.PriorityScreen',
                'appeng.client.gui.me.common.TerminalSettingsScreen',
                'appeng.client.gui.me.crafting.CraftingStatusScreen'}:
            # Production reference worlds retain neighbouring fixture blocks.
            # A previous quantum ring at origin+south intercepts this recorded
            # ray before it reaches the part. Clear that approach cell only;
            # retain the real part, original ray and native widget transition.
            case.setdefault('original_entry_actions',original)
            case['entry_approach_cleanup']='Remove previous fixture obstruction at origin+south; keep authored native ray.'
            case['actions']=[actions[0],{'type':'regression-place','block':'minecraft:air','dx':0,'dz':1,'ticks':4}]+actions[1:]
        if case['id']=='registry-item-create-linked_controller':
            case['original_entry_actions']=original
            case['entry_mode']='ordinary-native-sneak-use'
            case['actions']=[{**a,**({'shift':True} if a['type']=='client-use-item' else {})} for a in actions]
        if case['id']=='registry-item-create-crafting_blueprint':
            # This is a hanging blueprint, not a hand-opened bag. Place it
            # through item use, then interact with its actual entity surface.
            case['original_entry_actions']=original
            case['entry_mode']='ordinary-native-placement-and-entity-use'
            case['actions']=actions[:-1]+[
                {'type':'regression-open','dx':0,'dy':0,'dz':0,'ticks':12},
                {'type':'command','command':'item replace entity @a weapon.mainhand with minecraft:air','ticks':4},
                {'type':'regression-use-entity','entity':'create:crafting_blueprint','at':True,'ticks':18}]
        if len(actions)==2 and actions[0].get('type')=='fixture' and actions[0].get('block','').startswith('sophisticatedstorage:') and actions[1]['type'] in ['open','client-use-block']:
            block=actions[0]['block']
            if block not in items:continue
            case['original_entry_actions']=actions
            case['entry_mode']='ordinary-item-placement'
            # The approach pose can point down far enough to place a limited
            # barrel facing up. Clicking that front face deposits items rather
            # than opening its menu. Place it while looking horizontally so
            # the subsequent ordinary top-face click opens the native UI.
            pose=[{'type':'command','command':'execute as @a at @s run tp @s ~ ~ ~ 0 0','ticks':4}] if block.startswith('sophisticatedstorage:limited_barrel_') else []
            case['actions']=[{'type':'fixture','block':'minecraft:stone','ticks':6},
                {'type':'command','command':'setblock %d %d %d minecraft:air' % tuple(origin),'ticks':4}]+pose+[
                {'type':'command','command':'item replace entity @a weapon.mainhand with '+block,'ticks':4},
                {'type':'regression-open','dx':0,'dy':-1,'dz':0,'shift':True,'ticks':8},
                {'type':'command','command':'item replace entity @a weapon.mainhand with minecraft:air','ticks':4},
                {'type':'regression-open','dx':0,'dy':0,'dz':0,'ticks':18}]
    discovered=[c['id'] for c in cases]
    if mods:
        from collect_results import mod_ids
        hash_mods={a.get('sha256'):mod_ids(Path(a['file'])) for a in staged_data['artifacts'] if a.get('file')}
        def belongs(case):
            owners={case['mod_id']} if case.get('mod_id') else set()
            for action in case['actions']:
                owners.update(v.split(':')[0] for k,v in action.items() if k in ['block','item'] and ':' in str(v))
                owners.update(re.findall(r'\b([a-z0-9_.-]+):[a-z0-9_./-]+',str(action.get('command',''))))
            for digest in case.get('publisher_sha256',[]):owners.update(hash_mods.get(digest,set()))
            return bool(owners.intersection(mods))
        cases=[c for c in cases if belongs(c)]
    if max_cases:
        priority={'nether-chest':0,'sophisticated-barrel':1,'bartering-station':2}
        cases=sorted(cases,key=lambda c:priority.get(c['id'],3))[:max_cases]
    selected_cases=[c['id'] for c in cases if not only or c['id'] in only or identity(c['id']) in {identity(v) for v in only}]
    for case in cases:
        current = case['id']
        if only and current not in only and identity(current) not in {identity(v) for v in only}: continue
        row = {'id': current, 'status': 'running', 'actions': case['actions'],
               **{k:case[k] for k in ['source','publisher_sha256','original_evidence','expected_native_screen','expected_native_menu','expected_nonplayer_slot_count','entry_mode','entry_approach_cleanup','original_entry_actions','native_key_type_fixture','entry_fixture_helper_sha256','registry_entry_adaptation'] if k in case}}; rows.append(row); save()
        print('START', current, flush=True)
        try:
            if case.get('missing_registry_requirements'):
                row.update(status='invalid-fixture-workflow',missing_registry_requirements=case['missing_registry_requirements'],
                    registry_evidence='catalog.json and regression-catalog.json from this exact live publisher cohort',
                    coverage='Historical entry is unavailable in this exact registry (including optional integrations). No container functional pass claimed; dependency-complete variants require separate tests.')
                save();print('END',current,row['status'],flush=True);continue
            act('regression-world',ticks=1)
            act('regression-debug-capture',enabled=(current in {'item-wands-magic-bag-1','atlas-wands-magic-bag-1','sophisticated-backpack','travelers-backpack','atlas-ae2-terminal','atlas-merequester-wireless-terminal'} if capture_debug is None else capture_debug),ticks=1)
            act('command', command='gamemode creative @a', ticks=2)
            act('close', ticks=3)
            act('sneak',down=False,ticks=2)
            act('regression-reset-hand',ticks=3)
            act('command', command='clear @a', ticks=2)
            act('command', command='kill @e[type=!minecraft:player]', ticks=2)
            # A previous waterlogged fixture can leave flowing water and move
            # the next player out of native interaction range. Rebuild the
            # disposable work area before any authored setup, never mid-test.
            from fixture_isolation import commands as isolate_fixture,SOURCE_SHA256 as ISOLATION_SHA256
            row['fixture_isolation']={'helper_sha256':ISOLATION_SHA256,'commands':isolate_fixture(origin),
                'scope':'Disposable world setup before entry actions; no menu state or interaction result is changed.'}
            for command in row['fixture_isolation']['commands']:act('command',command=command,ticks=3)
            for action in case['actions']:
                kw = dict(action); kind = kw.pop('type')
                if kind in {'provider', 'profile', 'native-template-probe'}: raise ValueError('Forced provider prohibited')
                if kind == 'fixture' and kw.get('block') == 'minecraft:air':
                    act('fixture', **{**kw, 'block': 'minecraft:stone'})
                    result = act('command', command='setblock %d %d %d minecraft:air' % tuple(origin), ticks=2)
                else:
                    kind = {'client-use-block-ray': 'open-ray', 'client-use-any-block-ray': 'use-block-ray', 'client-use-entity-ray': 'use-entity-ray'}.get(kind, kind)
                    result = act(kind, **kw)
            state = act('wait', ticks=6)['client']
            windows = rendered_windows(state)
            if not windows:
                # Keep evidence from this entry attempt, not the previous
                # case's final snapshot, when native opening is absent.
                entry_state=snapshot()
                row['entry_native_host']=entry_state.get('nativeHost',{})
                row['entry_player']=entry_state.get('player',{})
                row['entry_native_player_menu']=entry_state.get('nativePlayerMenu',{})
            if not windows and state.get('screen')=='none' and current=='bb-polymer-creative-browser':
                command=next(v['command'] for v in case['actions'] if v.get('command','').endswith('run polymer creative'))
                act('regression-command-info',command=command,ticks=2)
                info=json.loads((profile/'regression-command-info.json').read_text())
                if not info['fullyParsed']:
                    expect('obsolete native creative-browser command is not registered by this publisher',not snapshot()['menus'])
                    row.update(status='invalid-fixture-workflow',native_command_info=info,
                               coverage='Recorded historical command is not provided by the installed Polymer binary. No container functional pass claimed.')
                    save();print('END',current,row['status'],flush=True);continue
            entry_block=next((v['block'] for v in case['actions'] if v['type']=='fixture'),None)
            negative=next((v for v in entry_policies if (v['minecraft'],v['loader'])==(mc,loader)
                           and v['publisher_sha256'] in publisher_hashes and entry_block in v['blocks']),None)
            if negative:
                data=snapshot()
                expect('installed native block retains its reviewed non-container interaction',not windows and state.get('screen')=='none'
                       and not data['menus'] and data['nativeHost']['blockClass']==negative['block_class']
                       and data['nativeHost']['blockEntityClass']==negative.get('block_entity_class','none') and not data['nativeHost']['blockEntityIsMenuProvider'])
                row.update(status='passed',native_entry_policy=negative,coverage='Exact publisher negative entry: in-world block has no inventory menu. No container features claimed.')
                save();print('END',current,row['status'],flush=True);continue
            if case.get('scenario')=='equipment-launcher':
                from equipment_checks import exercise
                row['mod_id']=case['mod_id']
                exercise(case,row,profile,installed_mods,items,act,expect,snapshot,total,tagged_total,cursor)
                save();print('END',current,row['status'],flush=True);continue
            if case.get('scenario')=='quark-backpack':
                from equipment_checks import backpack
                row['mod_id']='quark';backpack(case,row,profile,act,expect,snapshot,total,tagged_total,cursor)
                save();print('END',current,row['status'],flush=True);continue
            if not windows and state.get('screen','').endswith('.WaystoneEditScreen'):
                expect('native waypoint editor remains outside the inventory desktop',not snapshot()['menus'])
                modifiers=next((v for v in state.get('widgets',[]) if v.get('message')=='Manage Modifiers' and v.get('active',True)),None)
                if modifiers:
                    act('regression-native-tap',x=modifiers['x']+modifiers['width']/2,y=modifiers['y']+modifiers['height']/2,ticks=18)
                    state=act('wait',ticks=5)['client'];windows=rendered_windows(state)
                    row['native_entry_transition']='Preserved native waypoint editor -> its actual Manage Modifiers inventory button.'
            protected={'exposure:album','exposure:lectern_album','waystones:waystone_selection','waystones:sharestone_selection',
                'waystones:warp_scroll_selection','waystones:warp_stone_selection','waystones:portstone_selection','waystones:admin_selection',
                'waystones:inventory_selection','waystones:waystone'}
            native_screen=state.get('screen','')
            from native_screen_contracts import dockyard_management,SOURCE_SHA256 as NATIVE_SCREEN_CONTRACT_SHA256
            if dockyard_management(native_screen,row.get('entry_native_player_menu',{})):
                row['native_management_contract']={'helper_sha256':NATIVE_SCREEN_CONTRACT_SHA256,
                    'native_menu':row['entry_native_player_menu'],'scope':'Native management screen has zero inventory slots.'}
            native_only=(native_screen.startswith('net.blay09.mods.waystones.') and native_screen.endswith(('WaystoneEditScreen','WaystoneSelectionScreen','SharestoneSelectionScreen','WaystoneListScreen'))
                         or native_screen=='wraith.fwaystones.screen.WaystoneBlockScreen'
                         or dockyard_management(native_screen,row.get('entry_native_player_menu',{}))
                         or native_screen.startswith('io.github.mortuusars.exposure.') and native_screen.endswith(('AlbumScreen','AlbumSigningScreen','LecternAlbumScreen')))
            if state.get('menuId') in protected or native_only:
                menu_id=state.get('menuId') or state.get('menuClass') or native_screen
                expect('reviewed native-only reader or selector is not converted',not windows and state.get('screen') not in ['none',None]
                       and not snapshot()['menus'])
                act('regression-native-key',key=256,ticks=5)
                closed=act('wait',ticks=3)['client']
                expect('native-only screen closes without creating a desktop source',closed.get('screen')!=native_screen and not snapshot()['menus'])
                entry=next(a for a in reversed(case['actions']) if a['type'] in ['open','client-use-block','open-ray','use-block-ray','client-use-item','use-item'])
                request=dict(entry);kind=request.pop('type');kind={'use-item':'client-use-item'}.get(kind,kind);act(kind,**request)
                reopened=act('wait',ticks=5)['client']
                expect('ordinary interaction reopens the same native-only selector',
                       reopened.get('screen')==native_screen and (reopened.get('menuId') or reopened.get('menuClass') or reopened.get('screen'))==menu_id
                       and not reopened.get('windows') and not snapshot()['menus'])
                row.update(status='passed',menu_ids=[menu_id],native_screen_roundtrip={'screen':native_screen,'menu':menu_id},
                           coverage='Reviewed native-only screen preservation, Escape and ordinary reopening; native editor controls not certified by this case.')
                save();print('END',current,row['status'],flush=True);continue
            if current=='bb-polymer-creative-browser':
                from polymer_checks import exercise as polymer_browser
                polymer_browser(row,profile,act,expect,snapshot,state)
                save();print('END',current,row['status'],flush=True);continue
            expect('natural opening owns a real window', bool(windows), screen=state.get('screen'))
            expect('native layout callback succeeds', all(not w.get('layoutError') for w in windows))
            row['menu_ids'] = [w.get('menuId', w.get('menu')) for w in windows]
            if any('SophisticatedHostedScreenDefinition' in w.get('definition','') for w in windows):
                act('regression-native-widget-catalog',ticks=2)
                observed=json.loads((profile/'regression-native-widget-catalog.json').read_text())
                for w in windows:
                    native=next((v.get('native_screen') for v in observed if v.get('source_session')==w.get('sessionId') and v.get('native_screen')),None)
                    if native:w['nativeScreen']=native
            row['native_screens'] = [w.get('nativeScreen') for w in windows]
            # Changing to creative while this publisher menu is open can replace
            # the desktop screen. Palette templates are a native creative tool;
            # retain the creative entry mode instead of reopening it mid-case.
            if 'wands:palette_menu' not in row['menu_ids']:
                act('command', command='gamemode survival @a', ticks=3)
            # Captured entry fixtures may contain demonstration supplies. The
            # test reserves these player cells before measuring conservation;
            # the held source (hotbar) and all publisher container contents stay.
            act('regression-seed-player',items=[{'slot':i,'item':'minecraft:air','count':0} for i in range(9,17)],ticks=4)
            initial=snapshot()
            expect('ordinary menu entry leaves no pending world-use identity',initial['inputPolicy']['pendingBlockTarget']=='none')
            # Some explicit API windows appear only in workspaceWindows. Their
            # registry identity comes from the authoritative owned source, not
            # optional renderer diagnostics.
            source_types={m['session']:m.get('menuType') for m in initial['menus']}
            row['menu_ids']=[source_types.get(w.get('sessionId'),w.get('menuId',w.get('menu'))) for w in windows]
            if case.get('expected_native_menu'):
                data=initial
                from entity_adapter_checks import aircraft_source_preserved,CLASS as aircraft_adapter,SOURCE_SHA256 as adapter_sha256
                row.setdefault('test_helper_sha256',{})['entity_adapter_checks']=adapter_sha256
                adapted=aircraft_source_preserved(data['menus'],case['expected_native_menu'],case.get('expected_nonplayer_slot_count',0))
                expect('ordinary entry retains the publisher menu or its reviewed entity adapter',any(m['type']==case['expected_native_menu'] for m in data['menus']) or adapted,
                       expected=case['expected_native_menu'],actual=[m['type'] for m in data['menus']])
                if adapted:
                    row['approved_entity_adapter']=aircraft_adapter
                    row['native_adapter_sources']=[m['nativeAdapter'] for m in data['menus'] if m['type']==aircraft_adapter]
            # Candidate selection queries native mayPlace/mayPickup; no slot gets
            # force-seeded. Inventory itself is the only seeded item source.
            row['item_serialization_contract']=initial.get('itemDataFormat','tag-only-legacy-or-unrecorded')
            row['slot_access_contract']=initial.get('slotAccessContract','vanilla-slot-list-only')
            row['native_menus']=[m['type'] for m in initial['menus']]
            row['native_menu_types']=[m.get('menuType') for m in initial['menus']]
            expect('owned menu remains present in the tested player mode',bool(initial['menus']))
            from geckolib_checks import applicable as geckolib_applicable
            if geckolib_applicable(mc,loader,installed_mods,row['native_menus']):
                from geckolib_checks import exercise as geckolib_sync
                geckolib_sync(row,profile,initial,act,expect,snapshot)
            elif mc=='1.20.1' and loader=='fabric' and 'net.orcinus.galosphere.client.gui.CombustionTableMenu' in row['native_menus']:
                row['native_geckolib_comparison_not_applicable']='GeckoLib is absent from the actual installed publisher set; ordinary Galosphere menu interactions still run.'
            virtual_cells=[s for m in initial['menus'] for s in m['slots'] if s['virtual']]
            row['virtual_policy_query_contract']=all(s.get('policyQueried') is False for s in virtual_cells)
            if virtual_cells:
                expect('observer leaves virtual-slot policy callbacks untouched',row['virtual_policy_query_contract'])
            if any(m['type']=='com.telepathicgrunt.the_bumblezone.menus.BuzzingBriefcaseMenu' for m in initial['menus']):
                from bumblezone_checks import exercise as bee_care
                bee_care(row,profile,act,expect,snapshot)
                save();print('END',current,row['status'],flush=True);continue
            if any(m['type']=='io.github.lucaargolo.kibe.blocks.miscellaneous.BlockGeneratorScreenHandler' for m in initial['menus']):
                from kibe_checks import generator
                generator(row,initial,act,expect,snapshot,management,total,tagged_total,cursor)
                save();print('END',current,row['status'],flush=True);continue
            if any(m.get('menuType') in {'kibe:trash_can','kibe:pocket_trash_can'} or m['type'].endswith(('.trashcan.TrashCanScreenHandler','.trashcan.PocketTrashCanScreenHandler')) for m in initial['menus']):
                from kibe_checks import trash_can
                trash_can(row,initial,act,expect,snapshot,management,total,tagged_total,cursor)
                save();print('END',current,row['status'],flush=True);continue
            if initial['nativeHost'].get('blockEntityClass')=='techreborn.blockentity.storage.item.StorageUnitBaseBlockEntity':
                from techreborn_checks import storage_unit
                storage_unit(row,initial,blocks,act,expect,snapshot,management,total,tagged_total,cursor)
                save();print('END',current,row['status'],flush=True);continue
            if initial['nativeHost'].get('blockEntityClass','').startswith('moze_intel.projecte.') and any(v in initial['nativeHost']['blockEntityClass'] for v in ['CollectorMK','RelayMK']):
                from projecte_checks import collector
                collector(row,initial,items,blocks,act,expect,snapshot,management,total,tagged_total,cursor)
                save();print('END',current,row['status'],flush=True);continue
            if identity(current)=='ae2-terminal' and any(m['type']=='appeng.menu.me.common.MEStorageMenu' for m in initial['menus']):
                from ae2_repository_checks import exercise as repository_exercise
                repository_exercise(row,profile,initial,blocks,act,expect,snapshot,total,tagged_total,cursor)
                save();print('END',current,row['status'],flush=True);continue
            if case.get('entry_mode')=='native-connected-storage-network':
                from ssn_repository_checks import exercise as ssn_repository
                ssn_repository(row,profile,initial,blocks,act,expect,snapshot,management,total,tagged_total,cursor)
                save();print('END',current,row['status'],flush=True);continue
            emitter=next((m for m in initial['menus'] if m['type'].startswith('com.tom.storagemod.') and 'LevelEmitter' in m['type']),None)
            if emitter:
                def emitter_settings():
                    act('regression-native-settings',session=emitter['session'],fields=['count','lessThan'],ticks=2)
                    return json.loads((profile/'regression-native-settings.json').read_text())
                start=emitter_settings()['count'];expected=start
                for sign,y in [(1,11),(-1,51)]:
                    for x,delta in zip([24,51,84,121],[1,10,100,1000]):
                        act('regression-content-tap',session=emitter['session'],x=x,y=y,ticks=8)
                        expected=max(0,expected+sign*delta)
                        observed=emitter_settings()['count']
                        expect('native emitter '+str(sign*delta)+' button reaches authoritative count',observed==expected,expected=expected,actual=observed)
                expect('both native amount banks restore the original threshold',emitter_settings()['count']==start)
                act('regression-content-tap',session=emitter['session'],x=80,y=31,ticks=3)
                act('key',key=65,modifiers=2,ticks=1);act('text',text='4242',ticks=12);act('key',key=257,ticks=3)
                expect('multi-digit text entry reaches the final authoritative emitter amount',emitter_settings()['count']==4242)
                mode=emitter_settings()['lessThan']
                act('regression-content-tap',session=emitter['session'],space='window',x=-10,y=13,ticks=8)
                expect('attached native threshold toggle reaches the correct source',emitter_settings()['lessThan']!=mode)
                row['native_controls_checked']='Both ±1/10/100/1000 banks, multi-digit text and Enter, attached above/below toggle; native server fields asserted.'
            wands=next((m for m in initial['menus'] if m['type'] in ['net.nicguzzo.wands.menues.MagicBagMenu','net.nicguzzo.wands.menues.PaletteMenu']),None)
            if wands:
                sid=wands['session'];bag=wands['type'].endswith('.MagicBagMenu');item='minecraft:oak_planks';amount=7 if bag else 1
                slot=next(s['index'] for s in wands['slots'] if not s['player'] and s['active'])
                before=total(snapshot(item),item)
                act('regression-seed-player',items=[{'slot':9,'item':item,'count':amount,'snbt':'{siu_regression_marker:42}'}],ticks=4)
                signature=next(v['signature'] for v in snapshot(item)['inventory'] if v['index']==9)
                act('regression-player-click',slot=9,ticks=4);act('regression-slot-click',session=sid,slot=slot,ticks=4)
                data=snapshot(item);menu=next(m for m in data['menus'] if m['session']==sid)
                if bag:
                    expect('native magic-bag counter receives the whole stack behind its one-item display',menu['nativeBalance']['count']==amount
                           and cursor(data,item,0) and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
                    partner=next((v for v in ['netherchested:nether_chest','sophisticatedstorage:barrel'] if v in blocks),'minecraft:barrel')
                    from partner_checks import open_partner
                    data,sid,other=open_partner(row,data,sid,partner,act,expect,snapshot,nearPlayer=True,dx=-1,dz=1)
                    expect('opening storage retains the native magic-bag source',any(m['session']==sid for m in data['menus']) and len(data['menus'])>=2)
                    dest=next(s['index'] for s in other['slots'] if not s['player'] and not s['virtual'] and s['active'] and s.get('visible',True) and s['mayPlace'] and not s['count'])
                    act('regression-slot-click',session=sid,slot=slot,button=1,ticks=4)
                    expect('native bag withdraws its stored quantity rather than the display count',cursor(snapshot(item),item,amount))
                    act('regression-slot-click',session=other['session'],slot=dest,ticks=4);data=snapshot(item)
                    expect('bag-to-storage transfer preserves exact units and data',cursor(data,item,0) and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
                    act('regression-slot-click',session=other['session'],slot=dest,ticks=4);act('regression-slot-click',session=sid,slot=slot,ticks=4)
                    data=snapshot(item)
                    expect('return transfer restores the native bag balance exactly once',next(m for m in data['menus'] if m['session']==sid)['nativeBalance']['count']==amount
                           and cursor(data,item,0) and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
                    act('regression-close',session=other['session'],ticks=4);act('regression-slot-click',session=sid,slot=slot,button=1,ticks=4)
                    act('regression-close',session=sid,ticks=4);data=snapshot(item)
                    expect('closing the bag with its withdrawn stack preserves all units',total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
                    row.update(status='passed',coverage='Native stored-quantity ledger, one-item display, deposit/withdrawal, cross-storage transfer, metadata and carried-stack source close.',cross_mod_partner=partner)
                else:
                    expect('creative palette accepts a native block template',menu['slots'][slot]['item']==item and cursor(data,item,0))
                    act('regression-slot-click',session=sid,slot=slot,ticks=4);data=snapshot(item)
                    expect('creative palette clones one block without removing its template',cursor(data,item,1) and next(m for m in data['menus'] if m['session']==sid)['slots'][slot]['item']==item)
                    act('regression-player-click',slot=10,ticks=4);act('regression-slot-click',session=sid,slot=slot,button=1,ticks=4);data=snapshot(item)
                    expect('native right click clears only the palette template',next(m for m in data['menus'] if m['session']==sid)['slots'][slot]['count']==0
                           and cursor(data,item,0) and data['inventory'][10]['item']==item and data['inventory'][10]['count']==1)
                    row.update(status='passed',coverage='Creative native palette template insertion, one-block cloning and right-click template removal; not physical chest storage.')
                save();print('END',current,row['status'],flush=True);continue
            selected = None
            excluded = set()
            if initial['nativeHost'].get('blockEntityClass')=='techreborn.blockentity.generator.basic.SolidFuelGeneratorBlockEntity':
                from techreborn_checks import prime_solid_fuel
                selected=prime_solid_fuel(row,initial,act,expect,snapshot,total)
            if 'powahBurn' in initial['nativeHost']:
                from powah_checks import prime_furnator
                selected=prime_furnator(row,initial,act,expect,snapshot,total,cursor)
            if 'powahReactor' in initial['nativeHost']:
                from powah_checks import prime_reactor_carbon
                selected=prime_reactor_carbon(row,initial,act,expect,snapshot,total,cursor)
            if 'additionzBurn' in initial['nativeHost']:
                from additionz_checks import prime_chunk_loader
                selected=prime_chunk_loader(row,initial,act,expect,snapshot,total,cursor)
            conditioning=next((m for m in initial['menus'] if m['type']=='io.github.lucaargolo.seasonsextras.screenhandlers.AirConditioningScreenHandler'),None)
            if conditioning:
                # The three right-hand cells burn native fuel immediately.
                # Test that conversion explicitly, then use the nine reserve
                # cells for strict no-consumption transfer assertions.
                excluded.update([9,10,11]);sid=conditioning['session']
                fuel=next((v for v in ['minecraft:snow','minecraft:coal'] if v in items and
                           next(m for m in snapshot(v)['menus'] if m['session']==sid)['slots'][11]['mayPlace']),None)
                expect('native conditioning module has a supported fuel',fuel is not None)
                act('regression-seed-player',items=[{'slot':9,'item':fuel,'count':7,'snbt':'{siu_regression_marker:42}'}],ticks=4)
                signature=next(v['signature'] for v in snapshot(fuel)['inventory'] if v['index']==9)
                act('regression-player-click',slot=9,ticks=4);act('regression-slot-click',session=sid,slot=11,ticks=4)
                data=snapshot(fuel);burns=data['nativeHost'].get('airConditioningBurns',[])
                expect('native conditioning module consumes one fuel and starts its burn',len(burns)==3 and burns[2]['remaining']>0
                       and burns[2]['total']>=burns[2]['remaining'] and total(data,fuel)==6 and tagged_total(data,fuel,signature)==6)
                act('regression-slot-click',session=sid,slot=11,ticks=4);act('regression-player-click',slot=16,ticks=4)
                expect('unconsumed conditioning fuel is recoverable exactly once',snapshot(fuel)['inventory'][16]['count']==6 and cursor(snapshot(fuel),fuel,0))
                enabled=[v['enabled'] for v in snapshot(fuel)['nativeHost']['airConditioningBurns']]
                for module,active in enumerate(enabled):
                    if active:act('regression-conditioning-button',session=sid,button=module,ticks=6)
                disabled=snapshot(fuel)['nativeHost']['airConditioningBurns']
                expect('real native module switches disable reserve auto-feeding',not any(v['enabled'] for v in disabled))
                row['conditioning_original_enabled']=enabled
                row['native_recipe_checked']='One native fuel burned; six tagged units recovered. Native module switches disable reserve auto-feeding for strict storage transfers, then restore the original module settings.'
            if 'appeng.menu.implementations.VibrationChamberMenu' in row['native_menus']:
                # Native AE2 ramps fuel consumption to eight fuel ticks per
                # game tick. A single coal expires during the compound test;
                # prime one ordinary coal block instead of treating the next
                # legitimate fuel burn as lost transfer data.
                data=snapshot('minecraft:coal_block');menu=data['menus'][-1];sid=menu['session']
                fuel=next(s['index'] for s in menu['slots'] if not s['player'] and not s['virtual'] and s['active'] and s.get('visible',True) and s['mayPlace'] and not s['count'])
                act('regression-seed-player',items=[{'slot':9,'item':'minecraft:coal_block','count':1}],ticks=4)
                act('regression-player-click',slot=9,ticks=4);act('regression-slot-click',session=sid,slot=fuel,ticks=4)
                act('wait',ticks=20);data=snapshot('minecraft:coal_block')
                expect('native vibration chamber consumes one fuel and starts its native burn',total(data,'minecraft:coal_block')==0
                       and cursor(data,'minecraft:coal_block',0) and data['nativeHost'].get('getRemainingFuelTicks',0)>8000,native_host=data['nativeHost'])
                row['native_recipe_checked']='One native coal block consumed; long positive burn supports strict tagged-coal transfer checks while AE2 ramps its burn rate.'
                selected=(sid,fuel,'minecraft:coal',7)
            if 'appeng.menu.implementations.CellWorkbenchMenu' in row['native_menus'] and 'ae2:item_storage_cell_1k' in items:
                data=snapshot('ae2:item_storage_cell_1k');menu=data['menus'][-1]
                cell=next(s['index'] for s in menu['slots'] if not s['player'] and not s['virtual'] and s['active'] and s.get('visible',True) and s['mayPlace'] and not s['count'])
                selected=(menu['session'],cell,'ae2:item_storage_cell_1k',1)
            if 'appeng.menu.implementations.CondenserMenu' in row['native_menus']:
                data=snapshot();sid=data['menus'][-1]['session'];component='ae2:cell_component_1k'
                expect('publisher condenser component is registered',component in items)
                act('regression-seed-player',items=[{'slot':9,'item':component,'count':1}],ticks=4)
                act('regression-player-click',slot=9,ticks=4);act('regression-slot-click',session=sid,slot=2,ticks=5)
                live=act('wait',ticks=4)['client']
                mode=next(v for w in live['windows'] for v in w.get('nativeWidgets',[]) if v['type'].endswith('ServerSettingToggleButton') and v['active'] and v['visible'])
                act('regression-tap',session=sid,x=mode['x']+8,y=mode['y']+8,ticks=8)
                data=snapshot();expect('native condenser has real storage capacity',data['nativeHost'].get('getStorage',0)>0)
                act('regression-seed-player',items=[{'slot':i,'item':'minecraft:diamond','count':64} for i in range(9,13)],ticks=4)
                for source in range(9,13):
                    act('regression-player-click',slot=source,ticks=4);act('regression-slot-click',session=sid,slot=0,ticks=5)
                data=snapshot();native=next(m for m in data['menus'] if m['session']==sid)
                expect('native condenser converts 256 inputs into one matter ball',cursor(data,'minecraft:diamond',0)
                       and total(data,'minecraft:diamond')==0 and native['slots'][1]['item']=='ae2:matter_ball' and native['slots'][1]['count']==1,
                       native_host=data['nativeHost'],output=native['slots'][1])
                act('regression-slot-click',session=sid,slot=1,ticks=4);act('regression-player-click',slot=15,ticks=4)
                expect('native conversion output reaches separate inventory exactly once',total(snapshot(),'ae2:matter_ball')==1)
                act('regression-slot-click',session=sid,slot=2,ticks=4);act('regression-player-click',slot=16,ticks=4)
                row['native_recipe_checked']='256 diamond inputs -> one matter ball, real component and native mode control; result withdrawn to player inventory'
                selected=(sid,2,component,1);excluded.add(0)
            for item in ([] if selected else ['minecraft:diamond', 'minecraft:oak_log', 'minecraft:iron_ingot', 'minecraft:coal', 'minecraft:paper', 'minecraft:apple', 'minecraft:gold_ingot', 'minecraft:raw_iron', 'minecraft:iron_ore', 'minecraft:book', 'minecraft:wheat', 'minecraft:oak_planks', 'botania:white_mystical_flower', 'spell_engine:spell_book']):
                if item not in items: continue
                data = snapshot(item)
                for menu in reversed(data['menus']):
                    allowed = [s for s in menu['slots'] if s['index'] not in excluded and not s['player'] and not s['virtual'] and not s.get('requiresTomFilter') and s['active'] and s.get('visible',True) and s['mayPlace'] and s['count'] == 0 and s['max'] >= 1]
                    if allowed:
                        selected = (menu['session'], allowed[0]['index'], item, min(7, allowed[0]['max'])); break
                if selected: break
            if not selected:
                act('regression-discover-input', snbt='{siu_regression_marker:42}', ticks=2)
                candidate = json.loads((profile / 'regression-candidate.json').read_text())
                if not candidate.get('absent'):
                    selected = (candidate['session'], candidate['slot'], candidate['item'], candidate['amount'])
            if not selected:
                ghosts = [(m['session'],s['index']) for m in data['menus'] for s in m['slots']
                          if s['virtual'] and s['active'] and (s['type'].startswith('com.tom.storagemod.') and s['type'].endswith('.PhantomSlot')
                             or m['type']=='dev.latvian.mods.itemfilters.gui.InventoryFilterMenu')]
                if ghosts:
                    sid, ghost = ghosts[0]; item='minecraft:diamond'; amount=7
                    before=total(snapshot(item),item)
                    act('regression-seed-player',items=[{'slot':9,'item':item,'count':amount,'snbt':'{siu_regression_marker:42}'}],ticks=4)
                    signature=next(v['signature'] for v in snapshot(item)['inventory'] if v['index']==9)
                    act('regression-player-click',slot=9,ticks=4)
                    act('regression-slot-click',session=sid,slot=ghost,ticks=6)
                    data=snapshot(item); copy=next(m for m in data['menus'] if m['session']==sid)['slots'][ghost]
                    expect('native phantom click copies the filter and preserves the real cursor',copy['item']==item and copy['count']==1
                           and cursor(data,item,amount) and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
                    act('regression-player-click',slot=10,ticks=4)
                    act('regression-slot-click',session=sid,slot=ghost,ticks=6)
                    data=snapshot(item); copy=next(m for m in data['menus'] if m['session']==sid)['slots'][ghost]
                    expect('empty-cursor native click clears the phantom without losing real items',copy['count']==0
                           and cursor(data,item,0) and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
                    row.update(status='passed',coverage='Native ghost filter copy/clear, unchanged physical cursor, exact item conservation.')
                    save();print('END',current,row['status'],flush=True);continue
            if not selected:
                transition=next((v for v in reversed(case['actions']) if v['type']=='regression-native-widget' and v.get('parent_menu')),None)
                if transition:
                    state=act('wait',ticks=3)['client'];title=transition.get('parent_title','')
                    widgets=[v for w in state.get('windows',[]) for v in w.get('nativeWidgets',[]) if v.get('visible',True) and v.get('active',True)]
                    back=next((v for v in widgets if v['type'].endswith(('TabButton','ActionButton')) and
                               ((title and v.get('message')==title) or v.get('message','').strip().lower() in ['back','return'])),None)
                    tabs=[v for v in widgets if v['type'].endswith('.TabButton') and v.get('message')!=transition.get('message')]
                    if back is None and len(tabs)==1:back=tabs[0]
                    if back:
                        item='minecraft:diamond';amount=7;before=total(snapshot(item),item)
                        act('regression-seed-player',items=[{'slot':9,'item':item,'count':amount,'snbt':'{siu_regression_marker:42}'}],ticks=4)
                        signature=next(v['signature'] for v in snapshot(item)['inventory'] if v['index']==9)
                        act('regression-player-click',slot=9,ticks=4)
                        child=next(m['session'] for m in data['menus'] if m['type']==case['expected_native_menu'])
                        act('regression-native-widget',widget_type=back['type'],message=back.get('message',''),ordinal=0,ticks=18)
                        data=snapshot(item)
                        expect('native child return reopens its original parent menu',any(m['type']==transition['parent_menu'] for m in data['menus']),expected=transition['parent_menu'],actual=[m['type'] for m in data['menus']])
                        if transition.get('parent_screen'):
                            state=act('wait',ticks=3)['client']
                            expect('native return restores the original parent content',any(w.get('nativeScreen')==transition['parent_screen'] for w in state.get('windows',[])),expected=transition['parent_screen'])
                        expect('native child transition preserves the shared carried stack and metadata',cursor(data,item,amount)
                               and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
                        act('regression-player-click',slot=10,ticks=4)
                        expect('returned parent deposits the cursor once in separate inventory',cursor(snapshot(item),item,0) and total(snapshot(item),item)==before+amount)
                        row.update(status='passed',native_child_return_checked=True,native_child_parent_screen_checked=bool(transition.get('parent_screen')),coverage='Native semantic child button, publisher menu identity, native return tab while carrying a tagged stack, shared-cursor conservation and separate-inventory deposit; original parent content also checked when recorded in the native evidence. Dialog settings not exhaustively tested.')
                        save();print('END',current,row['status'],flush=True);continue
                    if transition.get('widget_type','').endswith(('.SettingsWidget','.StorageSettingsTab')):
                        item='minecraft:diamond';amount=7;before=total(snapshot(item),item)
                        act('regression-seed-player',items=[{'slot':9,'item':item,'count':amount,'snbt':'{siu_regression_marker:42}'}],ticks=4)
                        signature=snapshot(item)['inventory'][9]['signature']
                        act('regression-player-click',slot=9,ticks=4)
                        child=next(m['session'] for m in data['menus'] if m['type']==case['expected_native_menu'])
                        act('regression-close',session=child,ticks=8)
                        closed=snapshot(item)
                        expect('closing native settings preserves the complete shared carried stack',cursor(closed,item,amount)
                               and total(closed,item)==before+amount and tagged_total(closed,item,signature)==amount)
                        if not any(m['type']==transition['parent_menu'] for m in closed['menus']):
                            entry=next((v for v in reversed(case['actions'][:case['actions'].index(transition)]) if v['type'] in {
                                'client-use-block','client-use-item','regression-open','open-ray','use-block-ray','open','use-item'}),None)
                            expect('settings parent has a recorded ordinary reopening action',entry is not None)
                            request=dict(entry);kind=request.pop('type');kind={'open':'client-use-block','use-item':'client-use-item'}.get(kind,kind)
                            request.pop('capture',None);request['ticks']=18;act(kind,**request)
                        returned=snapshot(item)
                        expect('ordinary native settings close and reopen restores the publisher parent menu',any(m['type']==transition['parent_menu'] for m in returned['menus']))
                        expect('settings lifecycle does not consume or duplicate the cursor data',cursor(returned,item,amount)
                               and total(returned,item)==before+amount and tagged_total(returned,item,signature)==amount)
                        act('regression-player-click',slot=10,ticks=4)
                        expect('reopened parent returns the settings cursor once to separate inventory',cursor(snapshot(item),item,0)
                               and snapshot(item)['inventory'][10]['count']==amount and tagged_total(snapshot(item),item,signature)==amount)
                        row.update(status='passed',native_settings_lifecycle_checked=True,
                                   coverage='Observed native custom settings button, exact child menu entry, real child close with a tagged carried stack, ordinary parent reopen when needed, exact metadata and separate-inventory return. Individual settings toggles are not certified.')
                        save();print('END',current,row['status'],flush=True);continue
                row.update(status='entry-and-policy-only', reason='No empty physical input accepts the standard test items; requires specialized item/recipe scenario.')
                evidence=profile/'regression-policy-evidence';evidence.mkdir(exist_ok=True)
                (evidence/(current+'.json')).write_text(json.dumps(data,indent=2))
                row['policy_evidence']=str((evidence/(current+'.json')).relative_to(profile))
                save();print('END',current,row['status'],flush=True);continue
            sid, slot, item, amount = selected
            row.update(session=sid, slot=slot, item=item, amount=amount)
            if require_portable_lock and current in {'appbot-portable-mana-cell','megacells-portable-mana-cell','atlas-merequester-wireless-terminal','ae2-wireless-crafting-terminal'}:
                source=snapshot(item)
                expect('portable native source is protected in the separate player inventory',source['inputPolicy']['selectedSourceLocked'])
                host=source['inventory'][0]
                row['portable_host_energy']={'item':host['item'],'before':portable_energy(host)}
                expect('portable source fixture has an empty cursor and a held host',source['cursor']['count']==0 and host['count']==1)
                act('regression-player-click',slot=36,ticks=4)
                protected=snapshot(item)
                expect('real player-window pickup cannot remove an open native host',protected['cursor']['count']==0
                       and protected['inventory'][0]['item']==host['item'] and protected['inventory'][0]['count']==host['count']
                       and protected['inputPolicy']['selectedSourceLocked'] and any(m['session']==sid for m in protected['menus']))
            native_type=next(m['type'] for m in snapshot(item)['menus'] if m['session']==sid)
            if native_type not in rejected_inputs:
                for denied in ['minecraft:diamond','minecraft:stone','minecraft:apple','minecraft:wheat']:
                    if denied==item or denied not in items:continue
                    policy=snapshot(denied);cell=next(m for m in policy['menus'] if m['session']==sid)['slots'][slot]
                    if cell['mayPlace']:continue
                    source=next((v['index'] for v in policy['inventory'] if 17<=v['index']<36 and not v['count']),None)
                    expect('rejected-input fixture has an empty separate inventory cell',source is not None)
                    before_denied=total(policy,denied)
                    act('regression-seed-player',items=[{'slot':source,'item':denied,'count':3,'snbt':'{siu_regression_marker:43}'}],ticks=4)
                    signature_denied=snapshot(denied)['inventory'][source]['signature']
                    act('regression-player-click',slot=source,ticks=4);act('regression-slot-click',session=sid,slot=slot,ticks=4)
                    denied_state=snapshot(denied);after_cell=next(m for m in denied_state['menus'] if m['session']==sid)['slots'][slot]
                    expect('native slot rejects a forbidden item without consuming the cursor',cursor(denied_state,denied,3)
                           and after_cell['count']==cell['count'] and after_cell['item']==cell['item']
                           and total(denied_state,denied)==before_denied+3 and tagged_total(denied_state,denied,signature_denied)==3)
                    act('regression-player-click',slot=source,ticks=4)
                    expect('rejected native input returns once to separate inventory',cursor(snapshot(denied),denied,0)
                           and snapshot(denied)['inventory'][source]['count']==3)
                    row['native_rejected_input_checked']={'slot':slot,'item':denied};rejected_inputs.add(native_type);break
            before_seed=snapshot(item)
            before = total(before_seed, item)
            act('regression-seed-player', items=[{'slot': 9, 'item': item, 'count': amount, 'snbt': '{siu_regression_marker:42}'}], ticks=6)
            seeded = snapshot(item)
            signature = next(v['signature'] for v in seeded['inventory'] if v['index'] == 9)
            expected_tagged=after_seed(before_seed,item,signature,amount)
            row['preexisting_tagged_count']=expected_tagged-amount
            row['expected_tagged_count']=expected_tagged
            expect('source stack carries the regression data marker', 'siu_regression_marker' in signature)
            expect('fixture adds the exact native-capacity stack', total(seeded, item) == before + amount)
            act('regression-player-click', slot=9, ticks=8)
            expect('separate inventory supplies shared cursor', cursor(snapshot(item), item, amount))
            act('regression-slot-click', session=sid, slot=slot, ticks=10)
            data = snapshot(item)
            target = next(m for m in data['menus'] if m['session'] == sid)['slots'][slot]
            expect('native input receives exact stack', target['item'] == item and target['count'] == amount and cursor(data, item, 0))
            expect('insertion conserves physical items', total(data, item) == before + amount)
            editing=next(m for m in data['menus'] if m['session']==sid).get('nativeEditingOriginal')
            if editing:
                expect('native forge retains the complete original while editing a working weapon',editing['item']==item and editing['count']==amount
                       and editing['signature']==signature and re.search(r'\bsiu_regression_marker["\']?\s*:\s*42\b',target['signature']))
                row['native_editing_semantics']='Simply Swords temporarily separates awakening/gem editing data. The native original is checked while inserted; the complete item signature is checked after withdrawal.'
            else:
                expect('native insertion preserves stack data', tagged_total(data, item, signature) == expected_tagged)
            act('regression-slot-click', session=sid, slot=slot, ticks=10)
            expect('native input returns exact stack', cursor(snapshot(item), item, amount))
            act('regression-player-click', slot=10, ticks=8)
            data = snapshot(item)
            expect('round trip returns items once', cursor(data, item, 0) and total(data, item) == before + amount)
            expect('native withdrawal restores the full item metadata',tagged_total(data,item,signature)==expected_tagged)
            inputs = [s['index'] for s in next(m for m in data['menus'] if m['session'] == sid)['slots']
                      if s['index'] not in excluded and not s['player'] and not s['virtual'] and s['active'] and s.get('visible',True) and s['mayPlace'] and not s['count'] and s['max'] >= amount]
            row['sampled_input_slots'] = [slot]
            for extra_slot in dict.fromkeys([inputs[len(inputs)//2], inputs[-1]] if inputs else []):
                if extra_slot == slot: continue
                act('regression-player-click', slot=10, ticks=4)
                act('regression-slot-click', session=sid, slot=extra_slot, ticks=5)
                data = snapshot(item)
                # The publisher compacts its complete crate backing inventory
                # in recalculate() after every change, even for normal clicks.
                repacking=native_type.endswith('.CrateMenu') and native_type.startswith(('org.violetmoon.quark.','vazkii.quark.'))
                matter_repacking=native_type in ['moze_intel.projecte.gameObjs.container.DMFurnaceContainer','moze_intel.projecte.gameObjs.container.RMFurnaceContainer'] and extra_slot!=0
                # ProjectE compacts the native smelting-input handler every
                # server tick. Menu slot 1 is that handler's first cell; slot
                # 0 is the separate fuel handler and is never remapped here.
                withdrawal_slot=0 if repacking else 1 if matter_repacking else extra_slot
                target = next(m for m in data['menus'] if m['session'] == sid)['slots'][withdrawal_slot]
                expect('additional native input accepts tagged stack', target['item'] == item and target['count'] == amount
                       and cursor(data,item,0) and tagged_total(data,item,signature) == expected_tagged, slot=extra_slot)
                if repacking or matter_repacking:
                    row.setdefault('native_repacking_inputs',[]).append({'clicked':extra_slot,'native_compacted_slot':withdrawal_slot})
                if repacking:
                    expect('native crate repacking retains its exact quantity ledger',data['nativeHost']['quarkCrateTotal']==amount
                           and total(data,item)==before+amount)
                act('regression-slot-click', session=sid, slot=withdrawal_slot, ticks=4)
                act('regression-player-click', slot=10, ticks=4)
                data = snapshot(item)
                expect('additional input round trip conserves stack and data', cursor(data,item,0)
                       and total(data,item) == before+amount and tagged_total(data,item,signature) == expected_tagged, slot=extra_slot)
                row['sampled_input_slots'].append(extra_slot)
            family = next(m['type'] for m in data['menus'] if m['session'] == sid)
            if family in exercised and 'portable_host_energy' not in row:
                row.update(status='passed', coverage='Natural opening, native slot policy, tagged insertion/withdrawal and conservation. Compound gestures covered by another case of this native menu class.')
                save(); print('END', current, row['status'], flush=True); continue
            row['coverage'] = 'Native round trip, metadata, unlock/header drag, another menu with equal slot indices, sorting and independent close lifetime.'
            if amount > 1:
                act('regression-player-click', slot=10, ticks=4)
                act('regression-slot-click', session=sid, slot=slot, button=1, ticks=4)
                data = snapshot(item); target = next(m for m in data['menus'] if m['session'] == sid)['slots'][slot]
                expect('right-click deposits exactly one tagged item', target['count'] == 1 and cursor(data,item,amount-1)
                       and total(data,item) == before+amount and tagged_total(data,item,signature) == expected_tagged)
                act('regression-slot-click', session=sid, slot=slot, ticks=4)
                act('regression-slot-click', session=sid, slot=slot, ticks=4)
                act('regression-player-click', slot=10, ticks=4)
                data = snapshot(item)
                expect('split stack recombines without duplicate items or data loss', cursor(data,item,0)
                       and total(data,item) == before+amount and tagged_total(data,item,signature) == expected_tagged)
            # Move with real header drag, then repeat both directions. This
            # catches stale native coordinates after retained window movement.
            original_window = management(sid)
            original = original_window['bounds']
            lockable=original_window['available']['LOCK'] or original_window['available']['UNLOCK']
            if lockable:
                if not original_window['locked']:
                    act('regression-control',session=sid,control='LOCK',ticks=4)
                    original=management(sid)['bounds']
                act('regression-move',session=sid,dx=-25,dy=15,ticks=4)
                expect('lock prevents a real header drag',management(sid)['bounds']==original)
                act('regression-control', session=sid, control='LOCK', ticks=4)
            else:
                row['intentional_management_limit']='Native integration does not expose lock/unlock; test its declared movement policy.'
            movable=management(sid)['available']['MOVE']
            act('regression-move', session=sid, dx=-50, dy=30, ticks=8)
            bounds = management(sid)['bounds']
            row['movement_changed_bounds'] = original != bounds
            expect('real header drag follows the declared movement policy',(original!=bounds)==movable)
            act('regression-player-click', slot=10, ticks=6)
            act('regression-slot-click', session=sid, slot=slot, ticks=8)
            expect('moved window input uses new coordinates', cursor(snapshot(item), item, 0))
            act('regression-slot-click', session=sid, slot=slot, ticks=8)
            act('regression-player-click', slot=11, ticks=8)
            data = snapshot(item)
            expect('moved window round trip conserves stack', cursor(data, item, 0) and total(data, item) == before + amount)
            # Open a second publisher's real storage beside the first source.
            # Equal slot indices in different sessions must never alias.
            owner = row['menu_ids'][0].split(':')[0] if row['menu_ids'][0] else ''
            partner = next((b for b in ['netherchested:nether_chest', 'sophisticatedstorage:barrel', 'supplementaries:sack']
                            if b in blocks and b.split(':')[0] != owner), 'minecraft:barrel')
            from partner_checks import open_partner
            data,sid,other=open_partner(row,data,sid,partner,act,expect,snapshot,nearPlayer=True,dx=-1,dz=1)
            expect('opening second source retains first owned session', any(m['session'] == sid for m in data['menus']) and len(data['menus']) >= 2)
            accepting=lambda menu:[s for s in menu['slots'] if not s['player'] and not s['virtual'] and s['active'] and s.get('visible',True) and s['mayPlace'] and not s['count']]
            if not accepting(other):
                # Native sacks can blacklist container-like items, including
                # Create's packages. Verify a real refused insertion and retain
                # that pair's result, then use ordinary vanilla storage for the
                # shared-cursor lifetime scenario. Never pretend it accepted.
                expect('vanilla fallback is not needed for an unexplained refusal',partner!='minecraft:barrel')
                denied=next(s for s in other['slots'] if not s['player'] and not s['virtual'] and s['active'] and s.get('visible',True))
                act('regression-player-click',slot=11,ticks=4)
                act('regression-slot-click',session=other['session'],slot=denied['index'],ticks=4)
                refused=snapshot(item)
                expect('native secondary storage rejects its blacklisted item without consuming or duplicating it',
                       cursor(refused,item,amount) and total(refused,item)==before+amount and tagged_total(refused,item,signature)==expected_tagged
                       and all(not s['count'] for s in next(m for m in refused['menus'] if m['session']==other['session'])['slots'] if not s['player']))
                row.setdefault('native_rejected_cross_mod_partners',[]).append({'block':partner,'menu':other['type'],'item':item,'slot':denied['index'],'checked_by_real_click':True})
                act('regression-player-click',slot=11,ticks=4)
                act('regression-close',session=other['session'],ticks=6)
                partner='minecraft:barrel'
                data,sid,other=open_partner(row,snapshot(item),sid,partner,act,expect,snapshot,nearPlayer=True,dx=-1,dz=1)
                expect('native refusal fallback retains the original source and item ownership',any(m['session']==sid for m in data['menus'])
                       and cursor(data,item,0) and total(data,item)==before+amount and tagged_total(data,item,signature)==expected_tagged)
            if partner=='minecraft:barrel':
                expect('second native interaction opens the requested barrel rather than a held-item menu',
                       native_barrel_menu(other),actual=other['type'],registered_type=other.get('menuType'))
            partner_inputs = [s['index'] for s in accepting(other)]
            other_slot = slot if slot in partner_inputs else partner_inputs[0]
            row['cross_mod_partner'] = {'block': partner, 'menu': other['type'], 'session': other['session'], 'slot': other_slot, 'same_slot_index': slot == other_slot}
            act('regression-player-click', slot=11, ticks=6)
            act('regression-slot-click', session=sid, slot=slot, ticks=8)
            act('regression-slot-click', session=sid, slot=slot, ticks=8)
            act('regression-slot-click', session=other['session'], slot=other_slot, ticks=8)
            data = snapshot(item)
            second_stack = next(m for m in data['menus'] if m['session'] == other['session'])['slots'][other_slot]
            expect('cross-mod transfer reaches intended source only', second_stack['item'] == item and second_stack['count'] == amount and cursor(data, item, 0))
            act('regression-slot-click', session=sid, slot=slot, ticks=8)
            expect('empty first source cannot pick up the other menus stack', cursor(snapshot(item), item, 0))
            # Scatter two item types in the other publisher's storage, then use
            # SIU's actual sort control. Verify packing and both item totals.
            alternate = 'minecraft:apple' if item != 'minecraft:apple' else 'minecraft:diamond'
            policy = snapshot(alternate)
            partner_menu = next(m for m in policy['menus'] if m['session'] == other['session'])
            empty = [s for s in partner_menu['slots'] if not s['player'] and not s['virtual'] and s['active'] and s.get('visible',True) and s['mayPlace'] and s['count'] == 0]
            if len(empty) >= 2:
                act('regression-slot-click', session=other['session'], slot=other_slot, ticks=4)
                act('regression-slot-click', session=other['session'], slot=empty[-1]['index'], ticks=4)
                other_slot = empty[-1]['index']
                alternate_before = total(policy, alternate)
                act('regression-seed-player', items=[{'slot': 13, 'item': alternate, 'count': 3}], ticks=4)
                act('regression-player-click', slot=13, ticks=4)
                act('regression-slot-click', session=other['session'], slot=empty[0]['index'], ticks=6)
                act('regression-control', session=other['session'], control='SORT_INVENTORY', ticks=12)
                data = snapshot(item)
                expect('SIU sort conserves both publisher storage item types', cursor(data, item, 0)
                       and total(data, item) == before + amount and total(data, alternate) == alternate_before + 3)
                packed = [s for s in next(m for m in data['menus'] if m['session'] == other['session'])['slots'] if not s['player'] and not s['virtual'] and s['count']]
                expect('SIU sort packs scattered storage stacks', len(packed) == 2 and max(s['index'] for s in packed) < other_slot,
                       scattered_index=other_slot, packed=packed)
                other_slot = next(s['index'] for s in packed if s['item'] == item)
                alternate_slot = next(s['index'] for s in packed if s['item'] == alternate)
                act('regression-slot-click', session=other['session'], slot=alternate_slot, ticks=6)
                act('regression-player-click', slot=14, ticks=6)
                data = snapshot(item)
                expect('sorted secondary stack returns once to player inventory', cursor(data, item, 0)
                       and total(data, alternate) == alternate_before + 3)
                row['sort_partner_checked'] = True
            act('regression-slot-click', session=other['session'], slot=other_slot, ticks=8)
            act('regression-slot-click', session=sid, slot=slot, ticks=8)
            act('regression-slot-click', session=sid, slot=slot, ticks=8)
            act('regression-player-click', slot=12, ticks=8)
            data = snapshot(item)
            expect('two-menu round trip conserves one shared cursor and all physical items', cursor(data, item, 0) and total(data, item) == before + amount)
            expect('two-menu round trip deposits the tagged stack in the requested player cell',data['inventory'][12]['item']==item and data['inventory'][12]['count']==amount)
            expect('cross-mod transfers and sorting preserve stack data', tagged_total(data, item, signature) == expected_tagged)
            if amount>=3 and '__native_drag_distribution__' not in exercised:
                cells=[s['index'] for s in next(m for m in data['menus'] if m['session']==other['session'])['slots']
                       if not s['player'] and not s['virtual'] and s['active'] and s.get('visible',True) and s['mayPlace'] and not s['count'] and s['max']>=amount]
                if len(cells)>=2:
                    cells=cells[:2]
                    act('regression-player-click',slot=12,ticks=4)
                    act('regression-slot-drag',session=other['session'],slots=cells,ticks=6)
                    data=snapshot(item);native=next(m for m in data['menus'] if m['session']==other['session'])
                    expect('real left drag divides a tagged stack between native mod cells',all(native['slots'][i]['count']==amount//2 for i in cells)
                           and cursor(data,item,amount%2) and total(data,item)==before+amount and tagged_total(data,item,signature)==expected_tagged)
                    if amount%2:act('regression-player-click',slot=12,ticks=4)
                    for cell in cells:
                        act('regression-slot-click',session=other['session'],slot=cell,ticks=4)
                        act('regression-player-click',slot=12,ticks=4)
                    data=snapshot(item)
                    expect('dragged native stacks recombine without lost items or metadata',cursor(data,item,0) and data['inventory'][12]['count']==amount
                           and total(data,item)==before+amount and tagged_total(data,item,signature)==expected_tagged)
                    row['native_drag_partner_checked']={'block':partner,'slots':cells,'distribution':[amount//2,amount//2],'remainder':amount%2}
                    exercised.add('__native_drag_distribution__')
            if '__native_partner_reopen__' not in exercised:
                # Closing an empty menu only tests cursor lifetime. Leave real
                # tagged contents in the publisher storage and reopen it by
                # ordinary use to verify its native save/restore callbacks.
                resident_before=snapshot(item)
                player_before=sum(v['count'] for v in resident_before['inventory'] if v['item']==item)
                act('regression-player-click',slot=12,ticks=4)
                act('regression-slot-click',session=other['session'],slot=other_slot,ticks=4)
                old_partner=other['session']
                act('regression-close',session=old_partner,ticks=8)
                closed=snapshot(item)
                expect('closing populated partner removes its menu without copying its resident stack to player inventory',
                       all(m['session']!=old_partner for m in closed['menus']) and cursor(closed,item,0)
                       and sum(v['count'] for v in closed['inventory'] if v['item']==item)==player_before-amount)
                retained={m['session'] for m in closed['menus']}
                if closed['inputPolicy']['selectedSourceLocked']:
                    reopened,sid,other=open_partner(row,closed,sid,partner,act,expect,snapshot,place=False,nearPlayer=True,dx=-1,dz=1)
                else:
                    act('regression-open',nearPlayer=True,dx=-1,dz=1,emptyHand=True,ticks=18)
                    reopened=snapshot(item)
                expect('reopening populated partner retains the first publisher session',any(m['session']==sid for m in reopened['menus']))
                candidates=[m for m in reopened['menus'] if m['session'] not in retained and m['session']!=sid]
                expect('ordinary partner reuse opens one fresh native menu',len(candidates)==1)
                other=candidates[0]
                resident=[s for s in other['slots'] if not s['player'] and not s['virtual'] and s['item']==item and s['count']]
                expect('native close and reopen preserves the exact resident stack and full data',len(resident)==1
                       and resident[0]['count']==amount and resident[0]['signature']==signature
                       and total(reopened,item)==before+amount and tagged_total(reopened,item,signature)==expected_tagged)
                other_slot=resident[0]['index']
                act('regression-slot-click',session=other['session'],slot=other_slot,ticks=4)
                act('regression-player-click',slot=12,ticks=4)
                returned=snapshot(item)
                expect('resident stack withdraws once after native menu reopen',cursor(returned,item,0)
                       and returned['inventory'][12]['count']==amount and tagged_total(returned,item,signature)==expected_tagged)
                row['native_partner_reopen_checked']={'block':partner,'closed_session':old_partner,'reopened_session':other['session']}
                exercised.add('__native_partner_reopen__')
            act('regression-close', session=other['session'], ticks=10)
            data = snapshot(item)
            expect('closing another mod window retains first session', any(m['session'] == sid for m in data['menus']) and all(m['session'] != other['session'] for m in data['menus']))
            expect('independent close preserves items and custom data',total(data,item)==before+amount and tagged_total(data,item,signature)==expected_tagged)
            if conditioning:
                for module,active in enumerate(row['conditioning_original_enabled']):
                    if active:act('regression-conditioning-button',session=sid,button=module,ticks=6)
                restored=snapshot(item)['nativeHost']['airConditioningBurns']
                expect('real native module switches restore every original setting',
                       [v['enabled'] for v in restored]==row['conditioning_original_enabled'])
                row['native_conditioning_module_controls_checked']=True
            act('regression-player-click',slot=12,ticks=4)
            expect('source close scenario holds exactly the tagged stack',cursor(snapshot(item),item,amount))
            if 'portable_host_energy' in row:
                current_host=snapshot(item)['inventory'][0]
                energy=row['portable_host_energy'];energy['after']=portable_energy(current_host)
                expect('native portable source keeps draining power throughout real transfers',
                       current_host['item']==energy['item'] and 0<energy['after']<energy['before'],energy=energy)
            board_stock=None
            if family=='io.ejekta.bountiful.content.gui.BoardScreenHandler':
                act('regression-bountiful-decree-ledger',ticks=2)
                board_stock=json.loads((profile/'regression-bountiful-decree-ledger.json').read_text())
            act('regression-close',session=sid,ticks=10)
            data=snapshot(item)
            if board_stock is not None:
                act('regression-bountiful-decree-ledger',ticks=2)
                closed_stock=json.loads((profile/'regression-bountiful-decree-ledger.json').read_text())
                expect('native board retains every resident decree and its complete data after window close',closed_stock==board_stock)
                row['native_closed_board_stock']={'before':board_stock,'after':closed_stock,
                    'scope':'Read-only actual block entity decree inventory; closed world stock remains owned.'}
                data=with_closed_source(data,sid,closed_stock['cells'])
            expect('closing source with a carried stack conserves it exactly once',all(m['session']!=sid for m in data['menus'])
                   and total(data,item)==before+amount and tagged_total(data,item,signature)==expected_tagged)
            if 'portable_host_energy' in row:
                expect('closing the last portable view releases its source lock',not data['inputPolicy']['selectedSourceLocked'])
                act('regression-player-click',slot=15,ticks=4)
                settled=snapshot(item)
                expect('last portable close settles its carried stack once in separate inventory',
                       cursor(settled,item,0) and total(settled,item)==before+amount and tagged_total(settled,item,signature)==expected_tagged)
                act('regression-player-click',slot=36,ticks=4);picked=snapshot(item)
                expect('the closed portable source is pickable again through the real player window',
                       picked['cursor']['item']==row['portable_host_energy']['item'] and picked['cursor']['count']==1 and picked['inventory'][0]['count']==0)
                act('regression-player-click',slot=36,ticks=4)
                expect('the released host returns once with no carried item',snapshot(item)['cursor']['count']==0)
                row['portable_source_lifecycle_checked']=True
            row['status'] = 'passed'
            exercised.add(family)
        except Exception as error:
            import shutil
            evidence = profile / 'regression-failures' / current; evidence.mkdir(parents=True, exist_ok=True)
            for name in ['regression-bountiful-decree-ledger.json', 'regression-mutation-state.json', 'regression-click-inputs.json', 'regression-server-block-use.json', 'regression-native-browser.json', 'regression-block-use.json', 'regression-state.json', 'reply.json', 'observations.json','regression-ray-state.json','observer-policy-side-effects.json','regression-native-container-ledger.json','regression-point.json','regression-ae2-cell-ledger.json','regression-ae2-repository-state.json','regression-native-settings.json','regression-character.json','regression-builtin.json']:
                if (profile / name).exists(): shutil.copy2(profile / name, evidence / name)
            row.update(status='failed', error=str(error), trace=traceback.format_exc()); print('FAIL', current, error, flush=True)
            if isinstance(error, TimeoutError) or 'RegressionWorldDisconnected' in str(error):
                save(); raise
        save(); print('END', current, row['status'], flush=True)
    save()
    return {'cases': len(rows), 'passed': sum(r['status'] == 'passed' for r in rows),
            'entry_only': sum(r['status'] == 'entry-and-policy-only' for r in rows),
            'failed': sum(r['status'] == 'failed' for r in rows), 'assertions': len(assertions), 'report': str(report)}

if __name__ == '__main__':
    p = argparse.ArgumentParser(); p.add_argument('profile', type=Path); p.add_argument('--minecraft', required=True); p.add_argument('--loader', required=True); p.add_argument('--only', action='append', default=[]); a = p.parse_args()
    result = run(a.profile, a.minecraft, a.loader, a.only); print(json.dumps(result), flush=True)
    if result['failed']: raise SystemExit(1)
