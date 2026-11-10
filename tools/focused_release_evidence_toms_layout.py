"""Verify the narrowly derived native-layout Tom fixture at its original scope.

No test input, expected server value, or visibility threshold may change. The
one measured wrapper embeds its complete derived Java and Gradle init bytes;
ordinary report/runtime/dependency/observer checks still use the base contract.
"""
from pathlib import Path
import base64
import json

from focused_release_evidence import checked
from full_suite_provenance import sources, sha

BASE = 'functional-tests/scripts/Run-ReleaseGameplayQa.ps1'
JAVA = 'functional-tests/src/main/java/com/salts_inventory_update/functionaltest/InWorldGameplayHarness.java'
CHANGES = [('int '
  'rightExtent=label.equals("toms-level_emitter")?132:164,bottomExtent=label.equals("toms-level_emitter")?60:130;',
  'int '
  'rightExtent=label.equals("toms-level_emitter")?164:166,bottomExtent=label.equals("toms-level_emitter")?62:148;'),
 ('doubleValue()+8+rightExtent', 'doubleValue()+rightExtent'),
 ('doubleValue()+8+bottomExtent', 'doubleValue()+bottomExtent'),
 ('doubleValue()+8+184<=number(activeWindow,"x")', 'doubleValue()+164<=number(activeWindow,"x")'),
 ('doubleValue()+8+64<=number(activeWindow,"y")', 'doubleValue()+60<=number(activeWindow,"y")'),
 ('clickAt(((Number)call(context,"contentX")).doubleValue()+92,((Number)call(context,"contentY")).doubleValue()+59);',
  'clickAt(((Number)call(context,"contentX")).doubleValue()+24,((Number)call(context,"contentY")).doubleValue()+12);'),
 ('int x=((Number)call(context,"contentX")).intValue()+8,y=((Number)call(context,"contentY")).intValue()+8;',
  'int x=((Number)call(context,"contentX")).intValue(),y=((Number)call(context,"contentY")).intValue();\n'
  '        int '
  'titleX=((Number)call(context,"windowX")).intValue()+5,titleY=((Number)call(context,"windowY")).intValue()+4;'),
 ('assertTextPixels(image,scale,"toms-emitter-label-visible",x+28,y+10,84,9,false);',
  'assertTextPixels(image,scale,"toms-emitter-label-visible",titleX,titleY,84,9,false);'),
 ('assertTextPixels(image,scale,"toms-emitter-count-visible",x+32,y+28,50,9,true);',
  'assertTextPixels(image,scale,"toms-emitter-count-visible",x+65,y+27,57,8,true);'),
 ('for(int '
  'i=0;i<4;i++)assertTextPixels(image,scale,"toms-emitter-adjustment-"+i+"-visible",x+31+i*24,y+49,16,9,true);',
  'int[] buttonX={14,39,69,104},buttonWidth={20,25,30,35};\n'
  '                for(int row=0;row<2;row++)for(int '
  'i=0;i<4;i++)assertTextPixels(image,scale,"toms-emitter-adjustment-"+(row*4+i)+"-visible",x+buttonX[i]+3,y+8+row*40,buttonWidth[i]-6,8,true);')]


def derive_java(original):
    text = original
    for before, after in CHANGES:
        assert text.count(before) == 1, 'Missing/ambiguous original Tom geometry'
        text = text.replace(before, after)
    restored = text
    for before, after in reversed(CHANGES):
        assert restored.count(after) == 1
        restored = restored.replace(after, before)
    assert restored == original, 'Change outside the reviewed Tom geometry'
    return text


def init_text():
    return '''gradle.projectsEvaluated {
    def root=gradle.rootProject
    if(root.providers.gradleProperty('qaScenario').get()!='toms')throw new GradleException('Tom-only QA override')
    def mc=root.providers.gradleProperty('qaMinecraft').get()
    def loader=root.providers.gradleProperty('qaLoader').get()
    def target=root.project(":${mc}:${loader}")
    def qa=target.sourceSets.getByName(loader=='fabric'?'gameplayQa':'chestCloseQa')
    def original=root.file('functional-tests/src/main/java/com/salts_inventory_update/functionaltest/InWorldGameplayHarness.java')
    qa.java.srcDir(root.file('build/release-readiness/toms-native-layout-137-01/java'))
    target.tasks.named(qa.compileJavaTaskName).configure {
        setSource(source.filter { it.canonicalPath!=original.canonicalPath })
        inputs.file(original)
        outputs.upToDateWhen { false }
    }
}
'''


