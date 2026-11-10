"""Retained read-only Prominent component/widget observations, current backend."""
from pathlib import Path
from minecells_observer import literal

ROOT = Path(__file__).resolve().parents[2]
INPUTS = [ROOT / 'functional-tests/prominent-forge/prepare.py',
          ROOT / 'functional-tests/prominent-shop/prepare.py']


def enhance(text, mc):
    assert mc == '1.20.1'
    anchor = 'Object raw=coreCall(window,"containerMenu");if(raw instanceof AbstractContainerMenu menu){'
    assert text.count(anchor) == 1
    text = text.replace(anchor, anchor + literal(INPUTS[0], 'observe') + literal(INPUTS[1], 'observer'), 1)
    anchor = 'static boolean coreMode()'; assert text.count(anchor) == 1
    text = text.replace(anchor, literal(INPUTS[1], 'methods') + '\n    ' + anchor, 1)
    anchor = 'case "snapshot-server"'; assert text.count(anchor) == 1
    text = text.replace(anchor, literal(INPUTS[0], 'command') + literal(INPUTS[1], 'command') + '\n' + anchor, 1)
    # The current production observer's move command updates both the render
    # mouse position and screen events; do not inject old obfuscated fields.
    assert 'case "move"' in text and 'RuntimeNames.field(mc.mouseHandler.getClass(),"xpos")' in text
    return text
