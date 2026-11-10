"""Replay the retained crafter actions against fresh exact release subjects."""
from pathlib import Path
import hashlib, json, sys, zipfile
import nbtlib

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-ten'))
from send import send


def native_stack(text, mc):
    value = nbtlib.parse_nbt(text)
    assert isinstance(value, nbtlib.Compound)
    if not value: return ('minecraft:air', 0)
    assert isinstance(value.get('id'), nbtlib.String), value
    key, count_type = ('Count', nbtlib.Byte) if mc == '1.20.1' else ('count', nbtlib.Int)
    assert isinstance(value.get(key), count_type), value
    item, count = str(value['id']), int(value[key])
    assert count >= 0 and (count > 0 or item == 'minecraft:air'), value
    return item, count


def recipe_contract(profile, mc):
    records = json.loads((profile / 'staged-artifacts.json').read_text())['artifacts']
    publisher = next(v for v in records if v['filename'].startswith('minecells-'))
    jar = Path(publisher['file']); assert hashlib.sha256(jar.read_bytes()).hexdigest() == publisher['sha256']
    with zipfile.ZipFile(jar) as archive:
        recipes = {name: json.loads(archive.read(name)) for name in archive.namelist()
                   if name.startswith('data/') and ('/recipe/' in name or '/recipes/' in name) and name.endswith('.json')}
    recipes = {name: value for name, value in recipes.items() if value.get('type') == 'minecells:cell_forge_recipe'}
    assert len(recipes) == (79 if mc == '1.20.1' else 80), 'Pinned native recipe inventory changed'
    blade = next(v for k, v in recipes.items() if k.endswith('/cell_crafter/gear/balanced_blade.json'))
    assert blade['input'] == {'minecraft:stick': 1, 'minecells:cell_infused_steel': 6,
                              'minecraft:lapis_lazuli': 4, 'minecraft:emerald': 2}
    assert blade['output'] == {'id': 'minecells:balanced_blade', 'Count' if mc == '1.20.1' else 'count': 1}
    return dict(publisher=publisher, native_recipe_count=len(recipes), selected_recipe=blade)


def run(profile, mc, loader, *args, **kwargs):
    assert mc in ['1.20.1', '1.21.1'] and loader == 'fabric'
    profile = Path(profile)
    out = profile / 'minecells-release'; out.mkdir()
    original = ROOT / 'functional-tests/minecells-implemented/check.py'
    source = original.read_text()
    anchor = 'state={};checks=[]'
    assert source.count(anchor) == 1
    contract = recipe_contract(profile, mc)
    (out / 'native-recipe-contract.json').write_text(json.dumps(contract, indent=2) + '\n')
    # Preserve the retained actions and conditions, adapting only the renamed
    # observation key and the publisher's actual recipe inventory/stack codec.
    source = source[source.index(anchor):]
    assert source.count("window()['template']") == 1
    source = source.replace("window()['template']", "window().get('templateClass',window().get('template'))")
    replacements = {
        "vals()['allRecipes']==79": "vals()['allRecipes']==expected_recipe_count",
        "if 'minecraft:air' not in v['item']": "if native_stack(v['item'])[1] > 0",
        "sum('minecells:balanced_blade' in s and 'Count:1b' in s for s in after['drops'])==1":
            "len(after['drops'])==1 and native_stack(after['drops'][0])==('minecells:balanced_blade',1)",
        "any('minecells:balanced_blade' in d for d in after['drops'])":
            "len(after['drops'])==1 and native_stack(after['drops'][0])==('minecells:balanced_blade',1)",
    }
    for before, after in replacements.items():
        assert source.count(before) == 1, before
        source = source.replace(before, after, 1)
    generated = out / 'replayed-check.py'; generated.write_text(source)
    scope = ('Native Mine Cells cell/unbreakable crafter on the exact pinned publisher: recipe categories/search, '
             'scroll, modal ownership, missing-material refusal, exact native recipe '
             'consumption/output, movement/GUI scale and close, with the disposable '
             'openUnlocked preference. Not default-lock behavior, every weapon or progression path.')
    def input_send(output, kind, **kw):
        # The current production observer's move command also updates the
        # render-time pointer position, as the retained pointer command did.
        return send(output, 'move' if kind == 'pointer' else kind, **kw)
    namespace = dict(ROOT=ROOT, run=profile, out=out, send=input_send, sys=sys, json=json,
                     time=__import__('time'), __name__='minecells_focused_replay',
                     expected_recipe_count=contract['native_recipe_count'], native_stack=lambda s: native_stack(s, mc))
    exec(compile(source, str(generated), 'exec'), namespace)
    journal = out / 'focused-checks.json'; data = json.loads(journal.read_text())
    assert len(data['checks']) >= 25 and all(c['passed'] for c in data['checks'])
    data.update(complete=True, scope=scope, retained_driver_sha256=hashlib.sha256(original.read_bytes()).hexdigest(),
                adapted_driver_sha256=hashlib.sha256(generated.read_bytes()).hexdigest())
    journal.write_text(json.dumps(data, indent=2) + '\n')
    return dict(cases=2, failed=0, checks=len(data['checks']), minecells_features_checked=True, scope=scope)
