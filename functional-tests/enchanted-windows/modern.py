"""Focused 1.21.1 Enchanted 4.x checks on Fabric or NeoForge."""
import sys
from focused import Check


class ModernCheck(Check):
    def window(self, menu):
        w=next(w for w in self.state.get('allWindows',[]) if w.get('menuId')=='enchanted:'+menu)
        assert w.get('templateClass',w.get('template'))=='com.salts_inventory_update.compat.enchanted.EnchantedWindows$Template',w
        return w

    def altar(self):
        self.call('close');self.cmd('fill 8 65 7 10 65 8 enchanted:altar')
        self.call('fixture',block='enchanted:altar',ticks=24)
        self.cmd('tp @a 8.5 65 12.5 180 18');self.call('open-ray',ticks=12)
        self.passed('altar-slot-free-native-status-window',self.window('altar')['slots']==[])
        # Enchanted 4.x stores power/capacity in its native powerData format.
        self.cmd('data merge block 8 65 7 {power:1234.0d,powerData:{capacity:5000,upgrades:{},blocks:{}}}',ticks=16)
        values=self.window('altar')['nativeValues']
        self.passed('altar-live-power-data-sync',values['getMaxPower']==5000 and values['getCurrentPower']>=1234 and values['getRechargeMultiplier']>=1)
        self.capture('enchanted-altar-powered');self.drag('altar');self.close_reopen('altar')
        self.passed('altar-reopened-power-readout',self.window('altar')['nativeValues']['getMaxPower']==5000)
        self.capture('enchanted-altar-final');self.call('close')
        self.cmd('clone 8 65 7 10 65 8 3 65 7 replace move',ticks=25)
        self.cmd('data merge block 3 65 7 {power:5000.0d,powerData:{capacity:5000,upgrades:{},blocks:{}}}',ticks=4)


if __name__=='__main__':
    c=ModernCheck(sys.argv[1])
    for group in sys.argv[2:] or ['oven','shelf','altar','wheel','distillery']:
        print('GROUP',group,flush=True);getattr(c,group)()
    print('Focused checks:',len(c.results['checks']),flush=True)
