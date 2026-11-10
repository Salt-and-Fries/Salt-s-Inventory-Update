"""Record observed menu registry identities; class names and mod IDs are not registry IDs."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
plan=json.loads((ROOT/'output/project-everything-56-design/native-reference-plan.json').read_text())
expected={r['id']:r['native_state']['screen'] for r in plan['cases']}
found={}
for run in (ROOT/'build/remaining-ui-audit/gameplay').glob('1.20.1-*-integrated-reviewed-*'):
    for path in run.glob('*.json'):
        if 'records' not in path.name and not path.name.startswith('audit-'):continue
        try:rows=json.loads(path.read_text(encoding='utf-8-sig'))
        except (ValueError,OSError):continue
        if not isinstance(rows,list):continue
        for r in rows:
            state=(r.get('observation')or{}).get('client')or{}
            sources=[(w.get('nativeScreen'),w.get('menuId')) for w in state.get('allWindows',[])]
            if not state.get('allWindows'):
                sources.append((state.get('screen'),state.get('menuId')))
            elif r.get('id') in expected:
                sources.append((expected[r['id']],state.get('menuId')))
            for screen,menu in sources:
                if not screen or not menu or screen in ('none','com.salts_inventory_update.client.InventoryDesktopScreen'):continue
                found.setdefault(screen,{})[menu]=str(path.relative_to(ROOT))
out=ROOT/'output/project-everything-56-implementation/observed-menu-ids.json'
out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(found,indent=2)+'\n',encoding='utf-8')
print(f'{len(found)} screen classes with observed menu IDs')
