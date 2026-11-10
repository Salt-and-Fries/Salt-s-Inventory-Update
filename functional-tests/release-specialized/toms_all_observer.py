"""Adapt the same read-only Tom ledgers to their pinned native releases.

Legacy channel observations read the actual map in the disposable single-owner
world; they do not claim multiplayer visibility rules. World/chest setup is the
same explicit native fixture. No menu mutation or action handler is replaced.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
SNIPPET = ROOT / 'functional-tests/project-everything-regression/release-toms-observer.java.inc'


def helpers(mc, named):
    text = SNIPPET.read_text()
    old = 'BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(i==1?"toms_storage:inventory_connector":"minecraft:chest"))'
    assert text.count(old) == 1
    if named:
        block = 'registry(BuiltInRegistries.BLOCK,i==1?"toms_storage:inventory_connector":"minecraft:chest")'
        block = '(net.minecraft.world.level.block.Block)' + block
    else:
        getter = 'get' if mc in ['1.20.1', '1.21.1'] else 'getValue'
        block = 'BuiltInRegistries.BLOCK.' + getter + '(id(i==1?"toms_storage:inventory_connector":"minecraft:chest"))'
    text = text.replace(old, block)
    if mc == '1.20.1':
        text = text.replace('com.tom.storagemod.inventory.RemoteConnections', 'com.tom.storagemod.util.RemoteConnections')
        old = 'coreCall(remote,"streamChannels",player())'
        assert text.count(old) == 1
        text = text.replace(old, '((Map<?,?>)field(remote,"connections")).entrySet().stream()')
    if named:
        text = re.sub(r'\bcoreCall\(', 'call(', text)
        text = text.replace('regressionDesktop()', 'h("screen")').replace('regressionWindow(sid,null)', 'h("window",sid,null)')
        text = re.sub(r'\bPOS\b', 'origin()', text)
    return text


def inject(text, mc, named=False):
    anchor = 'if(kind.equals("regression-world"))return true;'
    assert text.count(anchor) == 1
    text = text.replace(anchor, anchor + '\n        if(kind.equals("regression-toms-state")){releaseTomsObserve(c);return true;}'
                        '\n        if(kind.equals("regression-toms-network")){releaseTomsNetworkFixture();return true;}', 1)
    return text[:text.rfind('}')] + helpers(mc, named) + '\n}\n'


def prepare_named(mc, run, enhance=None):
    folder = ROOT / 'build/release-readiness' / run / 'toms-observer'
    assert not folder.exists(); java = folder / 'java/com/salts_inventory_update/functionaltest'
    java.mkdir(parents=True)
    original = ROOT / ('functional-tests/all-target-rollout/java-' + mc) / 'com/salts_inventory_update/functionaltest/EverythingAuditQa.java'
    text = inject(original.read_text(), mc, True)
    if enhance: text = enhance(text, mc, True)
    (java / original.name).write_text(text)
    init = (ROOT / 'functional-tests/project-everything-regression/core.init.gradle').read_text()
    old = 'qa.java.srcDir(root.file("functional-tests/all-target-rollout/java-${mc}"))'
    assert init.count(old) == 1
    init = init.replace(old, 'qa.java.srcDir(root.file("' + (folder / 'java').relative_to(ROOT).as_posix() + '"))')
    init = private_named_init(init, run)
    path = folder / 'toms.init.gradle'; path.write_text(init)
    return path, original


def private_named_init(text, run):
    """Retain the actual built and copied QA archive for this named run only."""
    assert run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in run)
    folder='build/release-readiness/'+run+'/toms-observer'
    changes={
        "def generated = target.layout.buildDirectory.dir('chest-close-qa-generated').get().asFile":
            "def generated = root.file('"+folder+"/generated-metadata')",
        "destinationDirectory.set(target.layout.buildDirectory.dir('chest-close-qa-artifact'))":
            "destinationDirectory.set(root.layout.projectDirectory.dir('"+folder+"/artifact'))",
    }
    for old,new in changes.items():
        assert text.count(old)==1, 'Named QA archive path contract changed'
        text=text.replace(old,new,1)
    anchor="} else root.copy { from(qaJar.get().archiveFile); into(mods) }"
    assert text.count(anchor)==1, 'Named QA copy contract changed'
    addition='''
                if (loader != 'neoforge' || !fullGameplay || fullScenario != 'extension-empty')
                    throw new GradleException('Private named provenance requires the ordinary NeoForge extension-empty runtime')
                def observedQa = qaJar.get().archiveFile.get().asFile
                def copiedQa = new File(mods,observedQa.name)
                def qaDigest = MessageDigest.getInstance('SHA-256').digest(observedQa.bytes).encodeHex().toString()
                if (!copiedQa.isFile() || MessageDigest.getInstance('SHA-256').digest(copiedQa.bytes).encodeHex().toString() != qaDigest)
                    throw new GradleException('Actual named QA copy does not match the built archive')
                inputs.add([file:observedQa,filename:observedQa.name,family:'qa'])
                if (inputs*.filename.toSet().size()!=inputs.size()) throw new GradleException('Duplicate named runtime filename')
                new File(output,'staged-artifacts.json').text=JsonOutput.prettyPrint(JsonOutput.toJson([
                    minecraft:mc,loader:loader,scope:'gameplay-extension-empty',
                    artifacts:inputs.collect { entry -> [family:entry.family,file:entry.file.absolutePath,filename:entry.filename,
                        sha256:MessageDigest.getInstance('SHA-256').digest(entry.file.bytes).encodeHex().toString()] }]))
'''
    return text.replace(anchor,anchor+addition,1)
