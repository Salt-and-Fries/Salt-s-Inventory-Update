"""Use the current excluded observer in Forge's named reference launcher."""
from pathlib import Path
import importlib.util
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('modern',ROOT/'tools/prepare-rollout-production-modern.py')
modern=importlib.util.module_from_spec(spec);spec.loader.exec_module(modern);modern.MC='1.20.1'
folder=ROOT/'build/all-target-rollout/forge-observer/com/siu_reference';folder.mkdir(parents=True,exist_ok=True)
_,text=modern.qa_source('siu');(folder/'NativeUiReference.java').write_text(text)
(folder/'RuntimeNames.java').write_text('''package com.siu_reference;
import java.lang.reflect.Field;
import java.util.Set;
/** Forge's named QA runtime already uses the observer's compilation names. */
final class RuntimeNames {
 static String className(String name){return name;}
 static Set<String> names(Class<?> type,String name,String kind){return Set.of(name);}
 static Field field(Class<?> type,String name)throws NoSuchFieldException{return type.getDeclaredField(name);}
}
''')
source=(ROOT/'functional-tests/all-target-rollout/native.init.gradle').read_text()
line=next(line for line in source.splitlines() if line.strip().startswith('qa.java.srcDir(root.file(integrated?'))
source=source.replace(line,"    qa.java.srcDir(root.file('build/all-target-rollout/forge-observer'));"+line.split(';',1)[1])
init=ROOT/'build/all-target-rollout/forge-reference.init.gradle';init.write_text(source)
print('Prepared named Forge observer:',folder,init)
