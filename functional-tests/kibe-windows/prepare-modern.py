"""Excluded observation helpers for the two original Kibe 2.3.1 releases."""
from pathlib import Path
import importlib.util,sys

ROOT=Path(__file__).resolve().parents[2]
def module(name,path):
    s=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
legacy=module('kibe_observer',ROOT/'functional-tests/kibe-windows/prepare.py')
prep=module('kibe_modern_runtime',ROOT/'tools/prepare-rollout-production-modern.py')

def patch(text):
    extra=legacy.EXTRA
    extra=extra.replace('.saveWithFullMetadata()', '.saveWithFullMetadata(player().registryAccess())')
    extra=extra.replace('.save(new net.minecraft.nbt.CompoundTag())','.saveOptional(player().registryAccess())')
    extra=extra.replace('coreCall(m,"getTag").toString()','((ItemStack)coreCall(m,"getStack")).getComponents().toString()')
    extra=extra.replace('var colors=(Map<Integer,net.minecraft.world.item.DyeColor>)coreCall(be,"getRuneColors");','var colors=(net.minecraft.world.item.DyeColor[])coreCall(be,"getRuneColors");')
    extra=extra.replace('colors.put(i,net.minecraft.world.item.DyeColor.byId(i))','colors[i-1]=net.minecraft.world.item.DyeColor.byId(i)')
    extra=extra.replace('var fluid=net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(BuiltInRegistries.FLUID.get(id("kibe:liquid_xp")));',
        'var fluid=Class.forName("net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant").getMethod("of",net.minecraft.world.level.material.Fluid.class).invoke(null,BuiltInRegistries.FLUID.get(id("kibe:liquid_xp")));')
    extra=extra.replace('try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){',
        'try(var tx=(AutoCloseable)Class.forName("net.fabricmc.fabric.api.transfer.v1.transaction.Transaction").getMethod("openOuter").invoke(null)){')
    extra=extra.replace('tx.commit();','coreCall(tx,"commit");')
    start=extra.index('case "kibe-fill-xp"');end=extra.index('case "kibe-runes"',start)
    part=extra[start:end].replace('server(()->{','server(()->{try{',1).replace('return true;\n            });','return true;}catch(Exception failure){throw new IllegalStateException(failure);}\n            });',1)
    extra=extra[:start]+part+extra[end:]
    extra=extra.replace('write("kibe-state.json",Map.of(', '''var food=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++){
                    var source=player().getInventory().getItem(i);var contents=source.get(net.minecraft.core.component.DataComponents.CONTAINER);
                    if(contents!=null)food.add(Map.of("index",i,"contents",contents.stream().map(s->Map.of("item",BuiltInRegistries.ITEM.getKey(s.getItem()).toString(),"count",s.getCount())).toList()));}
                write("kibe-state.json",Map.of("foodContents",food,''')
    return text.replace('case "fixture"->{',extra+'case "fixture"->{',1)

if __name__=='__main__':
    if '--neo-prepare'in sys.argv:
        prep.MC='1.21.1'
        _,text=prep.qa_source('siu')
        out=ROOT/'build/kibe-all-targets/observer-1.21.1/com/siu_reference';out.mkdir(parents=True,exist_ok=True)
        (out/'NativeUiReference.java').write_text(patch(text),encoding='utf-8')
        (out/'RuntimeNames.java').write_text((ROOT/'build/ssn-implementation/observer-1.21.1/com/siu_reference/RuntimeNames.java').read_text(encoding='utf-8'),encoding='utf-8')
        init=(ROOT/'build/powah-all-targets/native-1.21.1.init.gradle').read_text(encoding='utf-8').replace('build/powah-all-targets/observer-1.21.1','build/kibe-all-targets/observer-1.21.1')
        (ROOT/'build/kibe-all-targets/native-1.21.1.init.gradle').write_text(init,encoding='utf-8')
        print('Prepared excluded NeoForge observer.')
    else:
        old=prep.qa_source
        def qa(side):
            path,text=old(side);return path,patch(text)
        prep.qa_source=qa;prep.main()
