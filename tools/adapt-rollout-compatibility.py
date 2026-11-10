"""Resolve the first source-level API differences in optional window ports."""
from pathlib import Path
import importlib.util,json,re
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'build/all-target-rollout'
spec=importlib.util.spec_from_file_location('ports',ROOT/'tools/port-rollout-compatibility.py');ports=importlib.util.module_from_spec(spec);spec.loader.exec_module(ports)
spec=importlib.util.spec_from_file_location('core',ROOT/'tools/resolve-rollout-core.py');core=importlib.util.module_from_spec(spec);spec.loader.exec_module(core)
FAMILY={'accessories':'accessories','aircraft':'immersive_aircraft','alexsmobs':'alexsmobs','bountiful':'bountiful','bumblezone':'the_bumblezone','cataclysm':'cataclysm','create':'create','graves':'universal-graves','minecells':'minecells','prominent':'prominentbase','tomstrading':'toms_trading_network','tradingpost':'tradingpost','travelersbackpack':'travelersbackpack'}

def main():
    availability=json.loads((OUT/'availability.json').read_text());manifest=json.loads((OUT/'compatibility-port-candidates.json').read_text())
    for version in ports.VERSIONS:
        root=ROOT/f'compatibility/versions/{version}/fabric/src/main/java/com/salts_inventory_update/compat'
        active={folder for folder,mod in FAMILY.items() if any(any(t.startswith(version+'/') for t in project['targets']) for project in availability['projects'].values() if mod in project['mod_ids'])}
        removed=[]
        for row in manifest:
            target=ROOT/row['target']
            if f'compatibility/versions/{version}/' not in target.as_posix() or not target.exists():continue
            rel=target.relative_to(root)
            unavailable=rel.parts[0] in FAMILY and rel.parts[0] not in active
            text=target.read_text()
            if rel.parts[:2]==('nativewindows','mixin'):
                unavailable=any('compat.'+folder+'.' in text for folder in FAMILY if folder not in active)
            if unavailable:
                backup=OUT/'compatibility-baseline'/target.relative_to(ROOT)
                assert backup.exists() and not backup.read_bytes(), 'Never remove pre-existing source'
                assert target.resolve().is_relative_to(root.resolve())
                target.unlink();removed.append(target.stem);row['status']='no-exact-upstream-release';continue
            text=text.replace('ItemStack.isSameItemSameTags','ItemStack.isSameItemSameComponents')
            text=text.replace('Consumer<FriendlyByteBuf> writer','Consumer<net.minecraft.network.RegistryFriendlyByteBuf> writer')
            text=text.replace('Consumer<FriendlyByteBuf> openingData','Consumer<net.minecraft.network.RegistryFriendlyByteBuf> openingData')
            text=text.replace('var tag=source.createTag();uses=source.getUses();specialPriceDiff=tag.getInt("specialPrice");demand=tag.getInt("demand");','uses=source.getUses();specialPriceDiff=source.getSpecialPriceDiff();demand=source.getDemand();')
            text=text.replace('c.state().name.tick();','').replace('slot.getItem().hasCustomHoverName()','slot.getItem().has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)')
            text=text.replace('c.minecraft().getFrameTime()','c.minecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false)' if version!='1.21.1' else 'c.minecraft().getTimer().getGameTimeDeltaPartialTick(false)')
            target.write_text(text)
        bootstrap=root/'CompatibilityBootstrap.java';s=bootstrap.read_text()
        s='\n'.join(l for l in s.splitlines() if not any('compat.'+f+'.' in l for f in FAMILY if f not in active))+'\n';bootstrap.write_text(s)
        mixin=ROOT/f'compatibility/versions/{version}/common/src/main/resources/salts_inventory_compat.reviewed-native.mixins.json';x=json.loads(mixin.read_text())
        for k in ['mixins','client']:x[k]=[n for n in x[k] if n not in removed]
        mixin.write_text(json.dumps(x,indent=2)+'\n')
        p=root/'nativewindows/NativePayloads.java';p.write_text(ports.convert((ROOT/'tools/rollout-templates/NativePayloads.java').read_text(),version))
        # Restore target-owned machine layouts lost when staging the new helpers.
        p=root/'nativewindows/NativeMachineLayouts.java';backup=OUT/'compatibility-baseline'/p.relative_to(ROOT)
        if backup.exists() and backup.read_bytes():
            original=backup.read_text();text=p.read_text()
            for m in re.finditer(r'    public static DesktopNativeLayout (\w+)\(',original):
                if 'DesktopNativeLayout '+m[1]+'(' in text:continue
                start=m.start();a=original.index('{',start);end=a+1;depth=1
                while depth:depth+=(original[end]=='{')-(original[end]=='}');end+=1
                text=text.rstrip()[:-1]+original[start:end]+'\n}\n'
            p.write_text(text)
        print(version,'active new families:',','.join(sorted(active)),'omitted files',len(removed))
    (OUT/'compatibility-port-candidates.json').write_text(json.dumps(manifest,indent=2))

if __name__=='__main__':main()
