"""Resolve three historical class-catalogue aliases to the tested advertised pin.

This does not qualify every jar containing an Accessories class. Historical
standalone/nested candidates stay visible; only the advertised beta.54 publisher
observed in the original loader and sealed functional run is selected.
"""
from functools import lru_cache
from pathlib import Path
import hashlib
import json
import re

from focused_release_evidence import checked, verify
from native_publisher_index import index, matches

RECEIPT = {
    'file': 'output/release-readiness/accessories-publisher-scope-1706-01/manifest.json',
    'sha256': '8ea3dcd8f715b372966446dfbced49e1b2627aae13961c5a3e86dc36cb0b04d8',
}
PAIRS = {('1.21.1/fabric', 'accessories-experimental'),
         ('1.21.1/neoforge', 'accessories'),
         ('1.21.1/neoforge', 'accessories-experimental')}


def record_hash(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


def validate_selection(root, reviewed, catalogue, receipt):
    """Strict input validation, also exercised with corrupted copies by the tests."""
    assert receipt['status'] == 'three-Accessories-cases-bound-to-current-advertised-and-actually-loaded-publishers'
    assert {(v['target'], v['case']) for v in receipt['records']} == PAIRS
    assert len(receipt['records']) == 3
    pair = (reviewed['target'], reviewed['case'])
    assert pair in PAIRS, 'No publisher-scope exception for this case'
    row = next(v for v in receipt['records'] if (v['target'], v['case']) == pair)
    assert record_hash(reviewed) == row['reviewed_record_sha256'], 'Changed original feature record'
    assert catalogue == row['historical_catalogue_candidates'], 'Changed historical publisher catalogue'
    proof = json.loads(checked(root, receipt['original_feature_proof']).read_text())
    assert reviewed in proof['records']
    availability = json.loads(checked(root, receipt['availability']).read_text())
    pin = row['advertised_pin']
    assert pin in availability and pin['project'] == 'Accessories'
    assert (pin['minecraft'] + '/' + pin['loader']) == pair[0]
    assert pin['upstream_available'] and pin['upstream_version'] == '1.1.0-beta.54+1.21.1'
    selected, publisher = row['selected_current_pin'], row['native_publisher']
    assert selected['sha512'] == publisher['sha512'] == pin['upstream_sha512']
    assert selected['filename'] == publisher['filename']
    assert publisher in reviewed['publishers']
    source = Path(selected['path'])
    assert source.resolve().is_relative_to(Path(root).resolve())
    assert hashlib.sha256(source.read_bytes()).hexdigest() == publisher['sha256']
    publisher_classes = [name.removesuffix('.class').replace('/', '.')
                         for node in publisher['class_locations'] for name in node['classes']]
    assert set(reviewed['native_classes']) <= set(publisher_classes)
    locations = matches(index(source, selected['sha512'], pin['loader']), publisher_classes)
    assert locations == selected['class_locations'] == publisher['class_locations']
    assert locations and all(not v['embedded_chain'] for v in locations), 'Selected publisher must own the root classes'
    assert all({'id': 'accessories', 'version': pin['upstream_version']} in v['declared_mods'] for v in locations)
    assert row['qualified_primary_runs'], 'No actual selected-publisher execution'
    for run in row['qualified_primary_runs']:
        seal = json.loads(checked(root, run['seal']).read_text())
        verify(root, seal, seal['subjects'])
        assert seal['run'] == run['run'] and seal['target'] == pair[0]
        assert seal['summary'] == run['summary'] and seal['staged'] == run['staged']
        staged = json.loads(checked(root, run['staged']).read_text())
        assert staged['minecraft'] + '/' + staged['loader'] == pair[0]
        assert {'core', 'compatibility'} <= {v['family'] for v in staged['artifacts']}
        if 'siu_loaded' in staged:
            assert staged['siu_loaded'] is True
        owners = [v for v in staged['artifacts'] if v.get('family') == 'upstream'
                  and v['filename'] == selected['filename']]
        assert len(owners) == 1 and owners[0]['sha256'] == publisher['sha256']
        assert Path(owners[0]['file']).resolve() == source.resolve()
        if 'sha512' in owners[0]:
            assert owners[0]['sha512'] == selected['sha512']
        assert owners[0]['filename'] == selected['filename']
        assert checked(root, run['publisher_source']) == source.resolve()
        assert run['runtime_copy'] in seal['runtime_copies']
        assert run['runtime_copy']['sha256'] == publisher['sha256']
        pattern = (r'(?m)^\s*- accessories ' + re.escape(pin['upstream_version']) + r'\s*$'
                   if pin['loader'] == 'fabric' else
                   r'(?m)^\s*Accessories ' + re.escape(pin['upstream_version']) + r' \(accessories\)\s*$')
        assert run['loader_pattern'] == pattern
        assert re.search(pattern, checked(root, run['loader_log']).read_text(errors='replace'))
    supplemental = receipt['supplemental_fabric_beta48']
    seal = json.loads(checked(root, supplemental['seal']).read_text())
    verify(root, seal, seal['subjects'])
    assert seal['run'] == 'readiness-accessories-embedded48-1211-fabric-01' and seal['assertions'] == supplemental['assertions'] == 19
    limitation = receipt['neoforge_beta48']
    assert limitation['functional_acceptance'] is False
    failed = json.loads(checked(root, limitation['native_failed_attempt']).read_text())
    assert failed['status'] == 'failed'
    control = json.loads(checked(root, limitation['native_only_control']).read_text())
    assert control['same_failure_without_SIU'] is True
    return [selected]


@lru_cache(maxsize=1)
def receipt_for(root):
    return json.loads(checked(root, RECEIPT).read_text())


def select_current_pin(root, reviewed, catalogue):
    if (reviewed['target'], reviewed['case']) not in PAIRS:
        return None
    selected = validate_selection(root, reviewed, catalogue, receipt_for(str(Path(root).resolve())))
    return dict(pins=selected, evidence=RECEIPT,
                historical_catalogue_publisher_candidates=catalogue,
                scope='Advertised beta.54 actually loaded in the sealed functional execution. Historical nested/standalone candidates are not granted acceptance by class presence; supplemental beta.48 results and the native-only NeoForge startup limitation remain separate in the receipt.')
