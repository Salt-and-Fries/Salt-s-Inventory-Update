"""Excluded fixture inputs; original Extended AE screens and networking unchanged."""
from pathlib import Path
import importlib.util,sys
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
EXTRA=r'''
            case "eae-pattern-view"->{
                // Same original packet sent by the native tooltip hotkey. This
                // visual fixture does not assert raw GLFW keyboard behavior.
                try{
                    String prefix;try{Class.forName("com.github.glodblock.extendedae.network.packet.CPatternKey");prefix="com.github.glodblock.extendedae";}
                    catch(ClassNotFoundException forge){prefix="com.glodblock.github.extendedae";}
                    var stack=mc.player.getMainHandItem();
                    if(!BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("pattern"))throw new IllegalStateException("Hold a real encoded pattern first");
                    var packet=Class.forName(prefix+".network.packet.CPatternKey").getConstructor(ItemStack.class).newInstance(stack);
                    String networkClass=prefix.endsWith("glodblock.extendedae")?"EAENetworkServer":"EPPNetworkHandler";
                    Object network=Class.forName(prefix+".network."+networkClass).getField("INSTANCE").get(null);
                    coreCall(network,"sendToServer",packet);
                }catch(Exception error){throw new IllegalStateException(error);}
            }
'''
if __name__=='__main__':
 forge='--forge'in sys.argv
 if forge:sys.argv.remove('--forge')
 script='prepare-rollout-production-forge.py'if forge else'prepare-rollout-production-modern.py'
 spec=importlib.util.spec_from_file_location('extended_ae_reference',ROOT/'tools'/script);prep=importlib.util.module_from_spec(spec);spec.loader.exec_module(prep)
 source=prep.modern if forge else prep;old=source.qa_source
 def qa(side):
  path,text=old(side);text=text.replace('case "fixture"->{',EXTRA+'case "fixture"->{',1)
  needle='static Map<String,Object> nativeValues(Object screen){\n        var values=new LinkedHashMap<String,Object>();'
  extra='''
        if(screen.getClass().getName().contains(".extendedae."))try{
            Object menu=((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)screen).getMenu();
            for(String name:List.of("page","currentMode","exp","exp2","lower","upper","itemName","runningThreads"))try{values.put(name,String.valueOf(field(menu,name)));}catch(Exception absent){}
            for(String name:List.of("visibleRows","runningThreads","rows","infos"))try{Object value=field(screen,name);values.put(name,value instanceof java.util.Collection<?> list?list.size():value instanceof Map<?,?> map?map.size():String.valueOf(value));}catch(Exception absent){}
        }catch(Exception error){values.put("extendedObserverError",error.toString());}
'''
  return path,text.replace(needle,needle+extra,1)
 source.qa_source=qa;prep.main()
