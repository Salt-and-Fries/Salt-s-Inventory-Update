from capture import Audit,ROOT
import json,sys
a=Audit('forge','extended-ae-optional-02'if '--optional'in sys.argv else'extended-ae-design-01')
if '--optional'in sys.argv:
 actions=next(r for r in json.loads((ROOT/'build/remaining-ui-audit/fixtures/pr-wireless-additional.json').read_text())['cases']if r['id']=='pr-wireless-ex-pattern')['native_actions']
 a.call('close')
 for step in actions:
  step=dict(step);kind=step.pop('type')
  if kind=='held':step['item']='expatternprovider:wireless_ex_pat'
  a.call(kind,**step)
 a.capture('forge-universal-wireless-ex-pat','Link and charge an Extended Wireless Pattern Access Terminal with AE2WTLib installed; use the held terminal.','Original AE2WTLib conditional screen, not a fabricated menu.')
else:
 # The native name editor is opened by Extended AE's knife integration.
 a.reset('ae2:interface');a.call('held',item='ae2:certus_quartz_cutting_knife');a.call('use-block-ray',ticks=10)
 a.capture('forge-renamer','Use a Certus Quartz Cutting Knife on an AE2 Interface.')
 a.reset();a.call('held',item='expatternprovider:pattern_modifier');a.call('use-item',ticks=10)
 for mode in ['multiply','replace','clone']:
  a.capture('forge-pattern-modifier-'+mode,'Use the Pattern Modifier item; select '+mode+' with its left toolbar mode button.')
  w=next(w for w in a.state['widgets']if 'Change'in w['message']or 'change'in w['message']);a.tap(w['x']+8,w['y']+8)
 a.part('ex_crafting_terminal')
 for mode in ['Crafting','Stonecutter','Smithing','Anvil']:
  w=next(w for w in a.state['widgets']if w['message']==mode);a.tap(w['x']+10,w['y']+10)
  a.capture('forge-ex-crafting-terminal-'+mode.lower(),'Use the powered Extended Crafting Terminal; select '+mode+'.')
