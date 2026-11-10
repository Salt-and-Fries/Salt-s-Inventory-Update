"""Fresh native ownership/input checks for the six original native-only entries.

Sixteen native screen types are reached through genuine block/item/entity use.
This does not certify dungeon generation, every teleport condition or gameplay
unrelated to SIU. Retained old navigation results keep their original scope.
"""
from pathlib import Path
import hashlib,json,sys
import nbtlib

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/all-target-rollout'))
from evidence import send
from mowzie_features import stack,item_count


def field(state,name):
    values=[v for k,v in state['client']['fields'].items() if k.endswith('#'+name)]
    assert len(values)==1,(name,state['client']['fields']);return values[0]


def owns_native(state,expected,sentinel):
    c=state['client']
    return (c['screen']==expected and c['desktopActive'] is False and isinstance(state['sessions'],list) and state['sessions']==[]
        and stack(state['inventory'][12]['item'])==sentinel and stack(state['cursor'])==stack('{}') and stack(state['nativeCursor'])==stack('{}'))


class NativeOnly:
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.checks=[];self.n=0;self.screens=set()
        self.out=self.profile/'native-only-release';self.out.mkdir()
    def act(self,kind,**kw):return send(self.profile,kind,**kw)
    def cmd(self,command,ticks=6):return self.act('command',command=command,ticks=ticks)
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.groups,checks=self.checks,screens=sorted(self.screens),
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Six original native-only mods on the exact three recorded targets; native screen ownership, real input/navigation/settings, Escape/reopening and SIU inventory return. Not dungeon generation, challenge completion, every teleport condition or untested releases.'),indent=2)+'\n')
    def check(self,name,value):
        self.checks.append(dict(name=name,passed=bool(value)));self.save();assert value,name
        print('PASS',name,flush=True)
    def state(self,label):
        self.act('regression-native-only-state',ticks=4);value=json.loads((self.profile/'release-native-only-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(value,indent=2)+'\n');return value
    def native(self,expected,label):
        d=self.state(label);self.check(label+'-original-screen-owns-input',owns_native(d,expected,self.sentinel))
        self.screens.add(expected);self.save();return d
    def key(self,key,ticks=5):return self.act('regression-native-only-key',key=key,ticks=ticks)
    def text(self,value):return self.act('regression-native-only-text',text=value,ticks=8)
    def tap(self,x,y,state,button=0):
        # Original exact-publisher native geometry at 640x450; translate its
        # centered frame to the ACTUAL current GUI dimensions, never a desktop.
        gx=state['client']['guiWidth'];gy=state['client']['guiHeight'];x=x+(gx-640)/2;y=y+(gy-450)/2
        assert 0<=x<gx and 0<=y<gy
        return self.act('regression-native-only-tap',x=x,y=y,button=button,ticks=8)
    def press(self,label,state):
        rows=[w for w in state['client']['widgets'] if w['message']==label and w['visible'] and w['active']]
        assert len(rows)==1,(label,rows)
        w=rows[0];return self.act('regression-native-only-tap',x=w['x']+w['width']/2,y=w['y']+w['height']/2,ticks=8)
    def edit(self,widget,value):
        assert widget['editBox'] and widget['visible'] and widget['active']
        self.act('regression-native-only-tap',x=widget['x']+widget['width']/2,y=widget['y']+widget['height']/2,ticks=2)
        self.key(268,ticks=1)
        for _ in widget['value']:self.key(261,ticks=1)
        self.text(value)
    def escape(self,label):
        self.key(256,ticks=12);d=self.state(label)
        self.check(label+'-escape-closes-native-screen-and-leaves-no-desktop-source',d['client']['screen']=='none' and not d['sessions'] and not d['client']['desktopActive'])
    def held(self,item,nbt=''):
        self.cmd('item replace entity @p weapon.mainhand with '+item+nbt)
    def use_item(self):return self.act('regression-use-held-item',shift=False,ticks=16)
    def block(self,item,creative=True):
        self.act('close',ticks=3);self.act('sneak',down=False,ticks=3)
        self.cmd('gamemode '+('creative' if creative else 'survival')+' @p')
        self.cmd('fill 6 64 6 13 64 13 minecraft:stone');self.cmd('fill 6 65 6 13 69 13 minecraft:air')
        self.cmd('setblock 8 65 8 '+item);self.cmd('tp @p 8.5 65 11.5 180 20.5');self.held('minecraft:air')
    def use_block(self):return self.act('regression-native-only-block-use',ticks=16)
    def dungeonz(self):
        op='net.dungeonz.block.screen.DungeonPortalOpScreen';normal='net.dungeonz.block.screen.DungeonPortalScreen'
        self.block('dungeonz:dungeon_portal');self.use_block();d=self.native(op,'dungeonz-portal-operator')
        self.key(258);self.native(op,'dungeonz-portal-keyboard');self.press('Done',d)
        self.use_block();d=self.native(normal,'dungeonz-portal')
        widgets=[w for w in d['client']['widgets'] if w['type'].endswith('$DungeonDifficultyButton')]
        assert len(widgets)==1;before=widgets[0]['fields'];self.tap(365,160,d)
        changed=self.native(normal,'dungeonz-difficulty')
        after=next(w for w in changed['client']['widgets'] if w['type'].endswith('$DungeonDifficultyButton'))['fields']
        self.check('dungeonz-native-difficulty-control-responds',before!=after)
        self.escape('dungeonz-portal-close');self.use_block();self.native(normal,'dungeonz-portal-reopened');self.escape('dungeonz-portal-reclose')
        gate='net.dungeonz.block.screen.DungeonGateOpScreen';self.block('dungeonz:dungeon_gate')
        self.act('sneak',down=True,ticks=5);self.use_block();d=self.native(gate,'dungeonz-gate-operator');self.press('Done',d)
        self.act('sneak',down=False,ticks=4)
        compass='net.dungeonz.item.screen.DungeonCompassScreen';self.block('minecraft:cartography_table');self.held('dungeonz:dungeon_compass');self.use_block()
        d=self.native(compass,'dungeonz-compass');before=next(w for w in d['client']['widgets'] if w['message']=='Calibrate')['active']
        self.tap(309,160,d);selected=self.native(compass,'dungeonz-compass-selector')
        self.check('dungeonz-native-compass-selection-enables-calibration',before is False and next(w for w in selected['client']['widgets'] if w['message']=='Calibrate')['active'] is True)
        self.escape('dungeonz-compass-close');self.use_block();self.native(compass,'dungeonz-compass-reopened');self.escape('dungeonz-compass-reclose')
    def sortilege(self):
        expected='net.lyof.sortilege.screen.custom.KnowledgeBookScreen'
        self.block('minecraft:stone');self.held('sortilege:knowledge_book');self.use_item();self.native(expected,'sortilege-empty');self.escape('sortilege-empty-close')
        self.held('sortilege:knowledge_book','{sorti_EnchantKnowledge:{"minecraft:sharpness":3,"minecraft:unbreaking":3},sorti_Authors:["ReleaseCheck"]}')
        self.use_item();d=self.native(expected,'sortilege-populated');before=field(d,'pageIndex')
        self.check('sortilege-native-populated-book-has-multiple-pages',field(d,'pageCount')>=2)
        self.tap(350,294,d);after=self.native(expected,'sortilege-next-page');self.check('sortilege-native-next-page-click-reaches-book',field(after,'pageIndex')==before+1)
        self.tap(277,294,after);back=self.native(expected,'sortilege-previous-page');self.check('sortilege-native-previous-page-restores-position',field(back,'pageIndex')==before)
        self.key(258);self.native(expected,'sortilege-keyboard');self.escape('sortilege-close');self.use_item();self.native(expected,'sortilege-reopened');self.escape('sortilege-reclose')
    def things(self):
        expected='com.glisco.things.client.DisplacementTomeScreen';self.block('minecraft:stone',creative=False)
        self.held('things:displacement_tome','{Fuel:4,Targets:{}}');self.cmd('item replace entity @p container.9 with things:displacement_page 3')
        self.use_item();d=self.native(expected,'things-tome');self.tap(381,153,d);d=self.native(expected,'things-add-destination')
        self.tap(450,172,d);self.text('Release Home');d=self.native(expected,'things-name-entry');self.tap(463,193,d)
        created=self.native(expected,'things-destination-created')
        self.check('things-native-text-and-create-destination-reach-real-tome','Release Home' in created['held'])
        self.escape('things-close');self.use_item();reopened=self.native(expected,'things-reopened')
        self.check('things-native-destination-persists-after-reopen','Release Home' in reopened['held'] and stack(reopened['held'])==stack(created['held']))
        self.escape('things-reclose')
    def wraith(self):
        expected='wraith.fwaystones.screen.WaystoneBlockScreen';self.block('fwaystones:waystone');self.use_block();d=self.native(expected,'wraith-waystone')
        edits=[w for w in d['client']['widgets'] if w['editBox'] and w['visible']]
        assert edits;search=min(edits,key=lambda w:w['y']);self.edit(search,'release-no-matching-destination')
        queried=self.native(expected,'wraith-search');self.check('wraith-native-search-accepts-text',any(w.get('value')=='release-no-matching-destination' for w in queried['client']['widgets']))
        self.tap(394,151,queried);settings=self.native(expected,'wraith-settings')
        self.check('wraith-native-settings-page-opens',field(settings,'page')!=field(queried,'page'))
        names=[w for w in settings['client']['widgets'] if w['editBox'] and w['visible']]
        assert names;self.edit(max(names,key=lambda w:w['y']),'Release Waystone');d=self.native(expected,'wraith-rename-input');self.tap(364,287,d)
        self.escape('wraith-close');self.use_block();reopened=self.native(expected,'wraith-reopened');self.tap(394,151,reopened);settings=self.native(expected,'wraith-renamed-settings')
        self.check('wraith-native-rename-persists-after-ordinary-reopening',any(w.get('value')=='Release Waystone' for w in settings['client']['widgets']))
        self.escape('wraith-reclose')
        for item,screen in [('fwaystones:pocket_wormhole','wraith.fwaystones.screen.PocketWormholeScreen'),('fwaystones:abyss_watcher','wraith.fwaystones.screen.AbyssScreen')]:
            self.held(item);self.use_item();self.native(screen,item.split(':')[1]);self.escape(item.split(':')[1]+'-close')
            self.use_item();self.native(screen,item.split(':')[1]+'-reopened');self.escape(item.split(':')[1]+'-reclose')
    def elevator(self):
        expected='xyz.vsngamer.elevatorid.client.gui.ElevatorScreen';self.block('elevatorid:elevator_white');self.use_block();d=self.native(expected,'elevator-options')
        before=nbtlib.parse_nbt(d['blockEntity']);self.press('Directional',d);changed=self.native(expected,'elevator-directional')
        self.check('elevator-native-directional-input-reaches-block-entity',nbtlib.parse_nbt(changed['blockEntity'])!=before)
        self.press('N',changed);north=self.native(expected,'elevator-facing');self.check('elevator-native-facing-input-reaches-block-entity',nbtlib.parse_nbt(north['blockEntity'])!=nbtlib.parse_nbt(changed['blockEntity']))
        self.escape('elevator-close');self.use_block();reopened=self.native(expected,'elevator-reopened')
        self.check('elevator-native-options-persist-after-reopen',nbtlib.parse_nbt(reopened['blockEntity'])==nbtlib.parse_nbt(north['blockEntity']))
        self.escape('elevator-reclose')
    def armor_statues(self):
        prefix='fuzs.statuemenus.api.v1.client.gui.screens.';self.block('minecraft:air')
        self.cmd('kill @e[type=minecraft:armor_stand]');self.cmd('summon minecraft:armor_stand 8.5 65 8.5 {NoGravity:1b,ShowArms:1b,Tags:["release_native_stand"]}')
        self.cmd('tp @p 8.5 65 11.0 180 10');self.act('client-use-entity-ray',entity='minecraft:armor_stand',shift=True,ticks=20)
        d=self.native(prefix+'ArmorStandRotationsScreen','armor-rotations')
        for label,y,screen in [('poses',185,prefix+'ArmorStandPosesScreen'),('style',211,prefix+'ArmorStandStyleScreen'),('position',239,prefix+'ArmorStandPositionScreen'),('alignments',267,'fuzs.armorstatues.client.gui.screens.armorstand.ArmorStandAlignmentsScreen'),('equipment',293,prefix+'ArmorStandEquipmentScreen')]:
            self.tap(200,y,d);d=self.native(screen,'armor-'+label)
        self.tap(200,211,d);style=self.native(prefix+'ArmorStandStyleScreen','armor-style-edit')
        edits=[w for w in style['client']['widgets'] if w['editBox'] and w['visible']];assert len(edits)==1
        self.edit(edits[0],'Release Stand');d=self.native(prefix+'ArmorStandStyleScreen','armor-name');self.press('Show Arms',d)
        changed=self.native(prefix+'ArmorStandStyleScreen','armor-native-edit');entity=nbtlib.parse_nbt(changed['armorStand'])
        self.check('armor-native-text-and-checkbox-reach-server-entity','Release Stand' in str(entity.get('CustomName','')) and int(entity['ShowArms'])==0)
        self.escape('armor-close');self.act('client-use-entity-ray',entity='minecraft:armor_stand',shift=True,ticks=16);reopened=self.native(prefix+'ArmorStandStyleScreen','armor-reopened')
        entity=nbtlib.parse_nbt(reopened['armorStand']);self.check('armor-native-edits-persist-after-reopen','Release Stand' in str(entity.get('CustomName','')) and int(entity['ShowArms'])==0)
        self.escape('armor-reclose');self.act('sneak',down=False,ticks=4)
    def run(self):
        self.groups=['dungeonz','sortilege','things','wraith-waystones'] if self.loader=='fabric' else ['elevatormod'] if self.loader=='forge' else ['armor-statues']
        self.act('close',ticks=3);self.cmd('clear @p')
        tag='{release_native_sentinel:71,typed:[B;2b,8b]}'
        self.cmd('item replace entity @p container.12 with minecraft:diamond'+(tag if self.mc=='1.20.1' else '[minecraft:custom_data='+tag+']')+' 7')
        seeded=self.state('sentinel');self.sentinel=stack(seeded['inventory'][12]['item'])
        self.check('foreign-typed-inventory-sentinel-is-real',item_count(seeded['inventory'][12]['item'])==('minecraft:diamond',7) and 'release_native_sentinel' in seeded['inventory'][12]['item'])
        for name in self.groups:getattr(self,dict(elevatormod='elevator',**{'armor-statues':'armor_statues','wraith-waystones':'wraith'}).get(name,name))()
        expected=9 if self.loader=='fabric' else 1 if self.loader=='forge' else 6
        self.check('all-required-native-screen-types-were-actually-observed',len(self.screens)==expected)
        self.act('inventory',ticks=14);returned=self.state('ordinary-inventory-return')
        self.check('ordinary-SIU-inventory-remains-available-after-native-screens',returned['client']['desktopActive'] and returned['client']['screen'].endswith('.InventoryDesktopScreen') and stack(returned['inventory'][12]['item'])==self.sentinel and not nbtlib.parse_nbt(returned['cursor']))
        self.act('close',ticks=3);self.save(True)
        return dict(cases=len(self.groups),failed=0,checks=len(self.checks),native_only_checked=True,screens=sorted(self.screens))


def run(profile,mc,loader,*args,**kw):return NativeOnly(profile,mc,loader).run()
