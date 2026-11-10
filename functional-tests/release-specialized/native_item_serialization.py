"""Use actual native persistent item data in legacy full-state observations.

Minecraft 1.20.1's ItemStack.CODEC encodes Count as an int, while save(CompoundTag)
uses the persistent byte Count and includes loader capability data. Keep the
modern registry-aware codec, and make the legacy observers match native saves.
"""
import re

CODEC_BODY=re.compile(
    r'var ops=net\.minecraft\.resources\.RegistryOps\.create\(net\.minecraft\.nbt\.NbtOps\.INSTANCE,player\(\)\.registryAccess\(\)\);\s*'
    r'return ItemStack\.CODEC\.encodeStart\(ops,(value|stack)\)\.result\(\)\.orElseThrow\(\(\)->new IllegalStateException\("[^"]+"\)\)\.toString\(\);')


def storage_item_saves(helpers,mc):
    if mc!='1.20.1':return helpers
    result,count=CODEC_BODY.subn(lambda m:'return '+m[1]+'.save(new net.minecraft.nbt.CompoundTag()).toString();',helpers)
    assert count==1,'Expected exactly one reviewed legacy item serializer'
    return result
