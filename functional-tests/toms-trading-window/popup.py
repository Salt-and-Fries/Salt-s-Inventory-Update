"""Follow-up checks after moving the original popup to the window overlay."""
import sys,json
from focused import Check

c=Check(sys.argv[1]);c.setup();c.capture('vending-config-final')
c.click(12,39,button=1);c.capture('vending-popup-final')
c.click(16,43);c.key(259);c.call('text',text='4',ticks=3);c.key(257)
s=c.snap('final-popup-count');c.check('final-popup-edits-count',c.count(c.menu(s)['slots'][0]['item'])==4)
c.click(16,53);s=c.snap('final-popup-nbt');c.check('final-popup-toggles-NBT','matchNBT:254' in s['fixture']);c.key(256)
c.click(12,39,button=1);c.click(160,105);c.click(12,39,button=1)
c.click(16,43);c.key(259);c.call('text',text='2',ticks=3);c.key(257)
s=c.snap('popup-dismiss-and-reopen');c.check('outside-click-dismisses-and-popup-reopens',c.count(c.menu(s)['slots'][0]['item'])==2);c.key(256)
c.click(12,39,button=1);c.click(16,63);c.click(16,43)
s=c.snap('native-tag-filter');c.check('original-tag-filter-conversion-works','toms_trading_network:tag_filter' in c.menu(s)['slots'][0]['item'])
c.setup();c.capture('vending-config-final');c.click(141,13);c.check('final-native-Trading-View-action',c.window('trading')['bounds']['height']==97)
c.capture('vending-trading-final')
