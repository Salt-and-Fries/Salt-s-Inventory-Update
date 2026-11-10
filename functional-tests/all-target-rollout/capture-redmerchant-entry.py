"""Normal close-range native merchant interaction; no fabricated provider."""
from pathlib import Path
import argparse,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
def act(kind,**kw):return send(a.run,kind,**kw)['client']
act('close',ticks=3);act('command',command='fill 5 64 5 12 64 14 minecraft:stone',ticks=3)
act('fixture',block='minecraft:air',ticks=5);act('sneak',down=False,ticks=3);act('held',item='minecraft:air',ticks=3)
for command in ['gamemode creative @a','difficulty normal','kill @e[type=!minecraft:player]',
 'summon supplementaries:red_merchant 8.5 65 8.5 {NoAI:1b,PersistenceRequired:1b}',
 'tp @a 8.5 65 10.5 180 10']:
 act('command',command=command,ticks=6)
act('use-entity',entity='supplementaries:red_merchant',shift=False,ticks=20)
capture(a.run,a.side,a.minecraft,a.loader,'own-supp-red-merchant','Normal empty-hand client interaction with the original merchant within two blocks. Original entity, menu, offers and opening packet are retained; no menu provider or screen is fabricated. Pictured offers only, not a purchase test.')
