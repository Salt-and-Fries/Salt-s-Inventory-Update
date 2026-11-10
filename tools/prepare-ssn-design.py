"""Disposable SIU-free SSN reference fixtures. Publisher JARs stay unchanged."""
from pathlib import Path
import importlib.util,sys
ROOT=Path(__file__).resolve().parents[1]

EXTRA=r'''case "ssn-setup"->{
    mc.player.closeContainer();mc.setScreen(null);
    work=server(()->{
        var p=player();var level=p.serverLevel();p.getInventory().clearContent();
        for(int x=6;x<=11;x++)for(int z=6;z<=9;z++)level.removeBlock(new BlockPos(x,65,z),false);
        String chosen=str(c,"block","storagenetwork:inventory");
        BlockPos master=chosen.equals("storagenetwork:master")?POS:POS.north();
        var places=new java.util.LinkedHashMap<BlockPos,String>();
        places.put(POS,chosen);places.put(POS.east(),"storagenetwork:storage_kabel");places.put(POS.east(2),"minecraft:chest");
        places.put(master,"storagenetwork:master");
        for(var e:places.entrySet()){
            var block=BuiltInRegistries.BLOCK.get(id(e.getValue()));var bs=block.defaultBlockState();
            level.setBlockAndUpdate(e.getKey(),bs);block.setPlacedBy(level,e.getKey(),bs,p,new ItemStack(block.asItem()));
        }
        var chest=(net.minecraft.world.Container)level.getBlockEntity(POS.east(2));
        String[] items={"stone","cobblestone","oak_log","oak_planks","birch_log","spruce_log","iron_ingot","gold_ingot","copper_ingot","redstone","lapis_lazuli","diamond","emerald","coal","glass","sand","dirt","gravel","clay_ball","stick","wheat","carrot","potato","apple","bread","torch","book"};
        for(int i=0;i<items.length;i++)chest.setItem(i,new ItemStack(BuiltInRegistries.ITEM.get(id("minecraft:"+items[i])),i+12));
        p.connection.teleport(8.5,65,12.5,180,8);return true;
    });
}
'''

def patch(text,named):
    needle='case "move"->{mc.screen.mouseMoved('
    replacement='''case "move"->{
        try{
            var px=FIELDX;px.setAccessible(true);var py=FIELDY;py.setAccessible(true);
            px.setDouble(mc.mouseHandler,c.get("x").getAsDouble()*mc.getWindow().getWidth()/mc.getWindow().getGuiScaledWidth());
            py.setDouble(mc.mouseHandler,c.get("y").getAsDouble()*mc.getWindow().getHeight()/mc.getWindow().getGuiScaledHeight());
        }catch(Exception e){throw new IllegalStateException(e);}
        mc.screen.mouseMoved('''
    field=lambda name:('mc.mouseHandler.getClass().getDeclaredField("'+name+'")' if named else 'RuntimeNames.field(mc.mouseHandler.getClass(),"'+name+'")')
    if needle in text:
        text=text.replace(needle,replacement.replace('FIELDX',field('xpos')).replace('FIELDY',field('ypos')))
    else:
        assert 'case "move"->{try{' in text # Current observer already moves the real pointer.
    return text.replace('case "fixture"->{',EXTRA+'case "fixture"->{',1)

if __name__=='__main__':
    if '--neo' in sys.argv:
        source=ROOT/'build/all-target-rollout/native-observers/java-1.21.1-current/com/siu_reference'
        out=ROOT/'tmp/pdfs/ssn/observer-1211/com/siu_reference';out.mkdir(parents=True,exist_ok=True)
        (out/'NativeUiReference.java').write_text(patch((source/'NativeUiReference.java').read_text(),True))
        (out/'RuntimeNames.java').write_text((source/'RuntimeNames.java').read_text())
        init=(ROOT/'functional-tests/all-target-rollout/native.init.gradle').read_text()
        line=next(l for l in init.splitlines() if l.strip().startswith('qa.java.srcDir(root.file(enhancedNeo?'))
        init=init.replace(line,"    qa.java.srcDir(root.file('tmp/pdfs/ssn/observer-1211'));"+line.split(';',1)[1])
        (ROOT/'tmp/pdfs/ssn/native-1211.init.gradle').write_text(init)
        print('Prepared isolated Neo observer and launch init')
    else:
        spec=importlib.util.spec_from_file_location('ssn_forge',ROOT/'tools/prepare-rollout-production-forge.py')
        prep=importlib.util.module_from_spec(spec);spec.loader.exec_module(prep)
        old=prep.modern.qa_source
        def qa(side):
            key,source=old(side);return key,patch(source,False)
        prep.modern.qa_source=qa;prep.main()
