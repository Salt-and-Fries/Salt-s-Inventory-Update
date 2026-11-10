"""Run only the menus affected by the five reported artwork defects."""
from pathlib import Path
import argparse
import json
import subprocess
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
p = argparse.ArgumentParser()
p.add_argument('run', type=Path)
p.add_argument('--minecraft', required=True)
p.add_argument('--loader', required=True)
p.add_argument('--create', action='store_true')
p.add_argument('--alloy', action='store_true')
p.add_argument('--pattern-access', action='store_true', help='Also check the related optional part menu')
p.add_argument('--from-case', help='Resume after a corrected disposable driver, preserving earlier results')
a = p.parse_args()

steps = [
    ('drive', 'capture-ae2-block-batch.py', [str(a.run), '--side', 'siu', '--minecraft', a.minecraft,
                                         '--loader', a.loader, '--only', 'drive']),
    ('catalog-items', 'capture-ae2-legacy-workflows.py' if a.minecraft == '1.20.1' else 'capture-ae2-modern-items.py',
     [str(a.run), '--side', 'siu', '--loader', a.loader] +
     (['--only', 'portable-item-cell', '--only', 'portable-fluid-cell', '--only', 'wireless-terminal']
      if a.minecraft == '1.20.1' else ['--minecraft', a.minecraft])),
    ('crafting-plan', 'capture-ae2-legacy-crafting.py' if a.minecraft == '1.20.1' else 'capture-ae2-modern-crafting.py',
     [str(a.run), '--side', 'siu', '--loader', a.loader] +
     ([] if a.minecraft == '1.20.1' else ['--minecraft', a.minecraft]))
]
if a.pattern_access:
    steps.insert(2, ('pattern-access', 'capture-ae2-parts.py', [str(a.run), '--side', 'siu',
                 '--minecraft', a.minecraft, '--loader', a.loader, '--only', 'pattern_access_terminal']))
if a.create:
    steps.append(('ammo', 'capture-create-family.py', [str(a.run), '--side', 'siu', '--minecraft', a.minecraft,
                                                      '--loader', a.loader, '--only', 'ammo', '--only', 'creative-ammo']))
if a.alloy:
    steps.append(('alloy', 'capture-container-batch.py', ['--siu', str(a.run), '--native', str(a.run),
                                                       '--side', 'siu', '--minecraft', a.minecraft,
                                                       '--loader', a.loader, '--only', 'alloy-forge']))
results = []
if a.from_case:
    start = next(i for i, step in enumerate(steps) if step[0] == a.from_case)
    steps = steps[start:]
    previous = a.run / 'artifact-fix-case-results.json'
    if previous.exists():
        results = json.loads(previous.read_text())
for name, script, arguments in steps:
    log = a.run / ('artifact-fix-' + name + '.log')
    with log.open('w', encoding='utf-8') as stream:
        subprocess.run([sys.executable, '-X', 'utf8', str(HERE / script), *arguments], cwd=ROOT,
                       stdout=stream, stderr=subprocess.STDOUT, check=True)
    text = log.read_text()
    assert 'PENDING' not in text, (name, text)
    results.append({'case': name, 'status': 'pictured real entry; focused checks only', 'log': str(log)})
    (a.run / 'artifact-fix-case-results.json').write_text(json.dumps(results, indent=2))
    print('PASS', name, flush=True)
print('Affected menu capture checks complete', flush=True)
