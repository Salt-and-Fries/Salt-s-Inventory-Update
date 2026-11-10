"""Read original full-phase QA inputs without relabelling them as current runs.

The original verifiers still inspect every archive, runtime copy, derived input,
native report and predicate. Only their source-directory lookup is scoped to the
exact archived 1144-file revision, rather than today's larger tool directory.
"""
from contextlib import contextmanager
from functools import lru_cache
from pathlib import Path
import json

from focused_release_evidence import checked
from focused_release_evidence_session_registration import verify_scoped_qa_inputs
from full_suite_provenance import digest_inputs, verify_bundle
import focused_release_evidence_controller_frames as controller
import focused_release_evidence_ae2_drive_gutter as legacy
import focused_release_evidence_ae2_modern_drive as modern
import focused_release_evidence_toms_layout as toms

HISTORY = dict(file='output/release-readiness/final-phase-QA-history-1713-01/manifest.json',
               sha256='f905d7e0a0a2b88f06dfc7c66866bb95f6a672b4b8e2317efc38274d7dfd0be9')
BASE_DIGEST = '57c834108359e803dece14e906db7add54bbc09fdd1ae2582acde106c9d04698'


@lru_cache(maxsize=1)
def history(root):
    root = Path(root).resolve()
    data = json.loads(checked(root, HISTORY).read_text())
    assert data['status'] == 'exact1144-original-full-phase-QA-inputs-preserved-two-reviewed-source-deltas-fourteen-later-tools'
    base = data['base_inputs']
    assert len(base) == 1144 and digest_inputs(base) == data['base_inputs_sha256'] == BASE_DIGEST
    verify_bundle(root, data['base_archive'], base)
    assert verify_scoped_qa_inputs(root, {'qa_inputs': base}) == data['reviewed_source_changes']
    assert len(data['later_standalone_or_reporting_tools']) == 14
    return data


def validate_lookup(data, current):
    """Reject missing inputs, unknown additions or any unreviewed byte change."""
    base, later = data['base_inputs'], data['later_standalone_or_reporting_tools']
    assert set(base).isdisjoint(later)
    assert set(current) == set(base) | set(later), 'Unknown/missing current full-phase QA input'
    expected = dict(base, **later)
    for row in data['reviewed_source_changes']:
        assert expected[row['file']] == row['original_sha256']
        expected[row['file']] = row['current_sha256']
    assert current == expected, 'Changed full-phase QA input outside the exact archived revision history'


@contextmanager
def archived_source_lookup(root, base):
    """Temporarily select an immutable source revision; restore every reader."""
    modules = (controller, legacy, modern, toms)
    originals = [module.sources for module in modules]
    def sources(actual_root, case):
        assert Path(actual_root).resolve() == Path(root).resolve()
        assert case['script'] in base, 'Unknown historical base launcher'
        return dict(base)
    try:
        for module in modules:
            module.sources = sources
        yield
    finally:
        for module, original in zip(modules, originals):
            module.sources = original


def verify_phase(root, case, entry, full):
    data = history(str(Path(root).resolve()))
    canonical = (toms.canonical_case(root, case) if case.get('qa_derivation', {}).get('kind') is None
                 else dict(case, script=case['qa_derivation']['base_script']))
    validate_lookup(data, full.qa_fingerprints(canonical))
    kind = case.get('qa_derivation', {}).get('kind')
    with archived_source_lookup(root, data['base_inputs']):
        if kind == controller.KIND:
            controller.verify_attempt(root, entry, case, full.verify_qa_attempt)
        elif kind == modern.KIND:
            modern.verify_attempt(root, entry, case, full.verify_qa_attempt)
        elif kind in {legacy.KIND, legacy.NATIVE_KIND}:
            legacy.verify_attempt(root, entry, case, full.verify_qa_attempt)
        else:
            expected = dict(data['base_inputs'])
            if canonical is not case:
                expected[case['script']] = full.provenance_sha(Path(root) / case['script'])
            assert entry['qa_inputs'] == expected, 'Original measured QA input set differs'
            toms.verify_attempt(root, entry, case, full.verify_qa_attempt)
    return dict(history=HISTORY, original_qa_source_archive=entry['qa_source_archive'],
                reviewed_source_changes=data['reviewed_source_changes'],
                later_tools_not_retroactive_runtime_inputs=data['later_standalone_or_reporting_tools'])
