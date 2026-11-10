"""Generate the current excluded observer for the named 1.21.1 Neo runtime."""
from pathlib import Path
import importlib.util
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('modern_observer',ROOT/'tools/prepare-rollout-production-modern.py')
modern=importlib.util.module_from_spec(spec);spec.loader.exec_module(modern);modern.MC='1.21.1'
_,source=modern.qa_source('native')
source=source.replace('net.minecraftforge.fml.ModList','net.neoforged.fml.ModList')
source=source.replace('out.put("siu_loaded",coreMode());',
    'out.put("worldLoaded",mc.player!=null&&mc.level!=null);out.put("siu_loaded",coreMode());',1)
folder=ROOT/'build/all-target-rollout/native-observers/java-1.21.1-current/com/siu_reference'
folder.mkdir(parents=True,exist_ok=True);(folder/'NativeUiReference.java').write_text(source)
(folder/'RuntimeNames.java').write_text('''package com.siu_reference;
import java.lang.reflect.Field;
import java.util.Set;
/** This excluded Neo runtime already uses the official named namespace. */
final class RuntimeNames {
    static String className(String name){return name;}
    static Set<String> names(Class<?> type,String name,String kind){return Set.of(name);}
    static Field field(Class<?> type,String name)throws NoSuchFieldException{return type.getDeclaredField(name);}
}
''')
print('Generated named observer:',folder)
