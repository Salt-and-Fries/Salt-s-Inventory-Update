"""Check every installed AE2 15 screen factory has its desktop resource."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
resources = [ROOT / 'compatibility/ae2/src/main/resources',
             ROOT / 'compatibility/ae2/versions/1.20.1/common/src/main/resources']
for loader in ['fabric', 'forge']:
    inventory = ROOT / f'build/ae2-port/patch-tools/1.20.1-{loader}-named/appeng/init/client/InitScreens.json'
    native = json.loads(inventory.read_text())
    paths = {node['constant']['value'] for method in native['methods'] for node in method['instructions']
             if node.get('constant', {}).get('kind') == 'string'
             and node['constant']['value'].startswith('/screens/')
             and node['constant']['value'].endswith('.json')}
    for path in paths:
        desktop = 'assets/ae2/screens/desktop/' + path.removeprefix('/screens/')
        assert any((root / desktop).is_file() for root in resources), f'{loader}: missing {desktop}'
    print(f'{loader}: all {len(paths)} native screen factory resources covered')
