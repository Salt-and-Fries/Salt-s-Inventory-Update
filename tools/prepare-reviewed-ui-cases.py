"""Recover real opening workflows for the approved designs; never force a provider."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
read = lambda p: json.loads((ROOT / p).read_text(encoding='utf-8-sig'))
plan = read('output/project-everything-56-design/native-reference-plan.json')
coverage = {r['id']: r for r in read('output/remaining-ui-audit/coverage.json')['cases']}
cases, missing = [], []
for c in plan['cases']:
    if c['minecraft'] != '1.20.1':
        continue
    actions = None
    run = Path(c['native']['capture']).parent.parent
    sources = [Path(c['native_observation_file'])]
    sources.extend(p.relative_to(ROOT) for p in (ROOT / run).glob('*.json')
                   if p.name.startswith(('audit-', 'native-')))
    reference = coverage.get(c['id'], {})
    if reference.get('source_manifest'):
        sources.append(Path(reference['source_manifest']))
    # Later aligned captures share the same physical opening setup as their parent.
    aliases = {c['id'], c['id'].removesuffix('-aligned'), c['id'].replace('-real-aligned', '-real')}
    for source in sources:
        if not (ROOT / source).exists():
            continue
        data = read(source)
        rows = data if isinstance(data, list) else data.get('cases', [])
        row = next((r for r in rows if r.get('id') in aliases), None)
        if not row:
            continue
        if row.get('actions'):
            actions = [v['request'] for v in row['actions']]
        elif row.get('native_actions'):
            actions = row['native_actions']
        if actions is not None:
            break
    if actions is None:
        if (ROOT / run / 'observations.json').exists():
            rows = read(run / 'observations.json')
            indices = [i for i, r in enumerate(rows)
                       if r.get('command', {}).get('capture') in {'native-' + c['id'], Path(c['native']['capture']).stem}]
            if indices:
                end = indices[-1]
                starts = [i for i in range(end) if rows[i].get('command', {}).get('type') == 'fixture']
                if starts:
                    actions = [{k: v for k, v in r['command'].items() if k != 'sequence'}
                               for r in rows[starts[-1]:end]
                               if r.get('command', {}).get('type') not in ('capture', 'close')]
    if actions is None:
        missing.append(c['id'])
        continue
    cases.append(dict(id=c['id'], family=c['family'], loader=c['loader'], entry=c['entry'],
                      native_actions=actions, core_actions=actions))
out = ROOT / 'build/project-everything-56-implementation/reference-actions.json'
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(cases, indent=2) + '\n', encoding='utf-8')
print(json.dumps(dict(recovered=len(cases), missing=missing)))
