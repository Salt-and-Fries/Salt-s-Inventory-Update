"""Reuse retained read-only Mine Cells observations on the current observer.

Only the retained literal Java observation helpers are imported, through AST.
The old preparers are never executed and neither publisher nor SIU is patched.
The same disposable openUnlocked preference supports the retained drag checks;
default lock behavior belongs to the separate Core acceptance.
"""
from pathlib import Path
import ast

ROOT = Path(__file__).resolve().parents[2]
INPUTS = [ROOT / 'functional-tests/minecells-research/prepare.py',
          ROOT / 'functional-tests/minecells-implemented/prepare.py']


def literal(path, name):
    values = [node.value.value for node in ast.walk(ast.parse(path.read_text()))
              if isinstance(node, ast.Assign) and isinstance(node.value, ast.Constant)
              and isinstance(node.value.value, str)
              and any(isinstance(t, ast.Name) and t.id == name for t in node.targets)]
    assert len(values) == 1, (path, name)
    return values[0]


def enhance(text, mc):
    assert mc in ['1.20.1', '1.21.1']
    helpers = literal(INPUTS[0], 'helpers')
    observe = literal(INPUTS[1], 'observe')
    command = literal(INPUTS[1], 'command')
    # The old resource-pack caption command is unrelated to this acceptance.
    command = command[command.index('case "minecells-server"'):]
    if mc == '1.21.1':
        command = command.replace('.save(new net.minecraft.nbt.CompoundTag())',
                                  '.saveOptional(player().registryAccess())')
    for anchor, replacement in [
        ('config.getClass().getField("enableMod").setBoolean(config,true);',
         'config.getClass().getField("enableMod").setBoolean(config,true);'
         'config.getClass().getField("openUnlocked").setBoolean(config,true);'),
        ('static boolean coreMode()', helpers + '\n    static boolean coreMode()'),
        ('Object raw=coreCall(window,"containerMenu");if(raw instanceof AbstractContainerMenu menu){',
         'Object raw=coreCall(window,"containerMenu");if(raw instanceof AbstractContainerMenu menu){' + observe),
        ('case "snapshot-server"', command + '\n            case "snapshot-server"'),
    ]:
        assert text.count(anchor) == 1, ('Unreviewed observer shape', anchor, text.count(anchor))
        text = text.replace(anchor, replacement, 1)
    return text