def wrapper_text(original, root, folder, java_bytes, init_bytes):
    root, folder = Path(root), Path(folder)
    repo = "$repo=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path"
    assert original.count(repo) == 1
    text = original.replace(repo, "$repo='" + str(root).replace("'", "''") + "'")
    payload = ["if($Scenario -ne 'toms' -or !$IntegrationOnly -or $CompleteCore -or $DedicatedServer -or $UpstreamOnly -or $Accessory){throw 'Only the original integration-only Tom profile may use this derived fixture'}"]
    for path in [root / JAVA, root / BASE]:
        payload.append("if((Get-FileHash -LiteralPath '" + str(path).replace("'", "''") + "' -Algorithm SHA256).Hash.ToLowerInvariant() -ne '" + sha(path) + "'){throw 'Canonical QA source changed'}")
    for path, raw in [(folder / 'java/com/salts_inventory_update/functionaltest/InWorldGameplayHarness.java', java_bytes), (folder / 'native-layout.init.gradle', init_bytes)]:
        import hashlib
        payload += ["$tomsLayoutBytes=[Convert]::FromBase64String('" + base64.b64encode(raw).decode() + "')",
            "$tomsLayoutPath='" + str(path).replace("'", "''") + "'",
            "if(!(Test-Path -LiteralPath $tomsLayoutPath)){[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($tomsLayoutPath))|Out-Null;[IO.File]::WriteAllBytes($tomsLayoutPath,$tomsLayoutBytes)}",
            "if((Get-FileHash -LiteralPath $tomsLayoutPath -Algorithm SHA256).Hash.ToLowerInvariant() -ne '" + hashlib.sha256(raw).hexdigest() + "'){throw 'Derived Tom input changed'}"]
    payload.append("$argsList+=@('-I','" + str(folder / 'native-layout.init.gradle').replace("'", "''") + "')")
    anchor = 'Push-Location -LiteralPath $repo'
    assert text.count(anchor) == 1
    return text.replace(anchor, '\n'.join(payload) + '\n' + anchor)


def canonical_case(root, case):
    if not case.get('qa_derivation'):
        return case
    root = Path(root).resolve()
    declaration = case['qa_derivation']
    assert declaration['base_script'] == BASE and case['name'] == 'toms'
    assert case['key'] == case['minecraft'] + ':' + case['loader'] + ':toms'
    assert case['args'] == ['-Minecraft', case['minecraft'], '-Loader', case['loader'], '-Scenario', 'toms', '-IntegrationOnly', '-RunId', '{runid}']
    assert case['kinds'] == ['core', 'compatibility'] and case['gradle'] is False
    manifest = json.loads(checked(root, declaration['manifest']).read_text())
    assert manifest['status'] == 'derived-Tom-oracles-prepared-not-run'
    assert manifest['original']['file'] == JAVA and manifest['original_wrapper']['file'] == BASE
    assert manifest['geometry_substitutions'] == [dict(before=a, after=b) for a, b in CHANGES]
    for field in ['original_assertions_and_server_values_preserved', 'all_eight_adjustment_label_regions', 'original_pixel_threshold_preserved', 'no_input_retry_or_server_override']:
        assert manifest[field] is True
    original = checked(root, manifest['original'])
    base = checked(root, manifest['original_wrapper'])
    java = checked(root, manifest['derived'])
    init = checked(root, manifest['init'])
    wrapper = checked(root, manifest['wrapper'])
    assert case['script'] == wrapper.relative_to(root).as_posix()
    folder = wrapper.parent
    assert folder.relative_to(root).as_posix() == 'build/release-readiness/toms-native-layout-137-01'
    assert java == folder / 'java/com/salts_inventory_update/functionaltest/InWorldGameplayHarness.java'
    assert init == folder / 'native-layout.init.gradle'
    assert java.read_text() == derive_java(original.read_text()), 'Unreviewed Java/test changes'
    assert init.read_text() == init_text(), 'Unreviewed Gradle override'
    assert wrapper.read_text() == wrapper_text(base.read_text(), root, folder, java.read_bytes(), init.read_bytes()), 'Original wrapper flags/assertions changed'
    result = dict(case)
    result['script'] = BASE
    return result


def verify_attempt(root, entry, case, original_verify):
    canonical = canonical_case(root, case)
    original_verify(root, entry, canonical)
    if canonical is case:
        return True
    expected = sources(root, canonical)
    expected[case['script']] = sha(Path(root) / case['script'])
    assert entry['qa_inputs'] == expected, 'Derived wrapper or original measured input missing'
    report = json.loads(checked(root, entry['runtime_evidence']['report']).read_text())
    assert report['scenario'] == 'toms' and report['failures'] == 0
    checks = report['results']
    for i in range(8):
        matches = [v for v in checks if v['name'] == 'toms-emitter-adjustment-' + str(i) + '-visible']
        assert len(matches) == 1 and matches[0]['status'] == 'PASS'
    if case['minecraft'] == '1.20.1':
        for name in ['legacy-emitter-increase-click', 'legacy-emitter-native-count-updated']:
            assert any(v['name'] == name and v['status'] == 'PASS' for v in checks), 'Original single-click/server predicate missing'
    return True
