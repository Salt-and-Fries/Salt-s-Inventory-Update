from capture import Audit,ROOT
import json
a=Audit()
a.part('precise_export_bus')
a.call('held',item='minecraft:gold_ingot',count=16,ticks=4)
a.call('wait',ticks=5)
player=next(s for s in a.state['slots']if s['player']and s['item']=='minecraft:gold_ingot')
target=next(s for s in a.state['slots']if not s['player']and 0<=s['x']<170 and 0<s['y']<150)
a.call('slot',slot=player['index']);a.call('slot',slot=target['index']);a.call('slot',slot=player['index'])
b=a.state['bounds'];x=b['x']+target['x']+8;y=b['y']+target['y']+8
a.call('move',x=x,y=y);a.call('click',x=x,y=y,button=2,ticks=2);a.call('release',x=x,y=y,button=2,ticks=6)
if a.state['screen'].endswith('SetAmount'):a.capture('forge-set-amount','Middle-click a populated precise bus filter to edit its target amount.')
else:print('Set Amount not opened',a.state['screen'],flush=True)
a.reset('expatternprovider:assembler_matrix_frame')
for cmd in ['fill 8 65 8 11 67 10 expatternprovider:assembler_matrix_frame','fill 9 65 9 10 65 9 expatternprovider:assembler_matrix_wall','fill 9 67 9 10 67 9 expatternprovider:assembler_matrix_wall','fill 9 66 8 10 66 8 expatternprovider:assembler_matrix_wall','fill 9 66 10 10 66 10 expatternprovider:assembler_matrix_wall','setblock 8 66 9 expatternprovider:assembler_matrix_wall','setblock 11 66 9 expatternprovider:assembler_matrix_wall','setblock 9 66 9 expatternprovider:assembler_matrix_pattern','setblock 10 66 9 expatternprovider:assembler_matrix_crafter','setblock 8 64 8 ae2:creative_energy_cell']:a.cmd(cmd)
a.call('wait',ticks=80)
p=json.loads((ROOT/'build/remaining-ui-audit/fixtures/own-pattern-item-fixtures.json').read_text())[0]
a.cmd('data merge block 9 66 9 {pattern:[{Slot:0,id:"'+p['item']+'",Count:1b,tag:'+p['nbt']+'}]}',ticks=20)
a.call('open',ticks=20);a.capture('forge-assembler-matrix-populated','Form and power an Assembler Matrix with pattern and crafting cells; insert an actual encoded crafting pattern.','Pattern fixture was loaded into the original world block inventory; original matrix/menu renderer used.')
a.reset('expatternprovider:ex_inscriber');a.call('open',ticks=12)
for w in a.state['widgets']:
 if 'Next' in w.get('message',''):
  a.tap(w['x']+8,w['y']+8);break
a.capture('forge-inscriber-second-job','Use the Extended Inscriber and its native next-job control.')
