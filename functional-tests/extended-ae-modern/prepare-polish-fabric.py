"""Clone the approved production Fabric fixture with a read-only frame observer."""
from pathlib import Path
import importlib.util, json, shutil, subprocess, os, hashlib, sys

root = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('production_qa', root/'tools/prepare-production-fabric-audit.py')
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
old_source = mod.qa_source
s = (root/'functional-tests/all-target-rollout/java-1.20.1/com/salts_inventory_update/functionaltest/EverythingAuditQa.java').read_text()
helper = s[s.index('    // Read-only evidence for raised'):s.rfind('\n}')].replace('call(', 'coreCall(')
# Compile Minecraft method references, so Tiny Remapper can map their names.
# Reflection by a named Minecraft method string would fail in intermediary.
helper = helper.replace('for(Object rect:(List<?>)coreCall(screen,"desktopExternalBounds"))external.add(Map.of("x",coreCall(rect,"getX"),"y",coreCall(rect,"getY"),"width",coreCall(rect,"getWidth"),"height",coreCall(rect,"getHeight")));',
    'for(Object value:(List<?>)coreCall(screen,"desktopExternalBounds")){var rect=(net.minecraft.client.renderer.Rect2i)value;external.add(Map.of("x",rect.getX(),"y",rect.getY(),"width",rect.getWidth(),"height",rect.getHeight()));}')

def qa_source(side):
    text = old_source(side)
    needle = 'row.put("nativeScreen",screen.getClass().getName());'
    assert text.count(needle) == 1
    text = text.replace(needle, needle+'if(screen.getClass().getName().contains(".extendedae.")||screen.getClass().getName().startsWith("appeng.")){auditAeFrame(screen,row);row.put("wirelessGeometry",coreCall(screen,"desktopFidelityGeometry"));}')
    pos = text.rfind('\n}')
    return text[:pos]+helper+text[pos:]

mod.qa_source = qa_source
qa = mod.prepare_qa('integrated')
old = root/'build/remaining-ui-audit/gameplay/1.20.1-fabric-siu-extended-ae-impl-10'
new = old.with_name('1.20.1-fabric-siu-'+sys.argv[1])
assert not new.exists()
new.mkdir()
launch = json.loads((old/'launch.json').read_text())
oldruntime = Path(launch['cwd'])
runtime = oldruntime.with_name(new.name)
assert not runtime.exists()
(runtime/'mods').mkdir(parents=True)
(runtime/'options.txt').write_text('tutorialStep:none\nrenderDistance:2\nsimulationDistance:5\nguiScale:2\npauseOnLostFocus:false\nautoJump:false\n')
staged = json.loads((old/'staged-artifacts.json').read_text())
for a in staged['artifacts']:
    p = Path(a['file'])
    if a['family'] == 'compatibility':
        p = root/'compatibility/versions/1.20.1/fabric/build/libs/salts_inventory_compat-1.20.1-Fabric-0.1.3.jar'
    if a['family'] == 'qa':
        p = qa
    shutil.copy2(p, runtime/'mods'/a['filename'])
    a.update(file=str(p.resolve()), sha256=hashlib.sha256(p.read_bytes()).hexdigest(), sha512=hashlib.sha512(p.read_bytes()).hexdigest())
args = (old/'java.args').read_text().replace(str(oldruntime).replace('\\','/'), str(runtime).replace('\\','/')).replace(str(oldruntime), str(runtime))
(new/'java.args').write_text(args)
launch.update(arguments=str(new/'java.args'), cwd=str(runtime), log=str(new/'launch.log'))
launch['environment']['NATIVE_REFERENCE_OUTPUT'] = str(new)
(new/'launch.json').write_text(json.dumps(launch, indent=2))
(new/'staged-artifacts.json').write_text(json.dumps(staged, indent=2))
with (new/'launch.log').open('w') as log:
    p = subprocess.Popen([launch['java'], '@'+launch['arguments']], cwd=runtime,
        env={**os.environ, **launch['environment']}, stdout=log, stderr=subprocess.STDOUT,
        creationflags=subprocess.CREATE_NO_WINDOW)
print('Fabric production client', p.pid)
