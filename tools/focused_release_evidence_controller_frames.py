"""Verify the explicitly archived controller render-observation derivation.

The two original rendering checks still have to pass. A separate native frame
clock prevents tick catch-up from being mistaken for a rendering opportunity.
This verifier does not assign fresh execution to the 13 retained profiles.
"""
from pathlib import Path
import json

from focused_release_evidence import checked
from full_suite_provenance import digest_inputs, sha, sources

KIND = 'independent-native-frame-observation-v1'
MANIFEST = dict(file='output/release-readiness/controller-frame-observation-170-01/manifest.json',
                sha256='18b93062c3d53996a480422db6463c6caeb4382659bba84e5de3f92f88f6c615')
DRIVER = 'build/release-readiness/run-full-with-controller-frames-170-01.py'
BASE = 'tools/Run-ControlifyReleaseQa.ps1'
STAGES = ('persistent-tucked-tabs-after-animation', 'persistent-tabs-with-disabled-animations')


def verify_frame_observations(report):
    assert report['failures'] == 0 and not any(v['status'] == 'FAIL' for v in report['results'])
    observations = report['qaNativeFrameObservations']
    assert len(observations) == 2 and [v['stage'] for v in observations] == list(STAGES)
    for row in observations:
        assert all(type(row[key]) is int for key in ['startCompletedFrame', 'observedCompletedFrame', 'waitTicks', 'tabFrames', 'elapsedMs'])
        assert row['startCompletedFrame'] >= 0 and row['observedCompletedFrame'] > row['startCompletedFrame']
        assert row['waitTicks'] >= 0 and row['tabFrames'] > 0 and 0 <= row['elapsedMs'] <= 45_000
    for name in [*STAGES, *(name+'-completed-native-frame' for name in STAGES),
                 'tabs-rendered-after-transition', 'disabled-animation-tabs-rendered']:
        matches = [v for v in report['results'] if v['name'] == name]
        assert len(matches) == 1 and matches[0]['status'] == 'PASS', ('Missing unchanged rendering check', name)
    return True


def expected_inputs(root, case):
    root = Path(root).resolve()
    declaration = case['qa_derivation']
    assert declaration == dict(kind=KIND, base_script=BASE, manifest=MANIFEST)
    assert case['name'] == 'controller' and case['key'] == 'all:controller'
    assert case['args'] == ['-RunId', 'readiness-final-20261106', '-Phase', 'all', '-Resume']
    assert case['kinds'] == ['core', 'compatibility', 'controlify'] and case['gradle'] is False
    manifest = json.loads(checked(root,MANIFEST).read_text())
    assert manifest['status'] == 'private-native-frame-observation-adapter-prepared-not-run'
    assert all(manifest[field] is True for field in ['original_assertions_actions_settling_ticks_preserved',
            'independent_native_frame_clock','no_input_retry_or_server_override','no_production_changes'])
    assert case['script'] == manifest['wrapper']['file']
    for row in manifest['versions']:
        original = checked(root,row['original']).read_text()
        derived = original
        for change in row['substitutions']:
            assert derived.count(change['before']) == 1
            derived = derived.replace(change['before'],change['after'])
        assert checked(root,row['derived']).read_text() == derived
        config = json.loads(checked(root,row['original_config']).read_text())
        config['client'].append('QaCompletedFrameMixin')
        assert json.loads(checked(root,row['derived_config']).read_text()) == config
    canonical = dict(case,script=BASE)
    inputs = sources(root,canonical)
    assert len(inputs) == 1144 and digest_inputs(inputs) == manifest['original_qa_inputs_sha256']
    assert inputs[BASE] == manifest['original_wrapper']['sha256']
    for ref in [*manifest['derived_files'], manifest['preparer'], MANIFEST]:
        checked(root,ref)
        inputs[ref['file']] = ref['sha256']
    inputs[DRIVER] = sha(root/DRIVER)
    assert len(inputs) == 1156
    return dict(sorted(inputs.items()))


def verify_attempt(root, entry, case, original_verify):
    expected = expected_inputs(root,case)
    assert entry['qa_inputs'] == expected, 'Derived or canonical controller QA inputs changed'
    original_verify(root,entry,case)
    root = Path(root).resolve()
    manifest = json.loads(checked(root,MANIFEST).read_text())
    preserved = json.loads(checked(root,manifest['original_failure']).read_text())
    old_modern_pass = preserved['records'][0]
    assert old_modern_pass['original_status'] == 'PASS' and old_modern_pass['failures'] == 0
    master = json.loads((root/'build/controlify-release-0.1.1/readiness-final-20261106-all/results.json').read_text(encoding='utf-8-sig'))
    profiles = master['profiles']
    assert len(profiles) == len({v['key'] for v in profiles}) == 54
    assert all(v['status'] in {'PASS','PREVIOUS_PASS'} for v in profiles)
    all_profiles = [v for v in profiles if v['scope'] == 'all']
    assert len(all_profiles) == 12
    observed, retained = [], []
    for profile in all_profiles:
        report_path = root/'build/controlify-qa'/profile['key']/'results.json'
        report = json.loads(report_path.read_text())
        if profile['key'] == old_modern_pass['key']:
            assert profile['status'] == 'PREVIOUS_PASS'
            assert sha(report_path) == old_modern_pass['members'][report_path.relative_to(root).as_posix()]
            retained.append(profile['key'])
        else:
            verify_frame_observations(report)
            observed.append(profile['key'])
    assert len(observed) == 11 and len(retained) == 1
    return True
