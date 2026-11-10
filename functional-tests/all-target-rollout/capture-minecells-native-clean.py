"""Original Mine Cells reference pictures, with the pointer off the parchment."""
from pathlib import Path
import argparse
from evidence import capture, send

p = argparse.ArgumentParser()
p.add_argument('run', type=Path)
p.add_argument('--minecraft', required=True)
a = p.parse_args()


def act(kind, **kw):
    return send(a.run, kind, **kw)['client']


for block, ui in [('minecells:cell_crafter', 'minecells-crafter'),
                  ('minecells:unbreakable_cell_crafter', 'minecells-unbreakable-crafter')]:
    act('close', ticks=3)
    act('fixture', block=block, ticks=8)
    act('command', command='gamemode survival @a', ticks=3)
    s = act('open', ticks=20)
    assert s.get('menuClass', '').endswith('.CellCrafterScreenHandler'), s.get('menuClass')
    act('move', x=620, y=400, ticks=10)
    capture(a.run, 'native', a.minecraft, 'fabric', ui,
            'Original unchanged publisher menu opened normally. The pointer is outside the UI, keeping hover text from obscuring its artwork.')
    b = s['bounds']
    act('click', x=b['x']+41, y=b['y']+36, ticks=1)
    act('release', x=b['x']+41, y=b['y']+36, ticks=10)
    act('move', x=620, y=400, ticks=10)
    picker_ui = 'minecells-recipe-picker' if ui == 'minecells-crafter' else ui+'-recipe-picker'
    capture(a.run, 'native', a.minecraft, 'fabric', picker_ui,
            'Original unchanged parchment browser; the pointer is moved outside the UI so the complete native reference is unobscured.')

print('Four native reference pictures complete', flush=True)
