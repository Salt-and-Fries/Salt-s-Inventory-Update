"""Original Primordial Forge and Serkonid entries, for paired UI pictures only."""
from pathlib import Path
import argparse,sys,time
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--shop-only',action='store_true');a=p.parse_args()
deadline=time.monotonic()+300
while not (a.run/'ready.json').exists():
 if time.monotonic()>deadline:raise TimeoutError('Prominent reference world did not become ready')
 time.sleep(1)
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def command(text,ticks=5):return act('command',command=text,ticks=ticks)
def picture(ui,suffix):
 s=act('wait',ticks=12)
 screens=[w.get('nativeScreen','') for w in s.get('windows',[])] if a.side=='siu' else [s.get('screen','')]
 assert any(n.endswith('.'+suffix) for n in screens),screens
 act('move',x=5,y=5,ticks=3)
 capture(a.run,a.side,'1.20.1','fabric',ui,'Original embedded Prominent Base 4.1.5 and unchanged parent data definitions. Operator commands seed only disposable progression/currency; ordinary block or entity interactions open the publisher UI. Pictured layout only, no complete transaction or gameplay suite.')
for c in ['gamerule sendCommandFeedback false','gamerule announceAdvancements false','time set day','weather clear','fill 5 64 5 12 64 14 minecraft:stone','gamemode survival @a']:
 command(c)
for stage,ui,suffix in ([] if a.shop_only else [(0,'prominent-forge-upgrade','PrimordialForgeUpgradeScreen'),(2,'prominent-forge-armor','PrimordialForgeScreen')]):
 act('close',ticks=3);act('held',item='minecraft:air',ticks=3);act('fixture',block='prominent:primordial_forge',ticks=8)
 command('prominent:set_primordial_forge_unlocked @p true');command('prominent:set_primordial_forge_stage @p '+str(stage));command('prominent:cinders @p set 0')
 act('client-use-block' if a.side=='siu' else 'open',ticks=25);picture(ui,suffix)
act('close',ticks=3);act('held',item='minecraft:air',ticks=3)
command('fill 5 65 5 12 69 12 minecraft:air',ticks=8) # Remove the prior forge's tall multiblock before the trader ray.
act('fixture',block='minecraft:air',ticks=6)
for c in ['kill @e[type=prominent:serkonid]','advancement grant @a only minecraft:end/kill_dragon','prominent:cinders @p set 1234',
          'summon prominent:serkonid 8.5 65 8.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,trades:"prominent:past/treasurer",trades_timestamp:'+str(int(time.time()*1000))+'L}',
          'tp @a 8.5 65 11 180 0']:
 command(c)
act('aim-entity',entity='prominent:serkonid',ticks=8);act('use-entity-ray',entity='prominent:serkonid',ticks=30)
picture('prominent-cinder-shop','CinderTradeScreen')
print('PASS original forge upgrade, armor selection and Cinder shop entries/pictures',flush=True)
