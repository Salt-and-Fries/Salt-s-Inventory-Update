"""Populated native AE2 status, ordinary configuration and export controls."""
from pathlib import Path
import copy,hashlib,json,math,zipfile
import nbtlib
from ae2_settings_features import Settings,ENTRIES,isolate,translate,pair,ledger,stack,MARKER,marker_item,item_cell

DOUBLES=['getAveragePowerInjection','getAveragePowerUsage','getStoredPower','getMaxStoredPower','getChannelPower']


def numeric(value,kind,minimum=0):
    data=value['value']
    return value['javaType']==kind and type(data)in([int]if kind=='java.lang.Integer'else[int,float])and math.isfinite(data)and data>=minimum


def valid_status(status):
    if not all(numeric(status[k],'java.lang.Double')for k in DOUBLES):return False
    if not numeric(status['getChannelsUsed'],'java.lang.Integer'):return False
    if not 0<status['getStoredPower']['value']<=status['getMaxStoredPower']['value']:return False
    groups=status['groups']
    if not groups:return False
    seen=set()
    for group in groups:
        missing=group['isMissingChannel'];key=(group['item'],item_cell(group['display']),missing['value'])
        if missing['javaType']!='java.lang.Boolean'or type(missing['value'])is not bool or key in seen:return False
        seen.add(key)
        if group['display']['count']!=1 or not group['item'].startswith('ae2:'):return False
        if not numeric(group['getCount'],'java.lang.Integer',1):return False
        if not all(numeric(group[k],'java.lang.Double')for k in ['getIdlePowerUsage','getPowerGenerationCapacity']):return False
    return True


def topology(status):
    return status['getChannelsUsed'],tuple(sorted((
        (g['item'],item_cell(g['display']),g['getCount']['value'],g['isMissingChannel']['value'],
         g['getIdlePowerUsage']['value'],g['getPowerGenerationCapacity']['value'])for g in status['groups']),key=repr))


def drive_count(status):return sum(g['getCount']['value']for g in status['groups']if g['item']=='ae2:drive')


def exported_grid(path,serial):
    with zipfile.ZipFile(path)as archive:
        assert archive.testzip()is None,'Native export CRC failure'
        assert len(archive.namelist())==len(set(archive.namelist())),'Duplicate native export member'
        candidates=[]
        for name in archive.namelist():
            if not name.endswith('.json'):continue
            data=json.loads(archive.read(name))
            if isinstance(data,dict)and data.get('id')==serial:candidates.append(data)
        assert len(candidates)==1,'Missing/ambiguous native grid identity'
        data=candidates[0];assert type(data['id'])is int and data['disposed']is False
        assert isinstance(data['nodes'],list)and len(data['nodes'])>=4,'Unpopulated native exported nodes'
        assert isinstance(data['machines'],list)and len(data['machines'])>=4,'Unpopulated native exported machines'
        return dict(file=str(path.resolve()),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),members=archive.namelist(),grid_serial=serial)


