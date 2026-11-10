"""Native fallback and protocol refusal over a real dedicated connection."""
from pathlib import Path
import argparse,hashlib,json,msvcrt,os,re,subprocess,time
from run import Campaign,Client,ROOT

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',required=True);a=parser.parse_args()
    campaign=Campaign(ROOT/'output/release-readiness'/a.run);mode=campaign.manifest['mode']
    inputs=[Path(__file__),Path(__file__).with_name('prepare_installation.py'),Path(__file__).with_name('run.py'),
            Path(__file__).with_name('prepare.py'),Path(__file__).with_name('AtomicObservation.java'),Path(__file__).with_name('InstallationClient.java')]
    campaign.manifest['qa_inputs']={str(path.relative_to(ROOT)):hashlib.sha256(path.read_bytes()).hexdigest() for path in inputs};campaign.save()
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            campaign.expect('current-observer-matches-the-prepared-excluded-client',hashlib.sha256(inputs[-1].read_bytes()).hexdigest()==campaign.manifest['source_sha256'])
            for subject in campaign.manifest['subjects']:
                actual=Path(campaign.manifest['launches'][subject['profile']]['cwd'])/'mods'/subject['filename']
                campaign.expect('exact-'+subject['profile']+'-'+subject['family'],hashlib.sha256(actual.read_bytes()).hexdigest()==subject['sha256']
                    and hashlib.sha256(Path(subject['file']).read_bytes()).hexdigest()==subject['sha256'])
            launch=campaign.manifest['launches']['server'];mods=Path(launch['cwd'])/'mods'
            campaign.expect('server-has-only-the-intended-production-modules-and-no-QA-mod',
                {p.name for p in mods.glob('*.jar')}=={s['filename'] for s in campaign.manifest['subjects'] if s['profile']=='server'})
            campaign.server_log=(campaign.folder/'server/launch.log').open('x')
            campaign.server=subprocess.Popen([launch['java'],'@'+launch['arguments']],cwd=launch['cwd'],stdout=campaign.server_log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
            deadline=time.monotonic()+180
            while not re.search(r'Done \([^)]+\)!',(campaign.folder/'server/launch.log').read_text(errors='replace')):
                if campaign.server.poll() is not None:raise RuntimeError('Dedicated installation server exited')
                if time.monotonic()>deadline:raise TimeoutError('Dedicated installation server readiness')
                time.sleep(.25)
            client=Client('a',campaign.folder/'a',campaign.manifest['launches']['a']);campaign.clients['a']=client;client.start()
            campaign.command('forceload add 0 0 20 20')
            for text in ['fill 0 64 0 20 64 20 minecraft:stone','setblock 8 65 8 minecraft:barrel']:
                result=campaign.command(text);campaign.expect('native-world-setup-'+text.split(' ')[0],not any(v in result.lower() for v in ['not loaded','incorrect argument','not a container']))
            campaign.command('gamemode survival SIUPlayerA');campaign.command('tp SIUPlayerA 8.5 65 10.5 180 0');campaign.command('clear SIUPlayerA')
            state=client.send(ticks=50)
            campaign.expect('real-separate-dedicated-connection',state['connected'] and '1 of a max' in campaign.command('list'))
            campaign.expect('installed-side-and-negotiation-match-the-profile',state['corePresent']==(mode=='client-only') and not state['serverSessions'])
            if mode=='protocol-mismatch':
                state=client.send('bad-protocol',ticks=45)
                campaign.expect('wrong-wire-version-is-refused-with-an-actionable-native-message',
                    not state['connected'] and not state['serverSessions'] and state['screen'].endswith('.DisconnectedScreen')
                    and any(text in state['screenText'].lower() for text in ['incompatible','update','version','protocol']),screen=state)
                campaign.expect('rejected-peer-has-no-native-storage-edit',not campaign.block())
            else:
                marker='minecraft:diamond[minecraft:custom_data={installation_marker:73,nested:{values:[I;1,2,3]}}]'
                campaign.command('item replace block 8 65 8 container.0 with '+marker+' 7');original=campaign.block()[0]
                state=client.send('use-block',ticks=24)
                campaign.expect('missing-peer-module-preserves-the-native-container-and-input',
                    state.get('nativeSlotCount')==63 and state.get('nativeMenu','').endswith('.ChestMenu') and not state['serverSessions'],screen=state)
                state=client.send('native-slot',slot=0,ticks=16)
                campaign.expect('native-withdrawal-has-no-hidden-desktop-duplicate',state['cursor']=={'item':'minecraft:diamond','count':7} and not campaign.block())
                state=client.send('native-slot',slot=27,ticks=16);owned=campaign.inventory('SIUPlayerA')
                campaign.expect('native-separate-inventory-transfer-preserves-every-component',
                    campaign.counts(owned)=={'minecraft:diamond':7} and owned[0]['_native_components']==original['_native_components'] and state['cursor']['count']==0)
                client.send('close',ticks=8);state=client.send('use-block',ticks=16)
                campaign.expect('native-close-and-reopen-keep-fallback-without-loss',state.get('nativeSlotCount')==63 and not campaign.block()
                    and campaign.counts(campaign.inventory('SIUPlayerA'))=={'minecraft:diamond':7})
                client.send('close',ticks=6)
            campaign.close()
            campaign.expect('dedicated-server-stops-cleanly-without-client-class-or-integration-failure',campaign.server.returncode==0
                and not any(text in (campaign.folder/'server/launch.log').read_text(errors='replace') for text in ['Unable to initialize optional integration','Attempted to load class','Mixin apply failed']))
            campaign.expect('qualification-inputs-unchanged',all(hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==sha for path,sha in campaign.manifest['qa_inputs'].items()))
            campaign.manifest.update(status='passed',finished=time.time());campaign.save()
        except Exception as error:
            campaign.manifest.update(status='failed',error=repr(error),finished=time.time());campaign.save()
            try:campaign.close()
            except Exception as cleanup:campaign.manifest['cleanup_error']=repr(cleanup);campaign.save()
            raise
        finally:lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)

if __name__=='__main__':main()
