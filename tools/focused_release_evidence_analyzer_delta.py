"""Exact whole-member boundary for the reviewed analyzer addition; no coverage grant."""
import hashlib,json,zipfile
CLASS='com/salts_inventory_update/compat/nativewindows/mixin/BountifulAnalyzerRefreshMixin.class'
CONFIG='salts_inventory_compat.reviewed-native.mixins.json'
def members(path):
    with zipfile.ZipFile(path)as z:
        names=z.namelist();assert len(names)==len(set(names)),'Duplicate archive member'
        return {n:z.read(n)for n in names}
def inspect(before,after,reviewed_class_sha256):
    old,new=members(before),members(after)
    assert CLASS not in old and set(new)==set(old)|{CLASS},'Only one declared mixin class may be added'
    assert hashlib.sha256(new[CLASS]).hexdigest()==reviewed_class_sha256,'New class differs from reviewed compiled body'
    expected=json.loads(old[CONFIG]);assert'BountifulAnalyzerRefreshMixin'not in expected['client']
    index=expected['client'].index('BountifulScreenMixin');expected['client'].insert(index+1,'BountifulAnalyzerRefreshMixin')
    assert json.loads(new[CONFIG])==expected,'Only the exact client registration may change'
    assert {n for n in old if old[n]!=new[n]}=={CONFIG},'Existing compiled code or unrelated resource changed'
    return dict(status='exact-new-analyzer-mixin-and-client-registration-only',new_class_sha256=reviewed_class_sha256,preserved_existing_members=len(old)-1)