class Network(Settings):
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.checks=[];self.n=0;self.case='network-status';self.cases=[];self.entry_records=[];self.baseline=None;self.export=None
        self.out=self.profile/'ae2-network-release';self.out.mkdir();self.command='server-command'if loader=='neoforge'else'command'
        self.act('regression-catalog',ticks=2);self.origin=json.loads((self.profile/'regression-catalog.json').read_text())['origin']
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,original_entries=self.entry_records,export=self.export,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scope='Real powered controller/grid, actual packet-delivered populated status against independent native backend snapshot, two/three/two connected drives, native local power units, moved input and ordinary reopening. Modern native Export Grid button and resulting populated ZIP; pinned legacy releases do not provide that button. Complete typed owned inventory/cursor unchanged. Dynamic energy averages are bounded/finite rather than falsely compared at different ticks. No exhaustive grid topology, large-list scrolling or remote export claim.'),indent=2)+'\n')
    def frame(self,label):
        self.act('regression-ae2-child-state',ticks=3);child=json.loads((self.profile/'release-ae2-child-state.json').read_text())
        self.act('regression-ae2-network-state',ticks=3);p=json.loads((self.profile/'release-ae2-network-state.json').read_text())
        c,s=pair(p,'NetworkStatusMenu');self.client,_=pair(child,'NetworkStatusMenu')
        assert c['session']==self.client['session']and c['screen']==self.client['screen']
        self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(dict(native=p,widgets=child,ledger=data),indent=2)+'\n')
        if self.baseline is not None:assert ledger(data)==self.baseline,'Read-only status input changed full owned items/cursor'
        return c,s,data
    def synced(self,c,s,count):
        return all(valid_status(v['status'])for v in [c,s])and topology(c['status'])==topology(s['status'])and drive_count(s['status'])==count and c['status']['getMaxStoredPower']==s['status']['getMaxStoredPower']
    def toggle(self):
        buttons=[w for w in self.client['widgets']if w.get('currentValue')is not None and w['visible']and w['active']]
        assert len(buttons)==1,'Expected unique native power-unit control';self.tap(self.client,buttons[0])
    def setup(self):
        self.act('close',ticks=3)
        for cmd in isolate(self.origin):self.cmd(cmd)
        self.cmd('clear @a');rows=[v for v in json.loads(ENTRIES.read_text())['cases']if v['id']=='atlas-ae2-controller-network-status'and(v['minecraft'],v['loader'])==(self.mc,self.loader)];assert len(rows)==1
        entry=rows[0];old=[int(v)for v in entry['captured_origin'].split(',')];actions=[]
        for original in entry['actions']:
            action=copy.deepcopy(original);kind=action.pop('type');assert kind in {'fixture','command','open'},('Unreviewed native action',kind)
            if kind=='open':continue # Ordinary actual top-face ray below replaces the historical manufactured hit.
            if kind=='command':action['command']=translate(action['command'],old,self.origin);kind=self.command
            self.act(kind,**action);actions.append(dict(type=kind,**action))
        x,y,z=self.origin
        for cmd in [f'setblock {x-1} {y} {z} ae2:drive',f'setblock {x} {y} {z+1} ae2:drive','gamemode survival @a']:
            self.cmd(cmd);actions.append(dict(type=self.command,command=cmd))
        suffix=MARKER if self.mc=='1.20.1'else'[minecraft:custom_data='+MARKER+']'
        self.cmd('item replace entity @a container.12 with minecraft:diamond'+suffix+' 7')
        self.cmd(f'tp @a {x+.5} {y+2} {z+.5} 0 90');self.act('wait',ticks=60);self.act('use-block-ray',ticks=35)
        c,s,data=self.frame('actual-populated-status');sentinel=next(v for v in data['inventory']if v['index']==12)
        self.check('actual-ray native populated status and exact typed foreign item',self.client['screen']==entry['expected_native_screen']and self.client['menu']==entry['expected_native_menu']and stack(sentinel['signature'],sentinel['count'])==stack(marker_item(self.mc))and data['cursor']['count']==0)
        self.baseline=ledger(data);self.entry_records.append(dict(original=entry,executed_world_actions=actions,entry='Ordinary actual top-face ray; no manufactured hit, status assignment, screen/provider construction or action-handler call',actual_runtime_staging='staged-artifacts.json'))
        self.check('actual packet status matches stable native topology with two drives',self.synced(c,s,2))
        return c,s
    def run(self):
        c,s=self.setup();original=c['selectedEnergyUnit'];self.toggle();c,s,_=self.frame('power-unit-changed')
        buttons=[w for w in self.client['widgets']if w.get('currentValue')==c['selectedEnergyUnit']and w['visible']and w['active']]
        self.check('original power-unit input changes the actual native local config',c['selectedEnergyUnit']!=original and len(buttons)==1 and self.synced(c,s,2))
        unit=c['selectedEnergyUnit'];before=self.client['bounds'];self.move(self.client);c,s,_=self.frame('status-moved')
        self.check('status moves through actual unlocked drag without changing data',self.client['bounds']['x']==before['x']+16 and self.client['bounds']['y']==before['y']+12 and self.synced(c,s,2))
        self.toggle();c,s,_=self.frame('moved-unit-input')
        self.check('moved native power-unit control remains usable',c['selectedEnergyUnit']!=unit and self.synced(c,s,2));unit=c['selectedEnergyUnit']
        x,y,z=self.origin;self.cmd(f'setblock {x} {y} {z-1} ae2:drive');self.act('wait',ticks=45);c,s,_=self.frame('native-topology-added')
        self.check('original status packet refreshes after a real connected drive is added',self.synced(c,s,3))
        self.cmd(f'setblock {x} {y} {z-1} minecraft:air');self.act('wait',ticks=45);c,s,_=self.frame('native-topology-removed')
        self.check('original status refreshes after the extra native drive is removed',self.synced(c,s,2))
        self.reopen();self.act('wait',ticks=35);c,s,_=self.frame('ordinary-status-reopened')
        self.check('ordinary native reopening retains units and populated original grid',c['selectedEnergyUnit']==unit and self.synced(c,s,2))
        if self.mc!='1.20.1':
            buttons=[w for w in self.client['widgets']if w['message']=='Export Grid'and w['visible']and w['active']]
            self.check('original modern export button is available with native operator permission',c['canExportGrid']is True and len(buttons)==1)
            directory=Path(s['serverDirectory']);serial=s['gridSerial'];assert type(serial)is int
            prefix='ae2_grid_'+str(serial)+'_';before={p.resolve():hashlib.sha256(p.read_bytes()).hexdigest()for p in directory.glob(prefix+'*.zip')}
            self.tap(self.client,buttons[0]);self.act('wait',ticks=35)
            changed=[p for p in directory.glob(prefix+'*.zip')if before.get(p.resolve())!=hashlib.sha256(p.read_bytes()).hexdigest()];assert len(changed)==1,'Native export did not produce exactly one new/changed archive'
            self.export=exported_grid(changed[0],serial)
            self.check('native export produces a valid populated archive for the exact backing grid',self.export['grid_serial']==serial)
            self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text())
            (self.out/'after-native-export.json').write_text(json.dumps(data,indent=2)+'\n')
            self.check('native export closes its original editing session and preserves all owned items',not data['menus']and ledger(data)==self.baseline)
        else:
            self.check('pinned legacy status correctly has no invented export button',c['exportUnavailableOnPinnedRelease']is True and not any(w['message']=='Export Grid'for w in self.client['widgets']))
            self.act('close',ticks=4);self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text())
            self.check('legacy native close preserves all owned items and cursor',not data['menus']and ledger(data)==self.baseline)
        self.cases=[self.case];self.save(True);return dict(cases=1,failed=0,checks=len(self.checks),ae2_network_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Network(profile,mc,loader).run()
