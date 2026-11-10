"""Focused search-field checks after replacing the contaminated native crop."""
from pathlib import Path
import json,sys
from focused import Check

c=Check(sys.argv[1]);c.folder=c.out/'search-bar-checks';c.folder.mkdir(exist_ok=True)
c.setup()
c.check('window-geometry-retained',c.window()['bounds']['width']==254 and c.window()['bounds']['height']==136)
c.check('initial-unfiltered-offers',c.info()['filteredOfferCount']==10)
c.capture('search-clean-empty')

# The whole new border rectangle focuses the original text box.
c.click(18,10);c.call('text',text='mending',ticks=8)
c.check('top-left-field-edge-focuses-and-filters',c.info()['search']=='mending' and c.info()['filteredOfferCount']==1)
c.capture('search-clean-mending')
c.click(100,25);c.call('key',key=269,ticks=1)
for _ in range(7):c.call('key',key=259,ticks=1)
c.check('bottom-right-field-edge-focuses-and-clears',c.info()['search']=='' and c.info()['filteredOfferCount']==10)

c.search('bread')
c.check('search-still-reaches-other-villager',c.info()['filteredOfferCount']==1 and 'minecraft:bread' in c.info()['filteredResults'][0])
c.call('key',key=268,ticks=1)
for _ in range(4):c.call('key',key=262,ticks=1)
c.call('key',key=259,ticks=3)
c.check('caret-navigation-and-backspace',c.info()['search']=='bred')
c.call('text',text='a',ticks=5)
c.check('insertion-refilters-native-offers',c.info()['search']=='bread' and c.info()['filteredOfferCount']==1)

c.search('abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmn')
c.check('long-search-keeps-native-50-character-limit',len(c.info()['search'])==50 and c.info()['filteredOfferCount']==0)
c.capture('search-clean-long')
before=c.info()['search'];c.inventory(9);c.call('text',text='x',ticks=3)
c.check('inventory-focus-stops-search-typing',c.info()['search']==before)
c.call('gui-scale',scale=3,width=1280,height=900,ticks=12)
c.search('mending')
c.check('search-and-caret-survive-GUI-scale-three',c.info()['search']=='mending' and c.info()['filteredOfferCount']==1)
c.capture('search-clean-scale-three')

c.call('gui-scale',scale=2,width=1280,height=900,ticks=10)
c.search('');c.cmd('item replace entity @a inventory.2 with minecraft:emerald 9');c.cmd('item replace entity @a inventory.3 with minecraft:book 1')
c.click(30,60);s=c.snapshot('clean-search-selected-trade')
c.check('cleared-search-retains-correct-trade-selection',c.count(s,2)==1 and 'minecraft:mending' in c.menu(s)['slots'][2]['item'])
c.call('move',x=639,y=449,ticks=110);c.capture('search-clean-full')
journal=json.loads((c.folder/'journal.json').read_text())
journal['scope']='Search field rendering, edge focus, keyboard editing, filtering, GUI scaling and one resulting offer only; no purchase or full functional suite'
(c.folder/'journal.json').write_text(json.dumps(journal,indent=2))
print('Focused search-bar checks finished.',flush=True)
