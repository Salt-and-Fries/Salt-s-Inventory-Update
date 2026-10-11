from pathlib import Path
from html import escape
from html.parser import HTMLParser
from urllib.parse import quote, urlsplit
import re

root = Path(__file__).resolve().parents[1]
md_path = root / 'docs/markdown/desktop_window_api.md'
html_path = root / 'docs/index.html'
markdown = md_path.read_text(encoding='utf-8')
old = html_path.read_text(encoding='utf-8')
lines = markdown.splitlines()
source_links = []

def slug(label):
    return re.sub(r'[^a-z0-9]+', '-', label.lower()).strip('-')

def href(target):
    if target.startswith(('https://', 'http://', '#')):
        return target
    parts = urlsplit(target)
    local = (md_path.parent / parts.path).resolve()
    assert local.exists(), f'Missing Markdown link: {target}'
    relative = local.relative_to(root).as_posix()
    if parts.fragment:
        headings = re.findall(r'^#{1,6} (.+)$', local.read_text(encoding='utf-8'), re.M)
        assert parts.fragment in {slug(h) for h in headings}, f'Missing fragment: {target}'
    source_links.append(target)
    kind = 'tree' if local.is_dir() else 'blob'
    return f'https://github.com/Salt-and-Fries/Salt-s-Inventory-Update/{kind}/master/{quote(relative)}' + (f'#{parts.fragment}' if parts.fragment else '')

token = re.compile(r'(`[^`]+`|\*\*[^*]+\*\*|\[[^\]]+\]\([^)]+\))')
def inline(value):
    result = []
    for part in token.split(value):
        if part.startswith('`') and part.endswith('`'):
            result.append('<code>' + escape(part[1:-1]) + '</code>')
        elif part.startswith('**') and part.endswith('**'):
            result.append('<strong>' + inline(part[2:-2]) + '</strong>')
        elif re.fullmatch(r'\[[^\]]+\]\([^)]+\)', part):
            label, target = re.fullmatch(r'\[([^\]]+)\]\(([^)]+)\)', part).groups()
            result.append('<a href="' + escape(href(target), quote=True) + '">' + inline(label) + '</a>')
        else:
            result.append(escape(part))
    return ''.join(result)

rendered = []
toc = []
codes = []
section = False
i = 0
while i < len(lines):
    line = lines[i]
    if not line.strip():
        i += 1
        continue
    if line.startswith('```'):
        language = line[3:].strip()
        block = []
        i += 1
        while i < len(lines) and not lines[i].startswith('```'):
            block.append(lines[i])
            i += 1
        assert i < len(lines), 'Unclosed code fence'
        codes.append('\n'.join(block))
        rendered.append('<pre><code class="language-' + escape(language) + '">' + escape(codes[-1]) + '</code></pre>')
        i += 1
        continue
    heading = re.fullmatch(r'(#{1,3}) (.+)', line)
    if heading:
        marks, label = heading.groups()
        key = slug(label)
        if len(marks) == 2:
            if section:
                rendered.append('</section>')
            rendered.append(f'<section class="chapter" id="{key}">')
            toc.append(f'<a href="#{key}">{escape(label)}</a>')
            section = True
        rendered.append(f'<h{len(marks)} id="heading-{key}">{inline(label)}</h{len(marks)}>')
        i += 1
        continue
    if line.startswith('|'):
        rows = []
        while i < len(lines) and lines[i].startswith('|'):
            rows.append([cell.strip() for cell in lines[i].strip().strip('|').split('|')])
            i += 1
        assert all(re.fullmatch(r':?-+:?', cell) for cell in rows[1]), 'Invalid table separator'
        assert all(len(row) == len(rows[0]) for row in rows), 'Uneven table columns'
        rendered.append('<div class="table-scroll"><table><thead><tr>' + ''.join('<th scope="col">' + inline(cell) + '</th>' for cell in rows[0]) + '</tr></thead><tbody>')
        for row in rows[2:]:
            rendered.append('<tr>' + ''.join('<td>' + inline(cell) + '</td>' for cell in row) + '</tr>')
        rendered.append('</tbody></table></div>')
        continue
    if line.startswith('- '):
        rendered.append('<ul>')
        while i < len(lines) and lines[i].startswith('- '):
            rendered.append('<li>' + inline(lines[i][2:]) + '</li>')
            i += 1
        rendered.append('</ul>')
        continue
    paragraph = []
    while i < len(lines) and lines[i].strip() and not lines[i].startswith(('#', '```', '- ', '|')):
        paragraph.append(lines[i])
        i += 1
    assert paragraph, f'Unsupported Markdown: {lines[i]}'
    rendered.append('<p>' + inline(' '.join(paragraph)) + '</p>')
if section:
    rendered.append('</section>')

prefix = old.split('<div class="layout">', 1)[0]
table_css = '.table-scroll{overflow-x:auto;margin:20px 0}table{width:100%;border-collapse:collapse;font-size:.92em}th,td{text-align:left;vertical-align:top;border:1px solid var(--line);padding:10px 12px}th{background:var(--bg)}td code{overflow-wrap:anywhere}'
if '.table-scroll{' not in prefix:
    prefix = prefix.replace('@media(prefers-color-scheme:dark)', table_css + '\n@media(prefers-color-scheme:dark)')
nav = '<div class="layout"><nav aria-label="Guide navigation"><label for="search">Find a topic</label><input id="search" type="search" placeholder="Search the guide…"><div class="toc">' + ''.join(toc) + '</div></nav><main>'
suffix = '<footer>' + old.split('<footer>', 1)[1]
output = prefix + nav + '\n'.join(rendered) + '\n' + suffix

class Audit(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids = []
        self.fragments = []
        self.code = []
        self.in_pre = False
        self.tables = 0
    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if 'id' in attrs:
            self.ids.append(attrs['id'])
        if attrs.get('href', '').startswith('#'):
            self.fragments.append(attrs['href'][1:])
        if tag == 'pre':
            self.in_pre = True
            self.code.append('')
        if tag == 'table':
            self.tables += 1
    def handle_endtag(self, tag):
        if tag == 'pre':
            self.in_pre = False
    def handle_data(self, data):
        if self.in_pre:
            self.code[-1] += data

audit = Audit()
audit.feed(output)
assert len(audit.ids) == len(set(audit.ids)), 'Duplicate HTML id'
assert set(audit.fragments) <= set(audit.ids), 'Broken HTML navigation'
assert audit.code == codes, 'Code sample changed during rendering'
assert audit.tables == 4, 'Expected four reference tables'
assert '\ufffd' not in output, 'Unicode replacement character'
md_path.write_bytes(markdown.replace('\r\n', '\n').replace('\n', '\r\n').encode('utf-8'))
html_path.write_bytes(output.replace('\r\n', '\n').replace('\n', '\r\n').encode('utf-8'))
print(f'Synced Markdown and HTML: {len(toc)} chapters, {audit.tables} tables, {len(codes)} unchanged code samples, {len(set(source_links))} valid repository links.')
