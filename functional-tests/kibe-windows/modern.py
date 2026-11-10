"""Focused Kibe 2.x checks; no complete gameplay suite."""
from pathlib import Path
import json,sys
from focused import Check,ROOT

class Modern(Check):
    def __init__(self,run):super().__init__(run,ROOT/'output/kibe-all-targets'/run.name)
    def cmd(self,command):
        # Native item-entity stacks use lowercase count in 1.21 data components.
        return super().cmd(command.replace('Count:3b','count:3'))
    def window(self,path):
        if '_generator_mk'in path:path='block_generator'
        return super().window(path)
    def xp(self):
        # A repeated fixture of the same block keeps its real block entity.
        # Remove it first so reruns cannot collect leftover native test items.
        self.call('close');self.call('fixture',block='minecraft:air',ticks=5)
        return super().xp()
    def cooler(self):
        self.cmd('difficulty normal');self.open('cooler_item');self.insert('cooler_item',0,'minecraft:bread',8)
        self.call('kibe-food',food=10,ticks=30);d=self.snap('cooler-open-not-feeding')
        self.passed('cooler-pauses-feeding-while-menu-open',d['food']==10 and self.menu(d,'CoolerBlockItemScreenHandler')['slots'][0]['count']==8,d)
        self.call('snapshot-server',ticks=3);locked=json.loads((self.run/'server-state.json').read_text(encoding='utf-8')).get('selectedLocked')
        self.passed('cooler-original-held-slot-protected',locked is True,locked)
        self.call('kibe-window-action',title='Cooler',action='USER_CLOSE',ticks=20);d=self.snap('cooler-closed-feeding')
        contents=next(c['contents']for c in d['foodContents']if c['index']==0)
        self.passed('cooler-resumes-native-feeding-after-close',d['food']>10 and contents==[{'item':'minecraft:bread','count':6}],d)
        self.call('use-item',ticks=10);m=self.menu(self.snap('cooler-reopen-food'),'CoolerBlockItemScreenHandler')
        self.passed('cooler-remaining-food-component-persists',m['slots'][0]['count']==6,m)
        self.call('close');self.call('fixture',block='minecraft:air',ticks=4)
        self.cmd('item replace entity @a weapon.offhand with kibe:cooler');self.call('kibe-offhand-use',ticks=12)
        self.passed('cooler-offhand-open',self.window('cooler_item')['templateClass'].endswith('KibeClient$Template'))
        self.insert('cooler_item',0,'minecraft:carrot',4);self.call('close');d=self.snap('cooler-offhand-components')
        self.passed('cooler-offhand-food-component-preserved',any(c['index']==40 and c['contents']==[{'item':'minecraft:carrot','count':4}]for c in d['foodContents']),d['foodContents'])
    def runes(self):
        self.open('entangled_chest');self.call('kibe-runes',ticks=8);self.insert('entangled_chest',3,'minecraft:diamond',9)
        self.capture('entangled-colored-runes','entangled_chest')
        self.call('held',item='kibe:entangled_bag',ticks=5);self.call('open',shift=True,ticks=8);self.call('use-item',shift=False,ticks=10)
        d=self.snap('entangled-bag-linked');bag=self.menu(d,'EntangledBagScreenHandler')
        self.passed('bag-native-link-copies-colored-component','rune_set'in bag['tag']and all(c in bag['tag'].upper()for c in ['ORANGE','MAGENTA','LIGHT_GRAY']),bag['tag'])
        self.passed('bag-shares-real-chest-inventory',any(s['index']==3 and s['item']=='minecraft:diamond'and s['count']==9 for s in bag['slots']),bag['slots'])
        self.slot('entangled_bag',3);self.inventory(12);d=self.snap('entangled-shared-after-withdraw')
        menus=[m for m in d['menus']if m['class'].endswith(('EntangledBagScreenHandler','EntangledChestScreenHandler'))]
        self.passed('bag-chest-shared-withdraw-sync',len(menus)==2 and all(next(s for s in m['slots']if s['index']==3)['count']==0 for m in menus),menus)
        self.capture('entangled-bag-linked-colored','entangled_bag')
    def native_screen(self):
        self.call('close');self.call('fixture',block='kibe:chunk_loader',ticks=5)
        self.cmd('setblock 8 65 8 kibe:chunk_loader[enabled=true]');self.call('open',ticks=12)
        self.passed('chunk-loader-keeps-original-screen',self.state['screen']=='io.github.lucaargolo.kibe.client.screen.ChunkLoaderScreen',self.state['screen'])
        self.call('capture',capture='kibe-native-chunk-loader',ticks=3)
    def all(self):
        for section in ['geometry','transfers','rejects','quick','cooler','xp','runes','generators','crafting','native_screen','chrome','machines']:
            print('SECTION',section,flush=True);getattr(self,section)()

if __name__=='__main__':
    q=Modern(Path(sys.argv[1]));getattr(q,sys.argv[2]if len(sys.argv)>2 else'all')()
