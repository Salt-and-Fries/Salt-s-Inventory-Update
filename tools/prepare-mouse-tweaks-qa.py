"""Fetch checksum-pinned, official Mouse Tweaks fixtures for focused Core QA."""
from pathlib import Path
import argparse, hashlib, json, urllib.request

ROOT = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--minecraft')
    parser.add_argument('--loader')
    args = parser.parse_args()
    targets = json.loads((ROOT / 'gradle/mouse-tweaks-targets.json').read_text())['targets']
    selected = [t for t in targets if (not args.minecraft or t['minecraft'] == args.minecraft)
                and (not args.loader or t['loader'] == args.loader)]
    if not selected:
        parser.error('No matching supported target')
    directory = ROOT / 'build/mouse-tweaks/fixtures'
    directory.mkdir(parents=True, exist_ok=True)
    for target in selected:
        filename = target['url'].rsplit('/', 1)[1]
        path = directory / filename
        if not path.is_file() or hashlib.sha512(path.read_bytes()).hexdigest() != target['sha512']:
            request = urllib.request.Request(target['url'], headers={'User-Agent': 'SaltsInventoryUpdate/MouseTweaks-QA'})
            with urllib.request.urlopen(request, timeout=30) as response:
                data = response.read()
            if hashlib.sha512(data).hexdigest() != target['sha512']:
                raise RuntimeError('Checksum mismatch: ' + filename)
            path.write_bytes(data)
        print('Verified', target['minecraft'], target['loader'], filename)
    # Only downloaded and verified targets enter the local launcher manifest.
    manifest = []
    for target in targets:
        path = directory / target['url'].rsplit('/', 1)[1]
        if path.is_file() and hashlib.sha512(path.read_bytes()).hexdigest() == target['sha512']:
            manifest.append(dict(target, file=str(path.relative_to(ROOT))))
    (ROOT / 'build/mouse-tweaks/fixtures.json').write_text(json.dumps(manifest, indent=2) + '\n')

if __name__ == '__main__':
    main()
