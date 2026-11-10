"""Two real client JVMs, exact production server jars, native input and RCON ledgers."""
from pathlib import Path
import argparse
import collections
import hashlib
import importlib.util
import json
import msvcrt
import os
import re
import subprocess
import sys
import time
import uuid
import nbtlib

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('release_remote_rcon',ROOT/'tools/verify-spatial-server.py')
native=importlib.util.module_from_spec(spec);spec.loader.exec_module(native)

def typed_nbt(value):
    """Retain native scalar/array/list types while ignoring compound key order."""
    if isinstance(value,nbtlib.Compound):return ['compound',{str(k):typed_nbt(v) for k,v in sorted(value.items())}]
    if isinstance(value,nbtlib.List):return ['list:'+value.subtype.__name__,[typed_nbt(v) for v in value]]
    if hasattr(value,'tolist'):return [type(value).__name__,value.tolist()]
    return [type(value).__name__,value.unpack()]

def native_items(value):
    result=[]
    for tag in value:
        raw=json.loads(json.dumps(tag.unpack(),default=lambda v:v.tolist()))
        if 'components' in tag:raw['_native_components']=typed_nbt(tag['components'])
        result.append(raw)
    return result

def offline_uuid(name):return uuid.UUID(bytes=hashlib.md5(('OfflinePlayer:'+name).encode()).digest(),version=3)

