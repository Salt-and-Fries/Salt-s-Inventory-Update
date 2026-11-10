"""Enter the customer view through Tom's real Trading View button."""
from pathlib import Path
import argparse,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
def act(kind,**kw):return send(a.run,kind,**kw)
act('close',ticks=3);act('fixture',block='toms_trading_network:vending_machine',ticks=16)
entry='client-use-block' if a.side=='siu' else 'open'
s=act(entry,ticks=20)['client']
if a.side=='siu' and not s.get('windows',s.get('allWindows',[])):s=act(entry,ticks=20)['client']
if a.side=='siu':
 w=next(w for w in s.get('windows',s.get('allWindows',[])) if w.get('nativeScreen','').endswith('.VendingMachineConfigScreen'))
 widget=next(v for v in w['nativeWidgets'] if v.get('message')=='Trading View')
 native=w['nativeBounds'];region=next(r for r in w['template']['regions'] if r['id']=='original-vending-panel')
 source=region['source'];b=w['bounds']
 x=b['x']+8+region['x']+widget['x']+widget['width']/2-native['x']-source['x']
 y=b['y']+24+region['y']+widget['y']+widget['height']/2-native['y']-source['y']
 s=act('xy-click',x=x,y=y,ticks=24)['client']
 assert any(w.get('nativeScreen','').endswith('.VendingMachineTradingScreen') for w in s.get('windows',s.get('allWindows',[]))),'Native customer transition failed'
else:
 widget=next(v for v in s['widgets'] if v.get('message')=='Trading View')
 x=widget['x']+widget['width']/2;y=widget['y']+widget['height']/2
 act('click',x=x,y=y,ticks=1);s=act('release',x=x,y=y,ticks=24)['client']
 assert s.get('screen','').endswith('.VendingMachineTradingScreen'),'Native customer transition failed'
capture(a.run,a.side,a.minecraft,a.loader,'toms-customer','The customer view is reached through the original Trading View button; pictured layout and this native transition only.')
print('PASS native customer-view transition',a.side,flush=True)
