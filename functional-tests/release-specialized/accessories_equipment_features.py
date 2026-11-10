"""Real paired equipment edits, native visibility and exclusive panel swaps."""
from pathlib import Path
from collections import Counter
import hashlib,json,sys
import nbtlib

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
from mowzie_features import stack,item_count


def empty(text):return not nbtlib.parse_nbt(text)


def ledger(state):
    values=[v['item'] for v in state['inventory']]+[state['canonicalCursor']]
    for group in state['groups'].values():values+=group['normal']+group['cosmetic']
    result=Counter()
    for value in values:
        if not empty(value):result[stack(value,1)]+=item_count(value)[1]
    return result


class Accessories:
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.checks=[];self.n=0
        self.out=self.profile/'accessories-equipment-release';self.out.mkdir()
    def act(self,kind,**kw):return send(self.profile,kind,**kw)
    def cmd(self,text,ticks=6):return self.act('server-command' if self.loader=='neoforge' else 'command',command=text,ticks=ticks)
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=['accessories-equipment'],checks=self.checks,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Actual native normal and cosmetic capability cells, supported item slot-validation fixture data, typed full-item conservation, native visibility controls, parent movement, exclusive available Curios/Aether swaps, rejection and close/reopen. No third-party accessory effects, Compat Layer or every modpack combination claim.'),indent=2)+'\n')
    def check(self,name,value):
        self.checks.append(dict(name=name,passed=bool(value)));self.save();assert value,name
        print('PASS',name,flush=True)
    def state(self,label):
        self.act('regression-accessories-equipment-state',ticks=5)
        value=json.loads((self.profile/'release-accessories-equipment-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(value,indent=2)+'\n');return value
    def open(self):self.act('regression-attachment-click',attachment='.accessories.AccessoriesClient$Button',ticks=18)
    def panel(self,state):
        rows=state['client']['panels'];assert len(rows)==1;return rows[0]
    def slot(self,state,slot):self.act('regression-slot-click',session=self.panel(state)['session'],slot=slot,ticks=10)
    def inv(self,slot):self.act('regression-player-click',slot=slot,ticks=10)
    def stable(self,state,baseline):
        return ledger(state)==baseline and all(empty(v['item']) for v in state['cursors'])
    def sources(self,state,suffix):return sum(v.endswith(suffix) for v in state['sources'])
    def seed(self,index,item,group,marker):
        validation='{valid_slots:["'+group+'"],invalid_slots:[]}'
        custom='{release_accessories_marker:'+str(marker)+',typed:[B;2b,8b]}'
        data=('{components:{"accessories:slot_validation":'+validation+'},release_accessories_marker:'+str(marker)+',typed:[B;2b,8b]}') if self.mc=='1.20.1' else '[accessories:slot_validation='+validation+',minecraft:custom_data='+custom+']'
        self.cmd(f'item replace entity @p container.{index} with minecraft:{item}{data}')
    def run(self):
        self.act('close',ticks=3);self.act('fixture',block='minecraft:stone',ticks=4)
        self.cmd('gamemode survival @p');self.cmd('clear @p');self.act('regression-character',ticks=12);self.open()
        initial=self.state('initial');panel=self.panel(initial);cells=panel['cells']
        normal=next(c for c in cells if not c['cosmetic']);cosmetic=next(c for c in cells if c['cosmetic'] and c['group']==normal['group'] and c['index']==normal['index'])
        group=normal['group'];index=normal['index'];ni=normal['slot'];ci=cosmetic['slot']
        self.check('paired-cells-have-real-native-ownership-and-distinct-hit-areas',ni!=ci and cosmetic['x']-normal['x']==19 and cosmetic['y']==normal['y'] and len(initial['groups'][group]['normal'])>index and empty(initial['groups'][group]['normal'][index]) and empty(initial['groups'][group]['cosmetic'][index]))
        self.seed(9,'stick',group,51);self.seed(10,'feather',group,52)
        foreign='{release_accessories_marker:53,typed:[B;2b,8b]}'
        self.cmd('item replace entity @p container.20 with minecraft:diamond'+(foreign if self.mc=='1.20.1' else '[minecraft:custom_data='+foreign+']')+' 3')
        seeded=self.state('seeded');baseline=ledger(seeded)
        expected_normal=seeded['inventory'][9]['item'];expected_cosmetic=seeded['inventory'][10]['item']
        self.check('player-fixtures-contain-real-typed-native-validation-items',item_count(expected_normal)==('minecraft:stick',1) and item_count(expected_cosmetic)==('minecraft:feather',1) and item_count(seeded['inventory'][20]['item'])==('minecraft:diamond',3) and 'accessories:slot_validation' in expected_normal and 'accessories:slot_validation' in expected_cosmetic and 'release_accessories_marker' in expected_normal and 'release_accessories_marker' in expected_cosmetic and self.stable(seeded,baseline))
        self.inv(9);self.slot(seeded,ni);self.inv(10);self.slot(seeded,ci)
        inserted=self.state('inserted')
        def equipped(d):return stack(d['groups'][group]['normal'][index])==stack(expected_normal) and stack(d['groups'][group]['cosmetic'][index])==stack(expected_cosmetic)
        self.check('normal-and-cosmetic-insertion-updates-the-original-native-inventories',equipped(inserted) and empty(inserted['inventory'][9]['item']) and empty(inserted['inventory'][10]['item']) and self.stable(inserted,baseline))
        before=inserted['groups'][group]['render'][index]
        self.act('regression-accessories-visibility-tap',session=self.panel(inserted)['session'],slot=ni,ticks=12)
        hidden=self.state('visibility-toggled')
        self.check('visibility-click-updates-native-and-client-render-state',hidden['groups'][group]['render'][index]!=before and next(c for c in self.panel(hidden)['cells'] if c['slot']==ni)['render']==hidden['groups'][group]['render'][index] and self.stable(hidden,baseline))
        self.act('regression-accessories-visibility-tap',session=self.panel(hidden)['session'],slot=ni,ticks=12)
        restored=self.state('visibility-restored');self.check('visibility-restores-without-editing-equipment',restored['groups'][group]['render'][index]==before and equipped(restored) and self.stable(restored,baseline))
        if restored['client']['characterLocked']:self.act('regression-builtin-control',kind='CHARACTER',control='LOCK',ticks=4)
        self.act('regression-builtin-move',kind='CHARACTER',dx=16,dy=12,ticks=8)
        moved=self.state('moved');old=restored['client']['character'];new=moved['client']['character'];a=self.panel(restored)['bounds'];b=self.panel(moved)['bounds']
        self.check('paired-equipment-panel-follows-real-parent-header-drag',new!=old and b['x']-a['x']==new['x']-old['x'] and b['y']-a['y']==new['y']-old['y'] and equipped(moved) and self.stable(moved,baseline))
        self.act('regression-character-info',ticks=2);attachments=json.loads((self.profile/'regression-character.json').read_text())['attachments']
        peers=[('.curios.CuriosCharacterButton','.CuriosDesktopMenu'),('.aether.AetherEquipmentClient$Cloud','.AetherEquipmentMenu')]
        for attachment,model in peers:
            if not any(a['type'].endswith(attachment) for a in attachments):continue
            self.act('regression-attachment-click',attachment=attachment,ticks=18);swapped=self.state('swapped-'+model[1:])
            self.check('native-equipped-data-survives-exclusive-swap-to-'+model,not swapped['client']['panels'] and self.sources(swapped,'.AccessoriesDesktopMenu')==0 and self.sources(swapped,model)==1 and equipped(swapped) and self.stable(swapped,baseline))
            self.open();back=self.state('restored-'+model[1:])
            self.check('return-to-accessories-closes-the-other-editing-surface-'+model,self.sources(back,model)==0 and self.sources(back,'.AccessoriesDesktopMenu')==1 and len(back['client']['panels'])==1 and equipped(back) and self.stable(back,baseline))
        self.act('regression-character-control',control='CLOSE',ticks=16);closed=self.state('parent-closed')
        self.check('parent-close-removes-editing-source-without-losing-native-gear',not closed['client']['panels'] and self.sources(closed,'.AccessoriesDesktopMenu')==0 and equipped(closed) and self.stable(closed,baseline))
        self.act('regression-character',ticks=12);self.open();reopened=self.state('reopened')
        self.check('native-normal-cosmetic-and-visibility-state-persist-after-reopening',equipped(reopened) and reopened['groups'][group]['render'][index]==before and self.stable(reopened,baseline))
        self.slot(reopened,ni);self.inv(9);self.slot(reopened,ci);self.inv(10);recovered=self.state('recovered')
        self.check('both-native-equipment-stacks-return-exactly-once-with-full-data',stack(recovered['inventory'][9]['item'])==stack(expected_normal) and stack(recovered['inventory'][10]['item'])==stack(expected_cosmetic) and empty(recovered['groups'][group]['normal'][index]) and empty(recovered['groups'][group]['cosmetic'][index]) and self.stable(recovered,baseline))
        self.inv(20);self.slot(recovered,ni);refused=self.state('foreign-refused')
        self.check('native-equipment-validator-refuses-foreign-item-with-exact-cursor',empty(refused['groups'][group]['normal'][index]) and stack(refused['canonicalCursor'])==stack(seeded['inventory'][20]['item']) and ledger(refused)==baseline)
        self.inv(20)
        for i in range(3):
            self.open();off=self.state('closed-'+str(i));self.check('repeated-close-cleans-one-equipment-source-'+str(i),not off['client']['panels'] and self.sources(off,'.AccessoriesDesktopMenu')==0 and self.stable(off,baseline))
            self.open();on=self.state('opened-'+str(i));self.check('repeated-open-creates-exactly-one-equipment-source-'+str(i),len(on['client']['panels'])==1 and self.sources(on,'.AccessoriesDesktopMenu')==1 and self.stable(on,baseline))
        self.act('regression-character-control',control='CLOSE',ticks=16);final=self.state('final')
        self.check('final-parent-close-leaves-no-equipment-source-or-carried-items',self.sources(final,'.AccessoriesDesktopMenu')==0 and self.stable(final,baseline))
        self.act('close',ticks=3);self.save(True)
        return dict(cases=1,failed=0,checks=len(self.checks),accessories_equipment_checked=True)


def run(profile,mc,loader,*args,**kw):return Accessories(profile,mc,loader).run()