class Client:
    def __init__(self,name,folder,launch):self.name=name;self.folder=folder;self.launch=launch;self.sequence=0;self.latest={};self.process=None
    def start(self):
        self.log=(self.folder/'launch.log').open('w')
        self.process=subprocess.Popen([self.launch['java'],'@'+self.launch['arguments']],cwd=self.launch['cwd'],
            env={**os.environ,**self.launch['environment']},stdout=self.log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
        deadline=time.monotonic()+240
        while not (self.folder/'ready.json').exists():
            self.alive()
            if time.monotonic()>deadline:raise TimeoutError('Remote login '+self.name)
            time.sleep(.25)
        self.send()
    def alive(self):
        failure=self.folder/'failure.json'
        if failure.exists():raise RuntimeError(failure.read_text())
        if self.process.poll() is not None:raise RuntimeError('Remote client exited '+self.name+' code='+str(self.process.returncode))
    def publish(self,kind='state',**values):
        self.alive();self.sequence+=1;data=dict(sequence=self.sequence,type=kind,ticks=values.pop('ticks',12),**values)
        path=self.folder/'command.tmp';path.write_text(json.dumps(data));path.replace(self.folder/'command.json')
        return data
    def await_reply(self):
        deadline=time.monotonic()+90
        while True:
            self.alive();path=self.folder/'reply.json'
            if path.exists():
                reply=json.loads(path.read_text())
                if reply['sequence']==self.sequence:
                    self.latest=reply['state']
                    with (self.folder/'journal.jsonl').open('a') as stream:
                        stream.write(json.dumps(dict(command=json.loads((self.folder/'command.json').read_text()),reply=reply))+'\n')
                    return self.latest
            if time.monotonic()>deadline:raise TimeoutError('Original remote input decision '+self.name+' '+str(self.sequence))
            time.sleep(.08)
    def send(self,kind='state',**values):self.publish(kind,**values);return self.await_reply()
    def source(self,session=None):
        return next(w for w in self.latest['windows'] if w.get('session') and (session is None or w['session']==session))
    def inventory(self):
        if not any(w['kind']=='PLAYER_INVENTORY' for w in self.latest['windows']):self.send('inventory')
        return next(w for w in self.latest['windows'] if w['kind']=='PLAYER_INVENTORY')
    def slot_command(self,window,index,*,player=False,button=0):
        current=next(w for w in self.latest['windows'] if w['id']==window['id'])
        cell=next(c for c in current['slots'] if c.get('inventorySlot' if player else 'menuSlot')==index)
        return dict(window=window['id'],x=cell['x']+8,y=cell['y']+8,button=button)
    def slot(self,window,index,**values):return self.send('tap',**self.slot_command(window,index,**values))
    def player(self,index,button=0):return self.slot(self.inventory(),index,player=True,button=button)
    def control(self,window,name):
        def find(label):
            current=next(w for w in self.latest['windows'] if w['id']==window['id'])
            return next((c for c in current['controls'] if c['control']==label),None)
        chosen=find(name)
        if chosen is None:
            more=find('ELLIPSIS');assert more,'Control missing without overflow menu: '+name
            self.send('tap',window=window['id'],x=more['x']+more['width']/2,y=more['y']+more['height']/2)
            chosen=find(name)
        assert chosen,'Native control not visible: '+name
        return self.send('tap',window=window['id'],x=chosen['x']+chosen['width']/2,y=chosen['y']+chosen['height']/2)
    def finish(self):
        if self.process and self.process.poll() is None:
            self.publish('finish')
            try:self.process.wait(timeout=60)
            except subprocess.TimeoutExpired:
                subprocess.run(['taskkill','/PID',str(self.process.pid),'/T','/F'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
                raise RuntimeError('Owned remote client failed to stop cleanly '+self.name)
        if self.process and self.process.returncode!=0:raise RuntimeError('Remote client nonzero exit '+self.name)
        if hasattr(self,'log'):self.log.close()

class Campaign:
    def __init__(self,folder):
        self.folder=folder;self.manifest=json.loads((folder/'manifest.json').read_text());self.checks=[];self.clients={};self.server=None;self.commands=[]
        assert self.manifest['status']=='prepared-not-run','Use a fresh prepared campaign'
        self.manifest.update(status='running',started=time.time());self.save()
    def save(self):
        (self.folder/'manifest.json').write_text(json.dumps({**self.manifest,'checks':self.checks},indent=2)+'\n')
    def expect(self,name,condition,**details):
        self.checks.append(dict(name=name,passed=bool(condition),**details));self.save();assert condition,name
        print('PASS',name,flush=True)
    def command(self,text):
        rcon=native.Rcon();rcon.port=25588;rcon.password='release-local-qa-only';rcon.command_timeout=60
        with rcon as connection:result=connection.command(text)
        self.commands.append(dict(command=text,result=result));(self.folder/'rcon-journal.json').write_text(json.dumps(self.commands,indent=2)+'\n')
        return result
    def nbt(self,text):
        result=self.command(text);prefix,separator,value=result.partition(': ')
        if not separator:raise AssertionError(result)
        return native_items(nbtlib.parse_nbt(value))
    def inventory(self,name):return self.nbt('data get entity '+name+' Inventory')
    def block(self,x=8,y=65,z=8):return self.nbt(f'data get block {x} {y} {z} Items')
    def dropped_gold(self):
        result=self.command('data get entity @e[type=minecraft:item,nbt={Item:{id:"minecraft:gold_ingot"}},limit=1] Item')
        if result.startswith('No entity was found'):return []
        prefix,separator,value=result.partition(': ');assert separator,result
        return native_items([nbtlib.parse_nbt(value)])
    @staticmethod
    def counts(items):
        counts=collections.Counter()
        for stack in items:counts[stack['id']]+=int(stack.get('count',stack.get('Count',0)))
        return counts
    def open(self,client,x=8,y=65,z=8):
        old={w['id'] for w in client.latest.get('windows',[])}
        client.send('use-block',x=x,y=y,z=z,ticks=24)
        return next(w for w in client.latest['windows'] if w.get('session') and w['id'] not in old)
    def start(self):
        sources=[Path(__file__),Path(__file__).with_name('prepare.py'),Path(__file__).with_name('RemoteClient.java'),Path(__file__).with_name('AtomicObservation.java')]
        self.manifest['qa_inputs']={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}
        self.expect('prepared-client-source-is-the-reviewed-current-observer',
            hashlib.sha256(Path(__file__).with_name('RemoteClient.java').read_bytes()).hexdigest()==self.manifest['source_sha256'])
        self.expect('dedicated-server-contains-no-QA-mod',
            {p.name for p in (Path(self.manifest['launches']['server']['cwd'])/'mods').glob('*.jar')}
            =={s['filename'] for s in self.manifest['subjects'] if s['profile']=='server'})
        for subject in self.manifest['subjects']:
            launch=self.manifest['launches'][subject['profile']];path=Path(launch['cwd'])/'mods'/subject['filename']
            self.expect('exact-'+subject['profile']+'-'+subject['family'],hashlib.sha256(path.read_bytes()).hexdigest()==subject['sha256']
                and hashlib.sha256(Path(subject['file']).read_bytes()).hexdigest()==subject['sha256'])
        launch=self.manifest['launches']['server'];self.server_log=(self.folder/'server/launch.log').open('w')
        self.server=subprocess.Popen([launch['java'],'@'+launch['arguments']],cwd=launch['cwd'],stdout=self.server_log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
        deadline=time.monotonic()+180
        while not re.search(r'Done \([^)]+\)!',(self.folder/'server/launch.log').read_text(errors='replace')):
            if self.server.poll() is not None:raise RuntimeError('Dedicated server exited before readiness')
            if time.monotonic()>deadline:raise TimeoutError('Production dedicated server readiness')
            time.sleep(.25)
        for name in ['a','b']:
            client=Client(name,self.folder/name,self.manifest['launches'][name]);self.clients[name]=client;client.start()
        self.expect('two-real-separate-clients-share-one-dedicated-server','2 of a max' in self.command('list')
            and all(c.latest['dedicated'] and c.latest['serverSessions'] for c in self.clients.values()))
        # Modern dedicated servers do not retain spawn chunks before anyone
        # joins. Establish fixtures only after ordinary login loads the area.
        for command in ['time set day','weather clear','fill 0 64 0 20 64 20 minecraft:stone',
                        'setblock 8 65 8 minecraft:barrel','setblock 10 65 8 minecraft:barrel','setblock 12 65 8 minecraft:furnace']:
            result=self.command(command)
            self.expect('native-world-setup-'+command.split(' ')[0],not any(text in result.lower() for text in ['not loaded','incorrect argument','not a container']))
        for name in ['SIUPlayerA','SIUPlayerB']:
            self.command(f'gamemode survival {name}');self.command(f'tp {name} 9.5 65 10.5 180 0');self.command(f'clear {name}')
        for client in self.clients.values():client.send(ticks=16)
    def execute(self):
        a,b=self.clients['a'],self.clients['b']
        marker='minecraft:diamond[minecraft:custom_data={siu_remote:42,nested:{text:"all components",values:[I;1,2,3]}}]'
        self.command('item replace block 8 65 8 container.0 with '+marker+' 32')
        original=self.block()[0];wa=self.open(a);wb=self.open(b)
        self.expect('different-owners-can-have-equal-session-and-slot-indices-with-distinct-tokens',wa['session']==wb['session']
            and wa['sessionToken']!=wb['sessionToken'] and any(c['menuSlot']==0 for c in wa['slots']) and any(c['menuSlot']==0 for c in wb['slots']))
        a.send('move',window=wa['id'],x=32,y=40);b.send('move',window=wb['id'],x=120,y=50)
        wa=a.source(wa['session']);wb=b.source(wb['session'])
        self.expect('independent-players-move-their-real-window-hit-areas',wa['state']['x']==32 and wb['state']['x']==120)
        b.send('forged-close',connectionNonce=b.latest['connectionNonce'],session=wa['session'],sessionToken=wa['sessionToken'])
        a.send();self.expect('another-players-token-cannot-close-either-equal-numbered-source',
            any(w['id']==wa['id'] for w in a.latest['windows']) and any(w['id']==wb['id'] for w in b.latest['windows']))
        b.send('forged-close',connectionNonce=a.latest['connectionNonce'],session=wb['session'],sessionToken=wb['sessionToken'])
        self.expect('foreign-connection-nonce-cannot-close-the-current-owners-source',any(w['id']==wb['id'] for w in b.latest['windows']))
        a.publish('tap',**a.slot_command(wa,0));b.publish('tap',**b.slot_command(wb,0));a.await_reply();b.await_reply()
        amounts=[c.latest['cursor']['count'] for c in [a,b]]
        self.expect('concurrent-withdrawal-has-one-winner-and-no-duplicated-cursor',sorted(amounts)==[0,32] and not self.block())
        winner,other=(a,b) if amounts[0]==32 else (b,a);winner.player(9)
        self.expect('authoritative-server-ledger-has-exactly-one-complete-withdrawn-stack',
            self.counts(self.inventory('SIUPlayerA'))+self.counts(self.inventory('SIUPlayerB'))=={'minecraft:diamond':32})
        owned=(self.inventory('SIUPlayerA')+self.inventory('SIUPlayerB'))[0]
        self.expect('concurrent-transfer-preserves-all-native-item-components',owned['_native_components']==original['_native_components'])
        winner.player(9);ws=winner.source();winner.slot(ws,0,button=1);winner.slot(ws,1)
        split=self.block();self.expect('native-right-and-left-deposits-preserve-exact-split-and-components',
            sorted(int(s['count']) for s in split)==[1,31] and all(s['_native_components']==original['_native_components'] for s in split))
        other.send();other.slot(other.source(),1,button=1);other.player(10)
        winner.slot(winner.source(),0);winner.player(10)
        self.expect('cross-player-split-pickup-conserves-all-items-on-the-server',
            self.counts(self.block())+self.counts(self.inventory('SIUPlayerA'))+self.counts(self.inventory('SIUPlayerB'))=={'minecraft:diamond':32})
        self.command('item replace block 8 65 8 container.10 with minecraft:dirt 16')
        self.command('item replace block 8 65 8 container.12 with minecraft:dirt 32')
        a.send(ticks=16);a.control(a.source(wa['session']),'SORT_INVENTORY')
        sorted_stock=self.block()
        self.expect('real-sort-control-merges-native-stock-without-changing-metadata-or-another-player',
            self.counts(sorted_stock)=={'minecraft:diamond':15,'minecraft:dirt':48}
            and sum(s['id']=='minecraft:dirt' for s in sorted_stock)==1
            and next(s for s in sorted_stock if s['id']=='minecraft:diamond')['_native_components']==original['_native_components'])
        # Native output slots are shared too. World setup supplies a completed
        # output, not a fake recipe provider or a direct output pickup.
        self.command('item replace block 12 65 8 container.2 with minecraft:iron_ingot 7')
        fa=self.open(a,12);fb=self.open(b,12)
        a.publish('tap',**a.slot_command(fa,2));b.publish('tap',**b.slot_command(fb,2));a.await_reply();b.await_reply()
        amounts=[c.latest['cursor']['count'] for c in [a,b]]
        self.expect('simultaneous-native-output-pickup-is-linearized-on-the-dedicated-server',sorted(amounts)==[0,7] and not self.block(12))
        output_owner=a if amounts[0]==7 else b;output_owner.player(12)
        self.expect('shared-output-is-owned-once-after-settlement',
            (self.counts(self.inventory('SIUPlayerA'))+self.counts(self.inventory('SIUPlayerB')))['minecraft:iron_ingot']==7)
        # A carried withdrawal must survive the actual connection ending.
        self.command('item replace block 10 65 8 container.0 with minecraft:gold_ingot[minecraft:custom_data={disconnect_marker:91}] 5')
        gold_original=self.block(10)[0]
        isolated=self.open(a,10);a.slot(isolated,0)
        self.expect('disconnect-fixture-is-a-real-native-withdrawal',a.latest['cursor']['count']==5 and not self.block(10))
        self.command('tp SIUPlayerB 13.5 65 13.5 180 0')
        a.finish();self.expect('other-player-remains-connected-after-owner-disconnect','1 of a max' in self.command('list') and b.send()['connected'])
        self.command('save-all flush')
        # Minecraft 26.3 moved player data to players/data. Vanilla settles a
        # disconnected player's cursor by dropping it, rather than inserting
        # it into the offline inventory. Count the authoritative native drop.
        player_file=Path(self.manifest['launches']['server']['cwd'])/'world/players/data'/(str(offline_uuid('SIUPlayerA'))+'.dat')
        saved=nbtlib.load(player_file).unpack().get('Inventory',[]);drop=self.dropped_gold()
        self.expect('disconnect-settles-the-carried-stack-once-through-the-native-drop-rule',
            self.counts(saved)['minecraft:gold_ingot']==0 and self.counts(self.inventory('SIUPlayerB'))['minecraft:gold_ingot']==0
            and self.counts(drop)['minecraft:gold_ingot']==5 and drop[0]['_native_components']==gold_original['_native_components'])
        self.manifest['disconnect_settlement']=dict(native_behavior='Vanilla drops the carried stack on disconnect; it is absent from the saved player inventory.',
            saved_player_file=str(player_file),saved_player_sha256=hashlib.sha256(player_file.read_bytes()).hexdigest(),dropped_stack=drop);self.save()
        reconnect=Client('a-reconnect',self.folder/'a-reconnect',self.manifest['launches']['a-reconnect']);self.clients['a-reconnect']=reconnect;reconnect.start()
        self.expect('reconnect-restores-the-same-player-with-no-duplicate-cursor',
            sum(s['count'] for s in reconnect.latest['inventory'] if s['item']=='minecraft:gold_ingot')==0
            and reconnect.latest['cursor']['count']==0 and '2 of a max' in self.command('list'))
        self.command('tp SIUPlayerA 9.5 65 7 180 0');reconnect.send(ticks=50)
        restored=self.inventory('SIUPlayerA')
        self.expect('reconnected-owner-recovers-the-native-dropped-stack-once-with-complete-components',
            self.counts(restored)['minecraft:gold_ingot']==5 and not self.dropped_gold()
            and next(s for s in restored if s['id']=='minecraft:gold_ingot')['_native_components']==gold_original['_native_components'])
        invalidated=self.open(reconnect,10);survivor=self.open(reconnect)
        self.command('setblock 10 65 8 minecraft:air');reconnect.send(ticks=40)
        self.expect('native-source-invalidation-closes-only-that-source',
            not any(w['id']==invalidated['id'] for w in reconnect.latest['windows'])
            and any(w['id']==survivor['id'] for w in reconnect.latest['windows']))
        final=self.counts(self.block())+self.counts(self.inventory('SIUPlayerA'))+self.counts(self.inventory('SIUPlayerB'))
        self.expect('final-server-ledger-preserves-all-three-owned-item-families',
            final=={'minecraft:diamond':32,'minecraft:dirt':48,'minecraft:iron_ingot':7,'minecraft:gold_ingot':5},ledger=dict(final))
    def close(self):
        failures=[]
        for client in self.clients.values():
            try:client.finish()
            except Exception as error:failures.append(str(error))
        if self.server and self.server.poll() is None:
            try:self.command('stop');self.server.wait(timeout=90)
            except Exception as error:
                failures.append(str(error));subprocess.run(['taskkill','/PID',str(self.server.pid),'/T','/F'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        if hasattr(self,'server_log'):self.server_log.close()
        if failures:raise RuntimeError('; '.join(failures))

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',required=True);args=parser.parse_args()
    assert all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    campaign=Campaign(ROOT/'output/release-readiness'/args.run)
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            campaign.start();campaign.execute();campaign.close()
            server_log=(campaign.folder/'server/launch.log').read_text(errors='replace')
            campaign.expect('production-server-stops-without-caught-SIU-integration-or-client-class-failures',
                'Unable to initialize optional integration' not in server_log and 'Attempted to load class' not in server_log
                and 'Mixin apply failed' not in server_log and campaign.server.returncode==0)
            campaign.expect('QA-source-inputs-remained-unchanged-through-the-campaign',all(
                hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==fingerprint for path,fingerprint in campaign.manifest['qa_inputs'].items()))
            campaign.manifest.update(status='passed',finished=time.time());campaign.save()
        except Exception as error:
            campaign.manifest.update(status='failed',error=repr(error),finished=time.time());campaign.save()
            try:campaign.close()
            except Exception as cleanup:campaign.manifest['cleanup_error']=repr(cleanup);campaign.save()
            raise
        finally:lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)

if __name__=='__main__':main()
