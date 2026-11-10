"""Read the recorded QA revision after the explicit HUD-observer amendment.

This never assigns a new run or silently accepts changed source. Only the one
archived observation helper may differ, under an explicit, validated amendment.
Every other measured input must still match. Shipping subjects and actual
runtime observers remain the responsibility of the original evidence readers.
"""
from pathlib import Path
import hashlib
import json
import zipfile

from full_suite_provenance import verify_bundle
from focused_release_evidence import checked

HARNESS = 'functional-tests/src/main/java/com/salts_inventory_update/functionaltest/InWorldGameplayHarness.java'
POINTER = 'output/release-readiness/qa-observation-amendment-current.json'


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def canonical(name):
    return str(name).replace('\\', '/')


def active(root):
    return (Path(root) / POINTER).is_file()


def amendment(root):
    root = Path(root).resolve()
    pointer = json.loads((root / POINTER).read_text())
    assert pointer['status'] == 'explicit-recorded-QA-source-lookup-active'
    proof = json.loads(checked(root, pointer['amendment']).read_text())
    assert proof['status'] == 'explicit-HUD-observation-amendment-original-QA-retained'
    assert proof['changed_native_source'] == HARNESS
    old, new = proof['old_qa'], proof['new_qa']
    assert old['count'] == new['count'] == 1144
    maps = []
    for value in [old, new]:
        archive = checked(root, value['archive'])
        with zipfile.ZipFile(archive) as bundle:
            inputs = {name: sha(bundle.read(name)) for name in bundle.namelist()}
        verify_bundle(root, value['archive'], inputs)
        assert len(inputs) == 1144
        maps.append(inputs)
    before, after = maps
    assert before.keys() == after.keys()
    assert [n for n in sorted(before) if before[n] != after[n]] == [HARNESS]
    assert before[HARNESS] != after[HARNESS]
    current = root / HARNESS
    assert sha(current.read_bytes()) == after[HARNESS], 'Unreviewed current HUD observer'
    with zipfile.ZipFile(checked(root, old['archive'])) as bundle:
        old_text = bundle.read(HARNESS).decode().replace('\r\n', '\n')
    new_text = current.read_text()
    def outside(text):
        start = text.index('    private static void hudServerSelected(String label, int expected) {')
        end = text.index('    private static void hudPointerOutside()', start)
        return text[:start], text[end:]
    assert outside(old_text) == outside(new_text), 'Change outside the one observer helper'
    assert proof['tests']['passed'] == 8 and proof['tests']['exit_code'] == 0
    log = checked(root, proof['tests']['log']).read_text()
    assert 'Ran 8 tests' in log and log.rstrip().endswith('OK')
    assert proof['public_artifacts_unchanged'] is True
    return proof, before, after


class MeasuredSources:
    """Verify each original measured byte; cache only the approved ZIP lookup."""
    def __init__(self, root):
        self.root = Path(root).resolve()
        self.approved = None
        self.archived_reads = []

    def read(self, name, digest):
        name = canonical(name)
        path = (self.root / name).resolve()
        assert path.is_relative_to(self.root) and path.is_file(), 'Invalid measured QA path'
        raw = path.read_bytes()
        if sha(raw) == digest:
            return raw
        assert name == HARNESS, ('Changed measured source without scoped archive policy', name)
        if self.approved is None:
            self.approved = amendment(self.root)
        proof, before, after = self.approved
        assert digest == before[HARNESS], 'Unknown original observer revision'
        assert sha(raw) == after[HARNESS], 'Unreviewed current observer revision'
        archive = checked(self.root, proof['old_qa']['archive'])
        with zipfile.ZipFile(archive) as bundle:
            raw = bundle.read(HARNESS)
        assert sha(raw) == digest, 'Changed preserved observer source'
        self.archived_reads.append(dict(file=HARNESS, sha256=digest, source_archive=proof['old_qa']['archive']))
        return raw


def verify_inputs(root, inputs):
    reader = MeasuredSources(root)
    names = [canonical(name) for name in inputs]
    assert len(set(names)) == len(names), 'Duplicate normalized measured QA input'
    for name, digest in inputs.items():
        reader.read(name, digest)
    return reader.archived_reads
