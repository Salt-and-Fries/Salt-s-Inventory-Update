"""Natural current Cataclysm/Galosphere/Miners entries, without forced menus."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import capture,send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--only',action='append');p.add_argument('--minecraft',default='1.21.1');p.add_argument('--loader',default='neoforge');a=p.parse_args();rows=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
act('catalog',ticks=1);blocks=set(json.loads((a.run/'catalog.json').read_text())['blocks'])
for ui,block in [('alexs-transmutation-table','alexsmobs:transmutation_table'),('deep-aether-combiner','deep_aether:combiner'),('galosphere-combustion-table','galosphere:combustion_table'),
 ('miners-delight-copper-pot','minersdelight:copper_pot'),('cataclysm-weapon-fusion','cataclysm:mechanical_fusion_anvil')]:
 if a.only and ui not in a.only:continue
 if block=='minersdelight:copper_pot' and block not in blocks and 'miners_delight:copper_pot' in blocks:block='miners_delight:copper_pot'
 if block not in blocks:continue
 act('close',ticks=2);act('sneak',down=False,ticks=2);act('command',command='gamemode creative @a',ticks=2);act('held',item='minecraft:air',ticks=2);act('fixture',block=block,ticks=6);act('client-use-block',ticks=15)
 capture(a.run,a.side,a.minecraft,a.loader,ui,'Original current publisher block, opened by its ordinary right-click interaction. Pictured default state; no forced provider.')
 rows.append({'ui':ui,'status':'ordinary-entry-and-picture-pass'});(a.run/'remaining-extra-captures.json').write_text(json.dumps(rows,indent=2))
if a.only and 'cataclysm-netherite-ministrosity' not in a.only:raise SystemExit(0)
act('close',ticks=2);act('sneak',down=False,ticks=2);act('held',item='minecraft:air',ticks=2);act('fixture',block='minecraft:air',ticks=4)
for c in ['kill @e[type=!minecraft:player]','difficulty normal','summon cataclysm:netherite_ministrosity 8.5 65 8.5 {Tame:1b,NoAI:1b}',
 'data modify entity @e[type=cataclysm:netherite_ministrosity,sort=nearest,limit=1] Owner set from entity @p UUID','tp @a 8.5 65 10.5 180 0']:act('command',command=c,ticks=6)
s=act('use-entity',entity='cataclysm:netherite_ministrosity',shift=False,ticks=20)
capture(a.run,a.side,a.minecraft,a.loader,'cataclysm-netherite-ministrosity','Owned native Netherite Ministrosity, normal empty-hand interaction. Model portrait and native cargo pictured.')
rows.append({'ui':'cataclysm-netherite-ministrosity','status':'ordinary-entry-and-picture-pass'});(a.run/'remaining-extra-captures.json').write_text(json.dumps(rows,indent=2))
