"""Rehome only the fork's new desktop helpers; strip opt-in reference QA code."""
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
REFERENCE = ROOT.parent / 'Applied-Energistics-2-Salts-inventory-api-test'
SOURCE = ROOT / 'compatibility/ae2/versions/26.3/neoforge/src/main/java'


def strip_qa(source):
    # These blocks contain only reference-atlas instrumentation; production layout
    # and input code are outside them. Balance braces while ignoring Java strings.
    token = 'if (Boolean.getBoolean("ae2.desktop.qa")'
    while token in source:
        start = source.index(token)
        opening = source.index('{', start)
        depth, quoted, escaped = 0, None, False
        for end in range(opening, len(source)):
            char = source[end]
            if quoted:
                if escaped: escaped = False
                elif char == '\\': escaped = True
                elif char == quoted: quoted = None
            elif char in ['"', "'"]: quoted = char
            elif char == '{': depth += 1
            elif char == '}':
                depth -= 1
                if depth == 0: break
        source = source[:start] + source[end+1:]
    source = '\n'.join(line for line in source.splitlines() if 'DesktopQaHooks' not in line) + '\n'
    return source


def main():
    mappings = json.loads((ROOT/'build/ae2-port/native-deltas/manifest.json').read_text())['mappings']
    wanted = {
        'appeng/client/gui/style/DesktopFrameEdges', 'appeng/client/gui/style/DesktopLayoutStyle',
        'appeng/client/gui/me/networktool/ToolboxCompanionScreen',
        'appeng/client/integration/salts/AEWindow', 'appeng/client/integration/salts/AEManagementControls',
        'appeng/client/gui/DesktopContentActions',
        'appeng/core/network/clientbound/DesktopMenuUpdatePacket', 'appeng/core/network/clientbound/MenuOpeningDataPacket',
        'appeng/menu/MenuOpeningData',
    }
    for native in sorted(wanted):
        original = next(path for path in REFERENCE.glob('src/*/java/'+native+'.java'))
        source = strip_qa(original.read_text(encoding='utf-8'))
        source = re.sub(r'^package [\w.]+;', 'package '+mappings[native].rsplit('/',1)[0].replace('/','.')+';', source, count=1, flags=re.MULTILINE)
        for old,new in mappings.items():
            source = source.replace(old.replace('/','.'),new.replace('/','.'))
        for old,new in mappings.items():
            old_simple, new_simple = old.rsplit('/',1)[-1], new.rsplit('/',1)[-1]
            source = re.sub(r'\b'+old_simple+r'\b',new_simple,source)
        # New helpers that originally shared appeng packages now use explicit imports.
        if '/style/' in native:
            source = source.replace('\n\n', '\n\nimport appeng.client.gui.style.*;\n',1)
        if native.endswith('ToolboxCompanionScreen'):
            source = source.replace('\n\n','\n\nimport appeng.client.gui.AEBaseScreen;\nimport appeng.client.gui.style.StyleManager;\n',1)
        if native.endswith('DesktopContentActions'):
            source = source.replace('\n\n','\n\nimport appeng.client.gui.AEBaseScreen;\n',1)
        target = SOURCE/(mappings[native]+'.java')
        target.parent.mkdir(parents=True,exist_ok=True)
        target.write_text(source,encoding='utf-8')
        print(target.relative_to(ROOT))


if __name__ == '__main__': main()
