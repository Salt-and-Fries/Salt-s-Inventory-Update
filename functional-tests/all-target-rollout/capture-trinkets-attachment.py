"""Picture Trinkets through the real armor-slot hover with its native Elytra consumer."""
from pathlib import Path
import argparse,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',required=True);a=p.parse_args()
def act(kind,**kw):return send(a.run,kind,**kw)['client']
act('close',ticks=3);act('command',command='gamerule sendCommandFeedback false',ticks=2);act('command',command='gamemode survival @a',ticks=3)
act('command',command='effect clear @a',ticks=3);s=act('inventory',ticks=10)
if a.side=='native':
 b=s['bounds'];slot=next(v for v in s['slots'] if v['index']==6);x=b['x']+slot['x']+8;y=b['y']+slot['y']+8
else:
 if not any(w['kind']=='CHARACTER' for w in s['workspaceWindows']):s=act('key',key=67,ticks=10)
 w=next(w for w in s['workspaceWindows'] if w['kind']=='CHARACTER')
 if w['locked']=='true':
  v=w['controls']['LOCK'];act('click',**v,ticks=1);s=act('release',**v,ticks=4)
 b=next(w['bounds'] for w in s['workspaceWindows'] if w['kind']=='CHARACTER')
 sx=b['x']+60;sy=b['y']+8;tx=260;ty=328
 act('click',x=sx,y=sy,ticks=1);act('drag',x=tx,y=ty,dx=tx-sx,dy=ty-sy,ticks=3);s=act('release',x=tx,y=ty,ticks=5)
 b=next(w['bounds'] for w in s['workspaceWindows'] if w['kind']=='CHARACTER');x=b['x']+16;y=b['y']+16+26+8
act('move',x=x,y=y,ticks=20)
consumer='Botania and the installed original equipment consumers' if a.minecraft=='1.20.1' else 'the unchanged Elytra Slot consumer'
capture(a.run,a.side,a.minecraft,'fabric','trinkets-equipment','Original chest-armor hover exposes the actual Trinkets equipment slots supplied by '+consumer+'. SIU retains this native hover attachment on Character; it is not another container session. Pictured state only, no exhaustive equipment or gameplay claim.')
