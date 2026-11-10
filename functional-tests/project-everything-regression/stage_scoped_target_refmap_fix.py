"""Stage the Forge SRG mapping required by the scoped use-item redirect once."""
import copy
import json
import zipfile
from stage_runtime_fixes import main, ROOT, OUT, SHA

baseline = OUT / 'release-before-scoped-target-refmap.json'
report = 'staged-scoped-target-refmap-fix.json'
assert not baseline.exists() and not (OUT / report).exists(), 'Preserve prior staging evidence'
filename = 'salts_inventory_update-1.20.1-Forge-0.1.6.jar'
entry = 'salts_inventory_update.refmap.json'
mixin = 'com/salts_inventory_update/mixin/server/ServerGamePacketListenerImplMixin'
target = ('Lnet/minecraft/server/level/ServerPlayerGameMode;useItemOn('
          'Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;'
          'Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;'
          'Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;')
with zipfile.ZipFile(ROOT / 'dist/core' / filename) as archive:
    old = json.loads(archive.read(entry))
with zipfile.ZipFile(ROOT / 'versions/1.20.1/forge/build/libs' / filename) as archive:
    new = json.loads(archive.read(entry))
# Prove the complete built refmap differs only by the two required mappings.
expected = copy.deepcopy(old)
for mapping in [expected['mappings'], expected['data']['searge']]:
    assert target not in mapping[mixin]
    mapping[mixin][target] = target.replace(';useItemOn(', ';m_7179_(')
assert new == expected, 'Unrelated refmap changes require separate review'
baseline.write_text(json.dumps({p.relative_to(ROOT).as_posix(): SHA(p.read_bytes())
    for family in ['core', 'compatibility'] for p in (ROOT / 'dist' / family).glob('*.jar')}, indent=2))
main([('core', '1.20.1', 'Forge', entry)], baseline.name, report,
     'release-before-scoped-target-refmap')
