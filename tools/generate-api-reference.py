"""Generate a public signature inventory; --check detects stale consumer documentation."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / 'docs/markdown/api_reference.md'
SOURCES = [ROOT / 'versions/common/src/main/java/com/salts_inventory_update/api',
           ROOT / 'versions/26.3/fabric/src/main/java/com/salts_inventory_update/api']

def mask(source):
    # Preserve offsets while ignoring comments/string contents for brace matching.
    pattern = r'/\*[\s\S]*?\*/|//[^\n]*|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\''
    return re.sub(pattern, lambda m: ' ' * len(m.group()), source)

def closing(masked, opening):
    depth = 1
    for position in range(opening + 1, len(masked)):
        if masked[position] == '{': depth += 1
        if masked[position] == '}': depth -= 1
        if depth == 0: return position
    raise ValueError('Unclosed Java type/member')

def declaration(value):
    value = re.sub(r'/\*[\s\S]*?\*/|//[^\n]*', '', value).strip()
    return re.sub(r'\s+', ' ', value)

def record_constructor(header):
    record = re.search(r'\brecord\s+(\w+)(?:<[^>]+>)?\s*\(([\s\S]*)\)', header)
    return (record[1], record[2]) if record else None

def inventory(source, masked, opening, end, interface=False, enum=False, indent='', record=None):
    result = []
    cursor = opening + 1
    while cursor < end:
        delimiter = re.search(r'[;{]', masked[cursor:end])
        if not delimiter:
            if enum:
                tail = declaration(source[cursor:end])
                if tail: result.append(indent + tail)
            break
        stop = cursor + delimiter.start()
        header = declaration(source[cursor:stop])
        if masked[stop] == ';':
            if header and (enum or 'public ' in header or interface and not re.match(r'(private|protected)\b', header)):
                result.append(indent + header + ';')
            enum = False
            cursor = stop + 1
            continue
        finish = closing(masked, stop)
        nested = re.search(r'\b(class|interface|record|enum)\s+\w+', header)
        public = bool(re.search(r'\bpublic\b', header)) or interface and not re.match(r'(private|protected)\b', header)
        if nested and public:
            result.append(indent + header + ' {')
            result += inventory(source, masked, stop, finish, nested[1] == 'interface', nested[1] == 'enum', indent + '    ', record_constructor(header))
            result.append(indent + '}')
        elif public and header:
            if record and header == 'public ' + record[0]:
                header += '(' + record[1] + ')'
            result.append(indent + header + ';')
        cursor = finish + 1
    return result

lines = ['# Public API signature reference — 0.1.4 / API 5', '',
         'This inventory lists public consumer declarations from the shared API and the 26.3 native API. Signatures are reference notation, not standalone implementations. Record components supply their corresponding accessors and canonical constructor; enum values are listed. Bodies and private implementation members are omitted. The [API guide](desktop_window_api.md) specifies defaults, lifecycle, geometry, threading and ownership; the [transfer reference](recipe_transfer_api.md) specifies transfer validation and bounds.', '',
         'The shared contracts apply to every supported target. Native declarations use 26.3 names/imports here. On 1.20.1/1.21.1, `Identifier` is `ResourceLocation` and input events use Salt’s legacy event package; 1.20.1 lacks typed payload codecs. `nativeGraphics()` is `GuiGraphics` through 1.21.11 and `GuiGraphicsExtractor` on 26.x. Slot action types are `ClickType` through 1.21.11 and `ContainerInput` on 26.x. Compile the examples on the matching target to resolve native binary differences.', '',
         'Regenerate with `python tools/generate-api-reference.py`; verify without writing with `python tools/generate-api-reference.py --check`.', '']
count = 0
for root in SOURCES:
    for path in sorted(root.rglob('*.java')):
        source = path.read_text(encoding='utf-8')
        masked = mask(source)
        match = re.search(r'\bpublic\s+(?:(?:final|abstract)\s+)?(class|interface|record|enum)\b', masked)
        if not match: continue
        opening = masked.index('{', match.start())
        end = closing(masked, opening)
        header = declaration(source[match.start():opening])
        package = re.search(r'^package (.*);', source, re.M)[1]
        relative = path.relative_to(ROOT).as_posix()
        origin = 'Shared; no native Minecraft types.' if root == SOURCES[0] else 'Native 26.3; available with the version-specific types described above.'
        lines += [f'## {path.stem}', '', f'`{package}`. {origin} [Source](../../{relative}).', '']
        if path.stem in {'DesktopChromeMetrics', 'DesktopSurfaceControl'}:
            lines += ['[Standard control placement and compression](desktop_window_api.md#matching-the-controls-on-salts-vanilla-windows) specifies ordering, full-title fit thresholds, the expanded-or-More/Close policy, trailing gap/right inset, title minimum and the supported mod-owned control subset.', '']
        lines += ['```java']
        lines += [line for line in source.splitlines() if line.startswith('import ')]
        lines += [header + ' {']
        lines += inventory(source, masked, opening, end, match[1] == 'interface', match[1] == 'enum', '    ', record_constructor(header))
        lines += ['}', '```', '']
        count += 1
output = '\n'.join(lines).rstrip() + '\n'
if '--check' in sys.argv:
    if not TARGET.exists() or TARGET.read_text(encoding='utf-8') != output:
        raise SystemExit('Public signature reference is stale; regenerate it.')
else:
    TARGET.write_text(output, encoding='utf-8')
print(f'Public signature reference: {count} types, synchronized.')
