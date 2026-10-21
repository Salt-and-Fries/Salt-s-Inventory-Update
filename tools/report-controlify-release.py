"""Aggregate a completed controller release matrix against the current production JARs."""
import argparse
from collections import Counter
from datetime import datetime, timezone
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import sys
import tomllib
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
FAMILIES = {'core': 'salts_inventory_update', 'compatibility': 'salts_inventory_compat',
            'controlify': 'salts_inventory_controlify'}
LOADERS = {'fabric': 'Fabric', 'forge': 'Forge', 'neoforge': 'NeoForge'}
ONBOARDING_CHECKS = {'first-world-alternative-host-installed', 'first-world-default-tutorial-not-opened',
                     'first-world-tutorial-preference-preserved',
                     'default-tutorial-still-opens-without-alternative-host',
                     'manual-help-still-opens-with-alternative-host'}


def read(path):
    return json.loads(Path(path).read_text(encoding='utf-8-sig'))


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def relative(path):
    return Path(path).resolve().relative_to(ROOT).as_posix()


def tool(filename):
    spec = importlib.util.spec_from_file_location(filename.replace('-', '_'), ROOT / 'tools' / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def inventory(folder, pins):
    runtime = tool('verify-controlify-runtime.py')
    properties = dict(re.findall(r'^(\w+)=(\S+)$', (ROOT / 'gradle.properties').read_text(), re.M))
    core_version = re.search(r'^version = "([^"]+)"$', (ROOT / 'build.gradle.kts').read_text(), re.M)[1]
    versions = {'core': core_version, 'compatibility': properties['compatibilityVersion'],
                'controlify': properties['controlifyVersion']}
    rows, hashes = [], {}
    for family, mod_id in FAMILIES.items():
        expected = set()
        sums = {}
        for line in (folder / family / 'SHA256SUMS.txt').read_text().splitlines():
            value, name = line.split('  ', 1)
            assert name not in sums, f'Duplicate checksum: {name}'
            sums[name] = value
        for pin in pins:
            game, loader = pin['minecraft'], pin['loader']
            core = properties.get('coreVersion' + game.replace('.', '_'), core_version)
            version = core if family == 'core' else versions[family]
            name = f'{mod_id}-{game}-{LOADERS[loader]}-{version}.jar'
            path = folder / family / name
            expected.add(name)
            value = digest(path)
            assert sums.get(name) == value, f'Incorrect checksum: {name}'
            with zipfile.ZipFile(path) as jar:
                assert jar.testzip() is None, name
                assert not any('/qa/' in p or 'GameplayHarness' in p for p in jar.namelist()), name
                if loader == 'fabric':
                    metadata = json.loads(jar.read('fabric.mod.json'))
                    assert metadata['id'] == mod_id and metadata['version'] == version, name
                else:
                    resource = 'META-INF/mods.toml' if loader == 'forge' else 'META-INF/neoforge.mods.toml'
                    metadata = tomllib.loads(jar.read(resource).decode())['mods']
                    assert len(metadata) == 1 and metadata[0]['modId'] == mod_id and metadata[0]['version'] == version, name
            if family != 'core':
                runtime.companion_versions(path, loader, core, version, mod_id)
            hashes[(game, loader, family)] = value
            rows.append({'family': family, 'minecraft': game, 'loader': loader, 'version': version,
                         'path': relative(path), 'sha256': value, 'bytes': path.stat().st_size})
        assert {p.name for p in (folder / family).glob('*.jar')} == expected, f'Stale or missing {family} archives'
        assert set(sums) == expected, f'Unexpected {family} checksums'
    bundle = folder / 'salts-inventory-all-latest.zip'
    with zipfile.ZipFile(bundle) as archive:
        names = [name for name in archive.namelist() if name.endswith('.jar')]
        assert len(names) == len(set(names)) == len(rows), 'Unexpected or duplicate bundled JAR entries'
        bundled = {name: hashlib.sha256(archive.read(name)).hexdigest()
                   for name in names}
        assert Counter(bundled.values()) == Counter(row['sha256'] for row in rows), 'Bundle differs from dist'
    return rows, hashes, versions


def junit():
    rows = []
    games = sorted({pin['minecraft'] for pin in read(ROOT / 'controlify/targets.json')['targets']})
    folders = [ROOT / family / game / 'fabric/build/test-results/test'
               for family in ('versions', 'compatibility/versions') for game in games]
    folders.append(ROOT / 'controlify/model/build/test-results/test')
    for folder in folders:
        files = sorted(folder.glob('TEST-*.xml'))
        assert files, f'Missing JUnit results: {folder}'
        for path in files:
            suite = ET.parse(path).getroot()
            row = {key: int(suite.get(key, '0')) for key in ('tests', 'failures', 'errors', 'skipped')}
            assert row['tests'] and not any(row[key] for key in ('failures', 'errors', 'skipped')), str(path)
            rows.append({'report': relative(path), **row})
    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--matrix', required=True, type=Path)
    parser.add_argument('--build-log', required=True, type=Path)
    parser.add_argument('--artifacts', default=ROOT / 'dist', type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--summary', required=True, type=Path)
    args = parser.parse_args()
    pins = read(ROOT / 'controlify/targets.json')['targets']
    manifest = read(args.matrix)
    cases = manifest['profiles']
    assert manifest['phase'] == 'all' and len(cases) == 42 and len({case['key'] for case in cases}) == 42
    assert Counter(case['phase'] for case in cases) == {'baseline': 12, 'focused': 26, 'mods': 4}
    assert {(case['minecraft'], case['loader']) for case in cases if case['phase'] == 'baseline'} == {
        (pin['minecraft'], pin['loader']) for pin in pins}
    assert 'BUILD SUCCESSFUL' in args.build_log.read_text(encoding='utf-8-sig'), 'Build did not succeed'
    artifacts, hashes, versions = inventory(args.artifacts.resolve(), pins)
    profiles = []
    for case in cases:
        assert case['status'] in ('PASS', 'PREVIOUS_PASS'), f"Incomplete profile: {case['key']}"
        evidence = ROOT / 'build/controlify-qa' / case['key'] / 'results.json'
        raw = read(evidence)
        staging = read(evidence.parent / 'staged-artifacts.json')
        assert all(staging[key] == case[key] for key in ('minecraft', 'loader', 'scope')), case['key']
        # Legacy reports record scope in the launcher inventory; modern reports also repeat it.
        assert raw.get('scope', staging['scope']) == case['scope'], f"Mismatched recorded scope: {case['key']}"
        rows = raw['results']
        assert raw['failures'] == 0 and not any(row['status'] == 'FAIL' for row in rows), str(evidence)
        checks = {row['name'] for row in rows if row['status'] == 'PASS'}
        if case['phase'] == 'baseline':
            assert ONBOARDING_CHECKS <= checks, f"Missing onboarding regression coverage: {case['key']}"
        summary = read(case['summary'])['targets']
        assert len(summary) == 1, case['key']
        validated = summary[0]
        assert validated['failures'] == 0 and validated['passes'] == sum(row['status'] == 'PASS' for row in rows)
        for family in FAMILIES:
            assert validated['artifact_sha256'][family] == hashes[(case['minecraft'], case['loader'], family)], case['key']
            assert validated['artifact_lineage'][family]['mode'] == 'exact', case['key']
        captures = [row for row in rows if row['status'] == 'CAPTURED']
        assert validated['screenshots'] == len(captures) and captures, case['key']
        profiles.append({key: case[key] for key in ('key', 'minecraft', 'loader', 'scope', 'browser', 'phase')} |
                        {'report': relative(evidence), 'verification': relative(case['summary']),
                         'checks': len(checks), 'passes': validated['passes'], 'screenshots': len(captures),
                         'elapsed_ms': raw['elapsedMs']})
    suites = junit()
    totals = {key: sum(row[key] for row in profiles) for key in ('checks', 'passes', 'screenshots', 'elapsed_ms')}
    summary = {'generated_at': datetime.now(timezone.utc).isoformat(), 'status': 'passed', 'versions': versions,
               'build_log': relative(args.build_log), 'matrix': relative(args.matrix), 'artifacts': artifacts,
               'profiles': profiles, 'totals': totals, 'junit': suites}
    args.summary.parent.mkdir(parents=True, exist_ok=True)
    args.summary.write_text(json.dumps(summary, indent=2) + '\n', encoding='utf-8')
    lines = ['# Controlify 0.1.1 release validation', '',
             f"Core **{versions['core']}**, Compatibility **{versions['compatibility']}**, Controlify **{versions['controlify']}**.", '',
             'All twelve Minecraft/loader targets have matching independently packaged release JARs. '
             'The root build passed. All 42 packaged gameplay profiles passed against the exact current production inputs, '
             'with no failed scenario records in the accepted reports.', '',
             f"The profiles contain **{totals['checks']:,} distinct profile/check pairs**, "
             f"**{totals['passes']:,} PASS records** and **{totals['screenshots']} native screenshots**. "
             'A check exercised in multiple profiles is counted once in each profile.', '',
             '## Fixes and release behavior', '',
             '- Core suppresses automatic regular-window help while a public alternative workspace host is registered. '
             'Fresh-world tests run before the synthetic controller connects, preserve the unseen preference, '
             'check automatic help when the host is temporarily unregistered, and check manual help after restoring it.',
             '- Both companions declare the matching Core patch. Compatibility and Controlify keep independent release labels. '
             'The old Minecraft 1.20.1 exact-version conflict is removed.',
             '- Modern visual probes measure the real icon/native-pane crossfade. They retain texture, geometry, '
             'native item and settled-pane checks without rejecting an intended transition.',
             '- Updated focused native tests require live ingredient-state refresh, stable membership while the wheel '
             'remains open, and newly eligible recipes after reopening. This preserves the existing documented '
             'selection-stability behavior; the older wait incorrectly expected immediate membership changes.', '',
             '## Executed gameplay profiles', '',
             '| Minecraft | Loader | Scope | Optional browser | Distinct checks | Screenshots |',
             '| --- | --- | --- | --- | ---: | ---: |']
    for row in profiles:
        lines.append(f"| {row['minecraft']} | {LOADERS[row['loader']]} | {row['scope']} | "
                     f"{row['browser'] or '—'} | {row['checks']} | {row['screenshots']} |")
    lines += ['', 'The twelve baselines cover legacy ported features and modern full recorded suites. '
              'Focused profiles cover legacy adapters, public APIs, wheel presentation, playtest regressions, '
              'the status tray, crafting behavior and wheel stability. The four 26.3 optional-mod profiles '
              'exercise JEI/REI alongside available Tom’s Storage, AE2 and Sophisticated adapters; '
              'the loader-specific report lists the actual installed targets.', '',
              '## Build and artifact verification', '',
              f"The root build gates passed. The retained JUnit XML contains {sum(row['tests'] for row in suites):,} tests "
              'with zero failures, errors or skips; Gradle may reuse unchanged test tasks. '
              'The release-version Python regression covers independently labeled companions and rejects incorrect Core pins.', '',
              'All 36 production JARs have the correct filenames, embedded mod versions and matching Core dependencies. '
              'Module checksums and the complete bundle match the final bytes. Distribution folders contain no stale JARs. '
              'QA fixtures remain outside production archives. The root gates check API-only compiler/bytecode boundaries '
              'and native Controlify/Minecraft symbols.', '',
              '- `dist/core/`: twelve Core JARs.', '- `dist/compatibility/`: twelve Compatibility JARs.',
              '- `dist/controlify/`: twelve Controlify JARs.', '- `dist/salts-inventory-all-latest.zip`: all 36 production JARs.', '',
              '## Evidence and reproduction', '',
              f"- Matrix: `{relative(args.matrix)}`.", f"- Root build: `{relative(args.build_log)}`.",
              f"- Machine-readable totals, artifact hashes and per-profile report links: `{relative(args.summary)}`.",
              '- Original failed attempts are retained under the matrix’s `attempts/` directories. '
              'Only successfully rerun, strictly verified reports contribute to these totals.', '',
              'Run `powershell -NoProfile -ExecutionPolicy Bypass -File tools/Run-ControlifyReleaseQa.ps1 '
              '-Phase all -RunId controlify011 -Resume` after the root build. Resume verifies existing reports '
              'against current artifacts and reruns any stale or failing profiles.', '',
              '## Coverage limits', '',
              'These are real rendered Minecraft clients and integrated-server worlds launched through development tooling. '
              'Original production JAR inputs, loaded code sources, pinned upstream dependencies and native remap lineage '
              'are verified; legacy loaders create the recorded named-namespace runtime derivatives. '
              'Native controller events are synthetic. Physical controller hardware, installed-launcher profiles, '
              'dedicated/remote multiplayer servers and arbitrary modpacks remain unverified.', '',
              'Unregistering a workspace host verifies Core’s default onboarding path while the companion is still loaded; '
              'it is not a separate Core-only installation test. The optional-mod profiles cover the listed 26.3 combinations, '
              'not every upstream mod version on every target. Passing this matrix does not establish that the mod is bug free.', '']
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text('\n'.join(lines), encoding='utf-8')
    print(f"Verified release report: {len(profiles)} profiles, {totals['checks']} profile/check pairs, {totals['screenshots']} screenshots")


if __name__ == '__main__':
    main()
