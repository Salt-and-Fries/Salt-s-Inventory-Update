"""Enter publisher gear submenus through the actual retained LibZ tab."""
from pathlib import Path
import hashlib

SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()

def cases(mc,loader,installed,items,menus,origin,gaps,hashes,entities=None):
    result=[]
    definitions=[('smitherz-smither','smitherz','minecraft:smithing_table','smitherz:smither'),
                 ('smitherz-grinder','smitherz','minecraft:grindstone','smitherz:grinder'),
                 ('tieredz-reforge','tiered','minecraft:anvil','tiered:reforge'),
                 ('tierify-reforge','tiered','minecraft:anvil','tiered:reforge')]
    for ui,mod,block,menu in definitions:
        if menu not in menus:continue
        gap=next((v for v in gaps if (v['minecraft'],v['loader'],v['ui'])==(mc,loader,ui)
                  and hashes.intersection(v['publisher_sha256'])),None)
        if gap is None:continue
        result.append(dict(id=ui,mod_id=mod,source='reviewed-native-gear-tab-entry',
            publisher_sha256=gap['publisher_sha256'],expected_native_screen=gap['screen'],
            entry_fixture_helper_sha256=SOURCE_SHA256,
            actions=[dict(type='fixture',block=block,ticks=8),dict(type='client-use-block',ticks=16),
                # Source coordinates select the native second tab, transformed
                # through its live public extraction region after movement.
                dict(type='regression-template-region-tap',parent_menu={
                    'minecraft:smithing_table':'minecraft:smithing','minecraft:grindstone':'minecraft:grindstone',
                    'minecraft:anvil':'minecraft:anvil'}[block],region='tabs',source_x=37,source_y=-12,ticks=18)]))
    return result
