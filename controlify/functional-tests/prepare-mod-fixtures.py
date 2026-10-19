"""Download the pinned, unmodified optional mods for 26.3 packaged controller QA."""
import argparse
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--loader', choices=['fabric', 'neoforge', 'all'], default='all')
parser.add_argument('--browser', choices=['jei', 'rei', 'all'], default='all')
parser.add_argument('--output', type=Path, default=ROOT / 'build/controlify-qa/fixtures')
args = parser.parse_args()
pins = json.loads(Path(__file__).with_name('mod-fixtures-26.3.json').read_text(encoding='utf8'))
for profile, entries in pins['profiles'].items():
    loader, browser = profile.split('-')
    if args.loader not in ['all', loader] or args.browser not in ['all', browser]:
        continue
    folder = args.output.resolve() / profile
    folder.mkdir(parents=True, exist_ok=True)
    staged = []
    for entry in entries:
        file = entry['file']
        destination = folder / file['filename']
        if not destination.is_file() or hashlib.sha512(destination.read_bytes()).hexdigest() != file['hashes']['sha512']:
            request = urllib.request.Request(file['url'], headers={'User-Agent': 'SaltsInventoryUpdate-compat-validation/1.0'})
            with urllib.request.urlopen(request, timeout=90) as response:
                content = response.read()
            if hashlib.sha512(content).hexdigest() != file['hashes']['sha512']:
                raise RuntimeError('Optional mod hash differs from pinned upstream file: ' + file['filename'])
            destination.write_bytes(content)
        staged.append({**entry, 'path': str(destination)})
    (folder / 'manifest.json').write_text(json.dumps({'minecraft': '26.3', 'loader': loader,
        'browser': browser, 'mods': staged}, indent=2) + '\n', encoding='utf8')
    print(profile + ': ' + ', '.join(entry['file']['filename'] for entry in staged))
