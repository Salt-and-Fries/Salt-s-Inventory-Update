"""Focused checks for the Character subview, shared equipment and independent Backpack."""
from pathlib import Path
import argparse
from focused import Check
from attachments import move


class CharacterCheck(Check):
    def char(self):return self.workspace('CHARACTER')
    def cloud(self):
        w=self.char();c=w['content'];active=w.get('aetherMode',False)
        self.tap(dict(x=c['x']+(15 if active else 33),y=c['y']+(72 if active else c['height']-8)))
        self.call('wait',ticks=5)
    def player(self,id,button=0):
        c=self.char()['content'];active=self.char().get('aetherMode',False)
        if id in [5,6,7,8]:x,y=(59 if active else 8),8+(id-5)*18
        elif id in [1,2,3,4]:x,y=(116 if active else 98)+((id-1)%2)*18,18+((id-1)//2)*18
        elif id==0:x,y=154,28
        elif id==45:x,y=(116 if active else 77),62
        else:raise ValueError(id)
        self.tap(dict(x=c['x']+x+8,y=c['y']+y+8),button)
    def gear(self,index):
        w=self.char();c=w['content'];cell=w['aetherCells'][index];self.tap(dict(x=c['x']+85+cell['x'],y=c['y']+16+cell['y']))
    def general(self):
        self.tap(self.general_point());self.call('wait',ticks=5)
    def general_point(self):
        w=self.char()
        # Observe the public rendered region: Curios occupies the portrait's
        # left corner, Accessories the right. Both retain native pointer input.
        choices=w['builtinControls']
        control=next((a for a in choices if a['type'].endswith('.CuriosCharacterButton')),None)
        if control is None:control=next(a for a in choices if a['type'].endswith('.AccessoriesClient$Button'))
        r=control['regions'][0];b=w['bounds']
        return dict(x=b['x']+r['x']+r['width']/2,y=b['y']+r['y']+r['height']/2)
    def no_separate_window(self):return not any(w.get('menuId')=='salts_inventory_compat:aether_equipment'for w in self.state.get('allWindows',[]))
    def main(self,loader):
        self.call('close');self.call('fixture',block='minecraft:air');self.cmd('gamemode survival @a');self.cmd('tp @a 8.5 65 12.5 180 18')
        for index,item,count in [(0,'minecraft:oak_planks',4),(1,'aether:red_cape',1),(2,'minecraft:stone',1),(3,'minecraft:iron_helmet',1),(4,'minecraft:shield',1)]:self.cmd(f'item replace entity @a inventory.{index} with {item} {count}')
        self.call('inventory',ticks=8);self.call('aether-character',ticks=8);before=self.char().copy();inv=self.workspace('INVENTORY').copy();window_count=len(self.state['workspaceWindows']);self.capture('character-normal')
        self.cloud();w=self.char();self.passed('cloud-reformats-character',w.get('aetherMode')is True)
        self.passed('same-character-identity-and-bounds',w['instance']==before['instance']and w['bounds']==before['bounds'])
        self.passed('no-new-equipment-window',self.no_separate_window()and len(self.state['workspaceWindows'])==window_count)
        self.passed('eight-native-equipment-slots',len(w['aetherCells'])==len(w['aetherSlots'])==8)
        self.passed('inventory-unaffected-by-cloud',self.workspace('INVENTORY')['instance']==inv['instance']and self.workspace('INVENTORY')['bounds']==inv['bounds']);self.capture('character-aether-empty')
        i=next(i for i,c in enumerate(w['aetherCells'])if c['identifier'].split(':')[-1].removeprefix('aether_').removesuffix('_slot')=='cape');self.inventory(10);self.gear(i);d,a=self.snap('character-cape')
        self.passed('cape-equipped-through-character',any(e['name'].split(':')[-1].removeprefix('aether_').removesuffix('_slot')=='cape'and'aether:red_cape'in e['item']for e in a['equipment']))
        self.passed('hidden-session-snapshot-reaches-character',self.char()['aetherSlots'][i]['item']=='aether:red_cape')
        self.cape_roundtrip(i)
        cell=w['aetherCells'][i];c=self.char()['content'];p=dict(x=c['x']+90+cell['x'],y=c['y']+7+cell['y']);old=next(e['render']for e in a['equipment']if e['name'].split(':')[-1].removeprefix('aether_').removesuffix('_slot')=='cape');self.tap(p);d,a=self.snap('character-visibility');self.passed('visibility-toggle-works-in-character',next(e['render']for e in a['equipment']if e['name'].split(':')[-1].removeprefix('aether_').removesuffix('_slot')=='cape')!=old);self.tap(p)
        self.inventory(11);self.gear(i);d,a=self.snap('character-invalid');self.passed('native-item-restriction-preserved',not any('minecraft:stone'in e['item']for e in a['equipment']));self.inventory(11)
        self.inventory(12);self.player(5);self.inventory(13);self.player(45);d,a=self.snap('character-armor-offhand')
        self.passed('relocated-armor-slot-works','minecraft:iron_helmet'in d['inventory'][39]['item']);self.passed('relocated-offhand-slot-works','minecraft:shield'in d['inventory'][40]['item'])
        self.inventory(9);self.player(1,1);self.player(2,1);self.cloud();d,a=self.snap('character-crafting-normal');m=next(m for m in d['menus']if m['id']==0)
        self.passed('cloud-restores-normal-character',self.char().get('aetherMode')is False and self.no_separate_window())
        self.passed('mode-switch-preserves-crafting-and-cursor',all('minecraft:oak_planks'in m['slots'][j]['item']for j in [1,2])and any(count in m['carried']for count in ['Count:2b','count:2']))
        self.cloud();self.player(3,1);self.player(4,1);self.player(0);self.inventory(14);d,a=self.snap('character-crafting-result');self.passed('native-two-by-two-crafting-works','minecraft:crafting_table'in d['inventory'][14]['item']);self.capture('character-equipped')
        c=self.char()['content'];self.tap(dict(x=c['x']+144,y=c['y']+70));self.capture('character-recipes-open');self.passed('relocated-recipe-book-opens',self.char()['recipeBookVisible']is True)
        self.tap(dict(x=c['x']+144,y=c['y']+70));self.capture('character-recipes-closed');self.passed('relocated-recipe-book-closes',self.char()['recipeBookVisible']is False)
        b=self.char()['bounds'];self.tap(dict(x=b['x']-12 if b['x']>=22 else b['x']+12,y=self.char()['content']['y']+12 if b['x']>=22 else b['y']-12));self.passed('crown-opens-native-moa-screen',self.state['screen'].endswith('MoaSkinsScreen'));self.call('key',key=256,ticks=8);self.passed('native-moa-return-keeps-character-mode',self.char().get('aetherMode')is True)
        self.general();path='salts_inventory_compat:'+('curios_accessories'if loader=='forge'else'accessories_equipment');self.passed('general-button-restores-normal-character',self.char().get('aetherMode')is False);self.passed('general-panel-opens-exclusively',self.window(path)is not None and self.no_separate_window());d,a=self.snap('character-general')
        self.passed('equipment-session-closed-before-general',not any(m['class'].endswith('.AetherEquipmentMenu')for m in d['menus']))
        if loader=='fabric':
            slots=self.window(path)['slots'];self.passed('accessories-retains-equipment-and-cosmetics',len(slots)>=16 and len(slots)%2==0)
            self.passed('general-view-sees-same-equipped-cape',any(s['item']=='aether:red_cape'for s in slots))
        self.capture('character-general-panel');self.cloud();self.passed('cloud-closes-general-panel',not any(w.get('menuId')==path for w in self.state['allWindows'])and self.char().get('aetherMode')is True)
        self.passed('switching-views-keeps-equipped-items',self.char()['aetherSlots'][i]['item']=='aether:red_cape')
        for _ in range(6):self.cloud()
        d,a=self.snap('character-toggle-cycle');self.passed('repeated-toggle-has-one-backing-session',self.char().get('aetherMode')is True and sum(m['class'].endswith('.AetherEquipmentMenu')for m in d['menus'])==1 and self.no_separate_window())
        char=self.char()['bounds'].copy();inv=self.workspace('INVENTORY')['bounds'].copy();move(self,'CHARACTER',char['x']+100,char['y']-30);self.passed('character-movement-retains-mode',self.char().get('aetherMode')is True and self.char()['bounds']!=char);self.passed('character-movement-keeps-inventory-independent',self.workspace('INVENTORY')['bounds']==inv)
        self.tap(self.char()['controls']['CLOSE']);self.call('wait',ticks=12);d,a=self.snap('character-closed');self.passed('character-close-disposes-equipment-session',not any(m['class'].endswith('.AetherEquipmentMenu')for m in d['menus']));self.passed('character-close-keeps-inventory',self.workspace('INVENTORY')is not None)
        self.call('aether-character',ticks=10);self.passed('character-reopens-in-normal-mode',self.char().get('aetherMode')is False)
        if loader=='forge':self.backpack_mode()
        else:self.cloud();self.capture('character-final')
        self.alternating_launchers(loader)
    def cape_roundtrip(self,i):
        self.gear(i);self.inventory(10);d,a=self.snap('character-cape-removed')
        self.passed('cape-can-be-removed-through-character','aether:red_cape'in d['inventory'][10]['item']and not any(e['name'].split(':')[-1].removeprefix('aether_').removesuffix('_slot')=='cape'and'aether:red_cape'in e['item']for e in a['equipment']))
        self.inventory(10);self.gear(i);d,a=self.snap('character-cape-reequipped')
        self.passed('cape-can-be-reequipped-without-duplication','aether:red_cape'not in d['inventory'][10]['item']and any(e['name'].split(':')[-1].removeprefix('aether_').removesuffix('_slot')=='cape'and'aether:red_cape'in e['item']for e in a['equipment']))
    def controller_targets(self):
        if not self.char().get('aetherMode'):self.cloud()
        w=self.char();targets=w['controllerTargets'];c=w['content']
        gear=[t for cell in w['aetherCells'] for t in targets
              if t['width']==16 and t['height']==16 and t['x']==c['x']+77+cell['x'] and t['y']==c['y']+8+cell['y']]
        identities=[(t['group'],t['row'],t['column']) for t in targets]
        toggles=[(c['x']+88+cell['x'],c['y']+5+cell['y']) for cell in w['aetherCells'] if cell['toggle']]
        results=dict(gear_count=len(gear),native_gear_count=len(w['aetherCells']),
            duplicate_navigation_cells=len(identities)-len(set(identities)),
            visibility_buttons=len(toggles),visibility_targets=sum(any(t['x']==x and t['y']==y and t['width']==5 and t['height']==5 for t in targets) for x,y in toggles),
            targets=targets)
        import json
        (self.folder/'aether-controller-targets.json').write_text(json.dumps(results,indent=2)+'\n')
        self.passed('controller-exposes-every-native-equipment-cell',len(gear)==len(w['aetherCells']))
        self.passed('controller-navigation-identities-are-unique',results['duplicate_navigation_cells']==0)
        self.passed('controller-exposes-every-native-visibility-control',results['visibility_targets']==len(toggles))
    def alternating_launchers(self,loader):
        path='salts_inventory_compat:'+('curios_accessories'if loader=='forge'else'accessories_equipment')
        self.call('wait',ticks=1)
        for _ in range(8):
            w=self.char();c=w['content'];active=w.get('aetherMode',False)
            point=self.general_point()
            self.call('click',**point,button=0,ticks=1)
            self.call('release',**point,button=0,ticks=1)
            # Cloud's position depends on the current presentation, including
            # the normal layout while an equipment response is still pending.
            w=self.char();c=w['content'];active=w.get('aetherMode',False)
            self.call('click',x=c['x']+(15 if active else 33),y=c['y']+(72 if active else c['height']-8),button=0,ticks=1)
            self.call('release',x=c['x']+(15 if active else 33),y=c['y']+(72 if active else c['height']-8),button=0,ticks=1)
            self.call('wait',ticks=8)
            assert not(self.char().get('aetherMode')and any(w.get('menuId')==path for w in self.state['allWindows']))
        if not self.char().get('aetherMode'):self.cloud()
        d,a=self.snap('character-alternating-launchers')
        self.passed('alternating-launchers-remain-exclusive',self.char().get('aetherMode')is True and not any(w.get('menuId')==path for w in self.state['allWindows'])and sum(m['class'].endswith('.AetherEquipmentMenu')for m in d['menus'])==1 and self.no_separate_window())
    def backpack_mode(self):
        self.cmd('item replace entity @a armor.chest with quark:backpack');self.call('wait',ticks=25);self.cloud();bag=self.window('salts_inventory_compat:quark_backpack');inv=self.workspace('INVENTORY').copy()
        self.passed('backpack-still-has-27-slots',len(bag['slots'])==27);self.passed('backpack-has-no-close-button','CLOSE'not in self.info(bag)['controls']);self.passed('backpack-does-not-create-an-aether-window',self.no_separate_window())
        old=bag['bounds'].copy();move(self,'CHARACTER',180,300);self.passed('character-move-does-not-move-backpack',self.window('salts_inventory_compat:quark_backpack')['bounds']==old)
        move(self,'INVENTORY',440,330);new=self.window('salts_inventory_compat:quark_backpack')['bounds'];now=self.workspace('INVENTORY')['bounds'];self.passed('backpack-still-moves-with-inventory',new['x']-old['x']==now['x']-inv['bounds']['x']and new['y']-old['y']==now['y']-inv['bounds']['y'])
        self.tap(self.workspace('INVENTORY')['controls']['CLOSE']);self.call('wait',ticks=10);self.passed('inventory-close-closes-backpack-only',not any(w.get('menuId')=='salts_inventory_compat:quark_backpack'for w in self.state['allWindows'])and self.char().get('aetherMode')is True)
        self.call('inventory',ticks=20);self.passed('inventory-reopen-restores-backpack',self.window('salts_inventory_compat:quark_backpack')is not None);self.capture('character-backpack-final')


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);p.add_argument('--loader',required=True,choices=['fabric','forge']);p.add_argument('--controller',action='store_true');a=p.parse_args();check=CharacterCheck(a.output);check.main(a.loader)
    if a.controller:check.controller_targets()
