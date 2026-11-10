"""Stage the optional legacy GeckoLib identity correction once; preserve API bytes."""
import json
from stage_runtime_fixes import main,ROOT,OUT,SHA
baseline=OUT/'release-before-geckolib-absent-ids.json'
report='staged-geckolib-absent-ids-fix.json'
assert not baseline.exists() and not (OUT/report).exists(),'Preserve previous staging evidence'
baseline.write_text(json.dumps({p.relative_to(ROOT).as_posix():SHA(p.read_bytes())
    for family in ['core','compatibility'] for p in (ROOT/'dist'/family).glob('*.jar')},indent=2)+'\n')
added=['com/salts_inventory_update/compat/geckolib/GeckoLibStackSynchronization.class',
       'com/salts_inventory_update/compat/nativewindows/mixin/GeckoLibStackSyncMixin.class']
entries=[*added,'com/salts_inventory_update/compat/nativewindows/mixin/NativeWindowsMixinPlugin.class',
         'salts_inventory_compat.reviewed-native.mixins.json']
changes=[('compatibility','1.20.1',loader,entry) for loader in ['Fabric','Forge'] for entry in entries]
main(changes,baseline.name,report,'release-before-geckolib-absent-ids',additions=added)
