"""Record real screenshots with their exact runtime observations and artifact inputs."""
from pathlib import Path
import hashlib,json,sys,time,msvcrt,uuid
from PIL import Image

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send as raw_send
LEDGER=ROOT/'build/all-target-rollout/ui-capture-ledger.json'

def send(run,command_type,**kwargs):
    """Normalize the two excluded observer protocols, without changing the UI."""
    kind=command_type
    run=Path(run)
    if run.name.startswith('26.3-'):
        # The 26.3 input ABI uses SDL scancodes/buttons. The driver protocol
        # retains GLFW-style codes so the same entry workflow works elsewhere.
        if kind in {'key','regression-native-key'}:
            key=kwargs.get('key',256)
            kwargs['keycode']=key+32 if 65<=key<=90 else key if 32<=key<=126 else 0
            mods=kwargs.get('modifiers',0)
            kwargs['modifiers']=(1 if mods&1 else 0)|(64 if mods&2 else 0)|(256 if mods&4 else 0)|(1024 if mods&8 else 0)
            kwargs['sdl']=True
            kwargs['key']=(key-65+4 if 65<=key<=90 else key-49+30 if 49<=key<=57 else
                           {48:39,256:41,257:40,258:43,259:42,261:76,262:79,263:80,264:81,265:82,266:75,267:78,268:74,269:77,32:44}.get(key,key))
        if run.parent.parent.name=='all-target-rollout' and kind in {'click','release'} and 'x' in kwargs:
            kwargs['button']={0:1,1:3,2:2}.get(kwargs.get('button',0),kwargs.get('button',0))
    if run.parent.parent.name=='all-target-rollout':
        # This older excluded observer exposes a complete left-click command,
        # rather than the split press/release ABI used by the newer observers.
        # Both real screen callbacks are issued by xy-click itself.
        if run.name.startswith('1.21.1-') and kind in {'click','release'} and 'x' in kwargs:
            if kind=='click':
                if kwargs.get('button',0)!=0:raise ValueError('This observer only exposes an atomic left click')
                kind='xy-click'
            else:
                kind='wait';kwargs={'ticks':kwargs.get('ticks',1)}
        aliases={'command':'server-command','move':'hover','use-item':'client-use-item',
                 'use-entity':'client-use-entity','use-block-ray':'client-use-any-block-ray','open-ray':'client-use-block-ray',
                 'use-entity-ray':'client-use-entity-ray','scroll':'xy-scroll',
                 'release':'xy-release','reviewed-session-seed':'seed'}
        if kind=='click' and 'x' in kwargs:kind='xy-press'
        elif kind=='held':
            count=float(kwargs.get('count',1))
            if not count.is_integer():raise ValueError('Held item count must be an integer')
            kind='server-command';kwargs={'command':'item replace entity @a weapon.mainhand with '+kwargs['item']+' '+str(int(count)),'ticks':kwargs.get('ticks',5)}
        else:kind=aliases.get(kind,kind)
        response=raw_send(run,kind,**kwargs)
        state=response.get('client',{})
        state['screen']=state.get('currentScreen',state.get('screen','none'))
        state['allWindows']=state.get('windows',[])
        if not state['allWindows'] and 'nativeBounds' in state:state.setdefault('bounds',state['nativeBounds'])
        return response
    return raw_send(run,kind,**kwargs)

def mutate_ledger(mutation):
    """All capture writers and maintenance operations use the same file lock."""
    with LEDGER.with_suffix('.lock').open('a+b') as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0)
        # LK_LOCK gives up after ten OS retries. Large paired observations on
        # slower project volumes can legitimately take longer to publish.
        deadline=time.monotonic()+120
        while True:
            try:
                msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
                break
            except OSError as error:
                if error.errno not in (13,36) or time.monotonic()>=deadline:raise
                time.sleep(.1)
        try:
            rows=json.loads(LEDGER.read_text()) if LEDGER.exists() else []
            rows=mutation(rows)
            temporary=LEDGER.with_suffix('.tmp')
            # The atlas now contains thousands of observations. Stream the
            # journal instead of allocating both a giant chunk list and joined
            # JSON string while reference clients are loading original assets.
            with temporary.open('w') as stream:json.dump(rows,stream,indent=2)
            temporary.replace(LEDGER)
        finally:
            lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)

