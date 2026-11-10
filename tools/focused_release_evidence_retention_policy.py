"""Narrow build13/build14 evidence equivalence; never fabricate a gameplay run.

Only native-Traveler-absent profiles can use this policy. Archive transitions,
the actual Mixin engine gate, source/runtime provenance and current-attempt
state must all be verified by the caller. These predicates do not by themselves
grant a profile or specialized feature pass.
"""
from pathlib import Path
import hashlib
import json
import re
import zipfile

PACKET_CLASSES = {
    'com/salts_inventory_update/compat/travelersbackpack/TravelersPackets.class',
    'com/salts_inventory_update/compat/travelersbackpack/TravelersPackets$Sync.class',
}
NATIVE_PREFIX = 'com/tiviacz/travelersbackpack/'


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def archive_pair(before, after, family):
    """Read both real archives and reject every unreviewed payload difference."""
    assert family in {'core', 'compatibility', 'controlify'}
    with zipfile.ZipFile(before) as left, zipfile.ZipFile(after) as right:
        assert left.testzip() is None and right.testzip() is None
        names_before, names_after = left.namelist(), right.namelist()
        assert len(names_before) == len(set(names_before)), 'Duplicate old archive member'
        assert len(names_after) == len(set(names_after)), 'Duplicate new archive member'
        assert set(names_before) == set(names_after), 'Added or removed archive payload'
        changed = sorted(name for name in names_before if left.read(name) != right.read(name))
        if family != 'compatibility':
            assert not changed and sha(before) == sha(after), 'Core/Controlify must be byte-identical'
            return {'changed': [], 'minimum_only': []}
        assert {name for name in changed if name.endswith('.class')} == PACKET_CLASSES
        minimum_only = []
        for name in changed:
            if name in PACKET_CLASSES:
                continue
            assert name.endswith('.mixins.json'), 'Unreviewed asset/descriptor/code change'
            old, new = json.loads(left.read(name)), json.loads(right.read(name))
            assert isinstance(old, dict) and isinstance(new, dict)
            # The reviewed production configs have no parent. Do not infer an
            # inheritance policy for a future transition that adds/uses one.
            assert 'parent' not in old and 'parent' not in new, 'Unreviewed inherited minimum'
            assert 'minVersion' not in old and new.pop('minVersion') == '0.8'
            assert old == new, 'Mixin configuration changed beyond its minimum declaration'
            minimum_only.append(name)
        assert minimum_only, 'The specific reviewed metadata transition is required'
        return {'changed': changed, 'minimum_only': minimum_only}


def native_traveler_absent(indexed):
    """Inspect every declared classpath node, including active nested jars."""
    assert indexed['loader'] in {'fabric', 'forge', 'neoforge'}
    assert re.fullmatch(r'[a-f0-9]{128}', indexed['sha512'])
    assert isinstance(indexed['nodes'], list) and indexed['nodes'], 'No native classpath observation'
    for node in indexed['nodes']:
        classes, mods = node['classes'], node['mods']
        assert isinstance(classes, (set, list)) and all(isinstance(v, str) and v.endswith('.class') for v in classes)
        assert isinstance(mods, list) and all(isinstance(v['id'], str) and v['id'] for v in mods)
        if any(name.startswith(NATIVE_PREFIX) for name in classes):
            return False
        if any(mod['id'] == 'travelersbackpack' for mod in mods):
            return False
    return True


def minimum_comparisons(actual, boundary, too_old, expected_class_hash):
    """Require the recorded real-class evaluation and its two explicit controls."""
    assert re.fullmatch(r'[a-f0-9]{64}', expected_class_hash)
    for value, accepted in [(actual, True), (boundary, True), (too_old, False)]:
        assert value['classSourceMatches'] is True
        assert value['loadedClassSha256'] == expected_class_hash
        assert type(value['oldComparison']) is int and type(value['newComparison']) is int
        assert value['oldAccepted'] is True and value['oldComparison'] <= 0
        assert value['newAccepted'] is accepted
        assert (value['newComparison'] <= 0) is accepted
    assert boundary['newComparison'] == 0 and too_old['newComparison'] > 0
    return True


def historical_completion(attempt, job, expected_original_subjects):
    """Keep the old process, subjects, counts and coverage at their actual scope."""
    assert attempt['job'] == job['id']
    assert attempt['status'] == 'passed' and type(attempt['exit_code']) is int and attempt['exit_code'] == 0
    assert attempt['qa_inputs_unchanged'] is True and attempt['subjects_unchanged'] is True
    assert attempt['qa_inputs'], 'Original QA input ledger required'
    assert attempt['subjects'] == {key: list(value) for key, value in expected_original_subjects.items()}
    assert len(attempt['results']) == 1
    row = attempt['results'][0]
    assert row['target'] == job['target'] and row['status'] == 'passed'
    assert type(row['exit_code']) is int and row['exit_code'] == 0
    measured = row['results']
    assert type(measured['cases']) is int and measured['cases'] > 0
    assert type(measured['failed']) is int and measured['failed'] == 0
    assert type(measured['assertions']) is int and measured['assertions'] > 0
    return row


def latest_state_allows_retention(latest, original, expected_current_subjects):
    """Never hide a newer failure, override a live replay, or duplicate a pass.

    A deliberately interrupted replay needs its own checked orchestration
    receipt outside this predicate. No interrupted state is approved here.
    """
    assert latest['job'] == original['job']
    if latest['run'] == original['run']:
        assert latest == original
        return True
    if latest['status'] == 'failed':
        raise AssertionError('A newer failed attempt requires investigation, not an older pass')
    if latest['status'] == 'running':
        return False
    if latest['status'] == 'passed':
        assert latest['subjects'] == {key: list(value) for key, value in expected_current_subjects.items()}
        return False  # Prefer the actual clean current-artifact execution.
    raise AssertionError('Unreviewed newer attempt state; no retention permitted')
