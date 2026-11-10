"""Focused real-menu checks after the Owo capture clipping fixes."""
from pathlib import Path
import argparse
import json

from evidence import capture, send

p = argparse.ArgumentParser()
p.add_argument('run', type=Path)
p.add_argument('--minecraft', required=True)
a = p.parse_args()
checks = []


def act(kind, **kw):
    return send(a.run, kind, **kw)['client']


def tap(x, y):
    act('move', x=x, y=y, ticks=2)
    act('click', x=x, y=y, ticks=1)
    return act('release', x=x, y=y, ticks=8)


def window(state):
    return next(w for w in state['windows'] if w.get('menuId') == 'minecells:cell_forge')


def check(name, passed, detail=None):
    checks.append({'name': name, 'passed': bool(passed), 'observed': detail})
    (a.run / 'minecells-focused-rendering-checks.json').write_text(json.dumps(checks, indent=2))
    assert passed, (name, detail)
    print('PASS', name, flush=True)


for block, ui in [('minecells:cell_crafter', 'minecells-crafter'),
                  ('minecells:unbreakable_cell_crafter', 'minecells-unbreakable-crafter')]:
    act('close', ticks=3)
    act('fixture', block=block, ticks=8)
    act('command', command='gamemode survival @a', ticks=3)
    s = act('open', ticks=20)
    w = window(s)
    check(ui + ': original crafting mode opens',
          w['nativeValues'].get('isRecipeListVisible') is False, w['nativeValues'])
    check(ui + ': integrated native crafting fragment retained',
          any(r['id'] == 'native-crafting' for r in w['template']['regions']))
    act('move', x=620, y=400, ticks=3)
    capture(a.run, 'siu', a.minecraft, 'fabric', ui,
            'Original Cell Crafter entry with the current clipping adapter; recipe-browser search and return were checked separately.')
    b = w['bounds']
    s = tap(b['x'] + 41, b['y'] + 58)
    w = window(s)
    check(ui + ': native recipe browser opens',
          w['nativeValues'].get('isRecipeListVisible') is True, w['nativeValues'])
    b = w['bounds']
    s = tap(b['x'] + 150, b['y'] + 57)
    s = act('text', text='balanced', ticks=8)
    values = window(s)['nativeValues']
    check(ui + ': original search receives text', values.get('recipeSearch') == 'balanced', values)
    act('key', key=268, ticks=1)
    for _ in 'balanced':
        s = act('key', key=261, ticks=1)
    check(ui + ': native search clears', window(s)['nativeValues'].get('recipeSearch') == '', window(s)['nativeValues'])
    act('move', x=620, y=400, ticks=10)
    picker_ui = 'minecells-recipe-picker' if ui == 'minecells-crafter' else ui + '-recipe-picker'
    capture(a.run, 'siu', a.minecraft, 'fabric', picker_ui,
            'Complete original parchment with current Owo clipping; native search entry, clearing and Escape return checked on this publisher release.')
    s = act('key', key=256, ticks=8)
    check(ui + ': Escape returns to crafting',
          window(s)['nativeValues'].get('isRecipeListVisible') is False, window(s)['nativeValues'])

print('Focused checks complete:', len(checks), flush=True)