def validate_state(state,side,ui):
    assert (state.get('screen','').endswith('InventoryDesktopScreen'))==(side=='siu'),state.get('screen')
    if side!='siu':
        # Some existing dedicated container screens (e.g. AE2 Spatial Anchor)
        # use controls without item cells. A real menu/screen still distinguishes
        # them from an item action that opened no UI at all.
        assert state.get('screen') not in (None,'none') and state.get('menuClass') and state.get('bounds') and 'slots' in state,'No native inventory/container UI is visible'
        return
    if ui=='trinkets-equipment':
        assert state.get('worldLoaded') and any(w.get('kind')=='CHARACTER' for w in state.get('workspaceWindows',[])), 'Trinkets attaches to the live Character inventory rather than opening another container session'
        return
    windows=state.get('windows',state.get('allWindows',[]))
    assert windows or (state.get('window') and state.get('menuId')),'No owned container window is visible'
    assert all(not w.get('layoutError') for w in windows),'Native layout callback failed'
    if ui=='merequester-wireless-terminal':
        assert any(w.get('menuId')=='merequester:wireless_requester_terminal' and w.get('definition','').endswith('.AEWindow') and w.get('bounds',{}).get('width',0)>=195 and w.get('bounds',{}).get('height',0)>=85 for w in windows),'Requester content must be initialized at its native desktop size; the small error window is not UI evidence'
    if ui=='bf-moa-clean':
        assert sum(w.get('menuId')=='salts_inventory_compat:moa' for w in windows)==1,'One normal Moa interaction must produce exactly one owned window'
    if ui=='bb-spawn-hamster':
        assert any(w.get('menuId') in ['salts_inventory_compat:hamster','spawn:hamster_inventory'] and isinstance(w.get('template'),dict) and 'AutomaticNativeWindow' not in w.get('definition','') for w in windows),'Hamster must use its original inventory and the reviewed window layout'
    if ui=='cataclysm-netherite-ministrosity':
        assert any(w.get('nativeScreen','').endswith('.MinistrosityInventoryScreen') and isinstance(w.get('template'),dict) and any(r['id']=='native-gold-slots' for r in w['template']['regions']) for w in windows),'Expected approved Ministrosity portrait and original gold cargo; generic storage is not confirmation'
    if ui.startswith('smallships-'):
        assert any(isinstance(w.get('template'),dict) and {'cargo','statistics','pager'}.issubset({r['id'] for r in w['template']['regions']}) for w in windows),'Expected complete ship cargo/statistics/pager regions; automatic fallback is not confirmation'
    ae2_menus={'ae2-priority':'ae2:priority','ae2-craft-amount':'ae2:craftamount',
               'ae2-craft-confirm':'ae2:craftconfirm','ae2-stock-amount':'ae2:set_stock_amount',
               'ae2-crafting-status':'ae2:craftingstatus','ae2-crafting-cpu':'ae2:craftingcpu'}
    if ui in ae2_menus:
        assert any(w.get('menuId')==ae2_menus[ui] for w in windows),'The requested native AE2 submenu did not open'
    if ui=='ae2-terminal-settings':
        assert any(w.get('nativeScreen','').endswith('.TerminalSettingsScreen') for w in windows),'Native terminal settings did not open'
    if ui=='villagersplus-ore-grinder':
        assert any('AutomaticNativeWindow' not in w.get('definition','') and isinstance(w.get('template'),dict) for w in windows),'Ore Grinder must use its integrated profile'
    if ui=='niftycarts-seed-drill':
        assert all('AutomaticNativeWindow' not in w.get('definition','') for w in windows),'Seed Drill must use its integrated row'
    if ui.startswith('niftycarts-wagon-'):
        assert all('AutomaticNativeWindow' not in w.get('definition','') for w in windows),'Wagon chest sizes must use their integrated grid'
    required=('storage' if ui.startswith('travelers-') and 'settings' not in ui else
              'four-native-offers' if ui.startswith('trading-post') else
              'original-vending-panel' if ui in ['toms-config','toms-customer'] else None)
    if required:
        assert any(isinstance(w.get('template'),dict) and any(r['id']==required for r in w['template']['regions']) for w in windows),f'Expected {required} integration; automatic fallback is not confirmation'

def capture(run,side,minecraft,loader,ui,note=''):
    run=Path(run).resolve()
    log=next((p for p in [run/'launch.log',run/'run/logs/latest.log'] if p.exists()),None)
    log_start=log.stat().st_size if log else 0
    stem=f'{side}-{ui}-{minecraft.replace(".", "")}-{loader}-{uuid.uuid4().hex[:8]}'
    reply=send(run,'capture',capture=stem,ticks=16)
    state=reply['client']
    if log:
        with log.open('rb') as stream:
            stream.seek(log_start);recent=stream.read().decode('utf-8',errors='replace')
        assert not any('[desktop-api]' in line and 'callback failed' in line for line in recent.splitlines()),'The native renderer failed during capture; do not accept a title-only/error frame'
    validate_state(state,side,ui)
    state['siu_loaded']=side=='siu'
    path=run/'screenshots'/(stem+'.png')
    deadline=time.monotonic()+5
    verified=False
    while time.monotonic()<deadline:
        try:
            with Image.open(path) as im:im.verify()
            verified=True;break
        except (FileNotFoundError,OSError,SyntaxError):time.sleep(.05)
    assert verified,path
    row={'minecraft':minecraft,'loader':loader,'ui':ui,'side':side,'screenshot':str(path),
         'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'state':state,'run':str(run),'note':note}
    artifacts=run/'staged-artifacts.json'
    if artifacts.exists():row['staged_artifacts']=json.loads(artifacts.read_text(encoding='utf-8-sig'))
    mutate_ledger(lambda rows:[r for r in rows if (r['minecraft'],r['loader'],r['ui'],r['side'])!=(minecraft,loader,ui,side)]+[row])
    print('CAPTURE',stem,flush=True)
    return state
