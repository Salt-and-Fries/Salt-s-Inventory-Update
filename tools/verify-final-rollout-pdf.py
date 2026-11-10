"""Render every final atlas page and verify its evidence and text bounds."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import csv
import hashlib
import json
import subprocess

import pdfplumber
from pypdf import PdfReader

ROOT = Path(__file__).resolve().parents[1]
PDF = ROOT / 'output/pdf/siu-all-target-ui-verification.pdf'
WORK = ROOT / ('build/all-target-rollout/pdf/final-render-' + hashlib.sha256(PDF.read_bytes()).hexdigest()[:12])
WORK.mkdir(parents=True, exist_ok=True)


def main():
    reader = PdfReader(PDF)
    pages = len(reader.pages)
    evidence = json.loads((WORK.parent / 'page-evidence.json').read_text())
    assert len(evidence) == 1444, len(evidence)
    assert len({entry['page'] for entry in evidence}) == len(evidence)
    renderer_warnings = []
    with ThreadPoolExecutor(max_workers=3) as pool:
        jobs = []
        for first in range(1, pages + 1, 250):
            last = min(first + 249, pages)
            if all((WORK / f'page-{number:04d}.png').is_file() for number in range(first, last + 1)):
                continue
            jobs.append(pool.submit(subprocess.run, [
                'pdftoppm', '-f', str(first), '-l', str(last), '-scale-to', '600',
                '-png', str(PDF), str(WORK / 'page')
            ], check=True, capture_output=True))
        for future in jobs:
            result = future.result()
            if result.stderr:
                renderer_warnings.append(result.stderr.decode(errors='replace'))
            print('Rendered page range', flush=True)
    rendered = list(WORK.glob('page-*.png'))
    assert len(rendered) == pages, (len(rendered), pages)
    out_of_bounds = []
    with pdfplumber.open(PDF) as document:
        for page_num, page in enumerate(document.pages, 1):
            for word in page.extract_words():
                box = [word[key] for key in ('x0', 'top', 'x1', 'bottom')]
                if box[0] < -1 or box[1] < -1 or box[2] > page.width + 1 or box[3] > page.height + 1:
                    out_of_bounds.append({'page': page_num, 'text': word['text'], 'bounds': box})
            page.close()
            if page_num % 250 == 0:
                print('Text checked through page', page_num, flush=True)
    (WORK / 'text-bounds.json').write_text(json.dumps(out_of_bounds, indent=2))
    verified = 0
    for entry in evidence:
        for side in ('native', 'siu'):
            source = ROOT / entry[side]['source']
            assert source.is_file(), source
            assert hashlib.sha256(source.read_bytes()).hexdigest() == entry[side]['sha256'], source
            verified += 1
    issues = json.loads((WORK.parent / 'comparison-input-issues.json').read_text())
    assert issues == [], issues
    with (ROOT / 'output/all-target-rollout/availability-and-registration.csv').open(encoding='utf-8-sig', newline='') as stream:
        rows = list(csv.DictReader(stream))
    assert len(rows) == 510, len(rows)
    summary = {
        'pdf': str(PDF), 'sha256': hashlib.sha256(PDF.read_bytes()).hexdigest(),
        'pages': pages, 'comparison_pages': len(evidence),
        'all_pages_rendered': len(rendered), 'source_frames_sha256_verified': verified,
        'outside_page_text': out_of_bounds, 'comparison_input_issues': issues,
        'availability_rows': len(rows), 'render_scale_to': 600,
        'renderer_environment_warnings': renderer_warnings,
        'functional_scope': 'Focused affected-menu checks; full gameplay suite not run.'
    }
    (WORK / 'render-verification.json').write_text(json.dumps(summary, indent=2))
    print(json.dumps(summary, indent=2), flush=True)
    assert not out_of_bounds, out_of_bounds


if __name__ == '__main__':
    main()
