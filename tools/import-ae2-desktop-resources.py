"""Import only the desktop integration's resources from a read-only AE2 fork.

Native AE2 screens, textures, menu classes and network implementations are not
copied. The resource manifest lets tests check that the fork's geometry has
survived packaging unchanged.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
BASE = 'de2b845e9'
HEAD = 'c9b815ec0'
STYLES = 'src/main/resources/assets/ae2/screens/desktop/'
LANGUAGE = 'src/generated/resources/assets/ae2/lang/en_us.json'


def git(reference, *args):
    return subprocess.check_output(['git', '--no-optional-locks', '-C', str(reference), *args])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reference', type=Path, required=True)
    args = parser.parse_args()
    reference = args.reference.resolve(strict=True)
    if reference == ROOT.resolve() or ROOT.resolve() in reference.parents:
        raise SystemExit('The reference must be a separate repository')
    if git(reference, 'rev-parse', 'HEAD') != git(reference, 'rev-parse', HEAD):
        raise SystemExit('Reference HEAD does not match the reviewed integration commit')
    if git(reference, 'status', '--porcelain').strip():
        raise SystemExit('Reference repository has uncommitted changes')
    resources = ROOT / 'compatibility/ae2/src/main/resources'
    changed = git(reference, 'diff', '--name-status', BASE, HEAD).decode().splitlines()
    style_files = [line.split('\t')[1] for line in changed
                   if line.startswith('A\t' + STYLES) and line.endswith('.json')]
    if len(style_files) != 42:
        raise SystemExit(f'Expected 42 reviewed desktop styles, found {len(style_files)}')
    manifest = {'reference': 'https://github.com/Salt-and-Fries/Applied-Energistics-2-Salts-inventory-api-test',
                'commit': git(reference, 'rev-parse', HEAD).decode().strip(),
                'upstreamCommit': git(reference, 'rev-parse', BASE).decode().strip(),
                'styles': {}}
    for source_path in sorted(style_files):
        relative = source_path.removeprefix('src/main/resources/')
        data = git(reference, 'show', f'{HEAD}:{source_path}')
        json.loads(data)  # Refuse to package an invalid document.
        target = resources / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        manifest['styles'][relative] = hashlib.sha256(data).hexdigest()
    original = json.loads(git(reference, 'show', f'{BASE}:{LANGUAGE}'))
    integrated = json.loads(git(reference, 'show', f'{HEAD}:{LANGUAGE}'))
    additions = {key: value for key, value in integrated.items() if key not in original}
    modified = [key for key in original if key in integrated and original[key] != integrated[key]]
    if modified:
        raise SystemExit(f'Integration modifies native translation keys: {modified}')
    language_path = resources / 'assets/ae2/lang/en_us.json'
    language_path.parent.mkdir(parents=True, exist_ok=True)
    language_path.write_text(json.dumps(additions, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    manifest['translationKeys'] = sorted(additions)
    manifest['translationSha256'] = hashlib.sha256(language_path.read_bytes()).hexdigest()
    notices = resources / 'META-INF'
    notices.mkdir(parents=True, exist_ok=True)
    (notices / 'ae2-desktop-reference.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    (notices / 'LICENSE_ae2_desktop').write_bytes(git(reference, 'show', f'{HEAD}:LICENSE'))
    (notices / 'NOTICE_ae2_desktop.txt').write_text(
        "AE2 desktop compatibility additions originate in Salt's AE2 API test fork.\n"
        + manifest['reference'] + '/tree/' + manifest['commit'] + '\n'
        + 'Adapted additions are distributed under LGPL-3.0-or-later.\n'
        + 'Corresponding adapted source is in compatibility/ae2 in the Salt inventory repository.\n'
        + 'Native AE2 implementation classes and textures are supplied by the separate AE2 mod.\n',
        encoding='utf-8')
    print(f'Imported {len(style_files)} exact desktop styles and {len(additions)} new translation keys')


if __name__ == '__main__':
    main()
