"""Prepare excluded modern Powah observers; publisher and release JARs stay intact."""
from pathlib import Path
import importlib.util
import sys

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('powah_legacy_observer',ROOT/'functional-tests/powah-windows/prepare.py')
legacy=importlib.util.module_from_spec(spec);spec.loader.exec_module(legacy)
for version in ['1.21.1','26.1.2']:
    source=ROOT/f'build/ssn-implementation/observer-{version}/com/siu_reference'
    out=ROOT/f'build/powah-all-targets/observer-{version}/com/siu_reference';out.mkdir(parents=True,exist_ok=True)
    text=(source/'NativeUiReference.java').read_text()
    extra=legacy.EXTRA
    extra=extra.replace('new ItemStack(BuiltInRegistries.ITEM.get(id(chosen)),36)','new ItemStack(BuiltInRegistries.ITEM.get(id(chosen)),36)')
    extra=extra.replace('BuiltInRegistries.BLOCK.get(id("powah:energy_cell_basic")).defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,false)',
                        'BuiltInRegistries.BLOCK.get(id("powah:energy_cable_starter")).defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,false)')
    # Observation only: include real data components for the bound card and FE.
    extra=extra.replace('s.getItem().save(new net.minecraft.nbt.CompoundTag()).toString()',
                        '(s.getItem().toString()+" "+s.getItem().getComponents().toString())')
    extra=extra.replace('player().getInventory().getItem(i).save(new net.minecraft.nbt.CompoundTag()).toString()',
                        '(player().getInventory().getItem(i).toString()+" "+player().getInventory().getItem(i).getComponents().toString())')
    helper=legacy.HELPERS.replace('block.saveWithFullMetadata().toString()','block.saveWithFullMetadata(player().registryAccess()).toString()')
    if version=='26.1.2':
        extra=extra.replace('mc.setScreen(null)','setScreen(null)').replace('p.serverLevel()','serverLevel()').replace('player().serverLevel()','serverLevel()')
        extra=extra.replace('p.getInventory().selected','p.getInventory().getSelectedSlot()')
        extra=extra.replace('BuiltInRegistries.ITEM.get(','BuiltInRegistries.ITEM.getValue(').replace('BuiltInRegistries.BLOCK.get(','BuiltInRegistries.BLOCK.getValue(')
        extra=extra.replace('p.connection.teleport(8.5,65,12.5,180,8)','command("tp @a 8.5 65 12.5 180 8")')
        extra=extra.replace('field(m,"te")','field(m,"blockEntity")')
        extra=extra.replace('ClickType.QUICK_MOVE','ContainerInput.QUICK_MOVE')
    text=text.replace('case "fixture"->{',extra+'case "fixture"->{',1)
    text=text[:text.rfind('}')]+helper+'\n}\n'
    (out/'NativeUiReference.java').write_text(text)
    (out/'RuntimeNames.java').write_text((source/'RuntimeNames.java').read_text())
    init=(ROOT/f'build/ssn-implementation/native-{version}.init.gradle').read_text().replace(
        f'build/ssn-implementation/observer-{version}',f'build/powah-all-targets/observer-{version}')
    (ROOT/f'build/powah-all-targets/native-{version}.init.gradle').write_text(init)
    print('Prepared',version,'focused Powah observer')
