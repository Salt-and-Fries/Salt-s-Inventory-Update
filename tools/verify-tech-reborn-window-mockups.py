"""Coverage and raster checks for the design PDF; not a gameplay suite."""
from pathlib import Path
import hashlib,json
from PIL import Image
from pypdf import PdfReader

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/tech-reborn-window-design'
sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()
manifest=json.loads((OUT/'manifest.json').read_text())
rows=json.loads((OUT/'fresh-native.json').read_text())
old=json.loads((OUT/'native-references.json').read_text())['cases']
byid={r['id']:r for r in rows}
assert len(rows)==67 and len(byid)==67
for r in rows:
    assert sha(ROOT/r['capture'])==r['sha256'] and r['state']['siu_loaded'] is False
for r in old:
    assert sha(ROOT/r['native']['capture'])==r['native']['sha256']
assert len([p for p in manifest['pages']if p['kind']=='machine'])==51
assert len({p['screen']for p in manifest['pages']if p['kind']=='machine'})==51
assert len(manifest['pages'])==58
formed=[r for r in rows if r.get('variant')=='formed-idle']
assert len(formed)==8 and all(r['state']['trValid']for r in formed)
assert len([r for r in rows if r.get('variant')=='unformed'])==8
for p in OUT.glob('*-blueprint.json'):
    assert json.loads(p.read_text())['valid'] is True

slot_checks=0
for p in manifest['pages']:
    row=byid[p['id']];b=row['state']['bounds'];native=Image.open(ROOT/row['capture']).convert('RGBA')
    im=Image.open(ROOT/p['window_image']).convert('RGBA');g=p['geometry'];assert g['image_pixels_per_gui_pixel']==2
    for s in g['slots']:
        x1,y1,x2,y2=s['source'];src=native.crop(tuple(2*v for v in [b['x']+x1,b['y']+y1,b['x']+x2,b['y']+y2]));x,y=[v*2 for v in s['dest']]
        assert im.crop((x,y,x+36,y+36)).tobytes()==src.tobytes(),(p['id'],s['index'])
        assert 0<=x and 0<=y and x+36<=im.width and y+36<=im.height
        slot_checks+=1

readout_checks=0
for p in manifest['pages']:
    if p['screen']not in ['GuiAESU','GuiIDSU','GuiLESU','GuiMFE','GuiMFSU','GuiBatbox']:continue
    r=byid[p['id']];b=r['state']['bounds'];g=p['geometry']['body'];sx,sy,_,_=g['source'];dx,dy=g['dest']
    rect=[45,18,125,26]
    source=Image.open(ROOT/r['capture']).convert('RGBA').crop(tuple(2*v for v in [b['x']+rect[0],b['y']+rect[1],b['x']+rect[2],b['y']+rect[3]]))
    target=Image.open(ROOT/p['window_image']).convert('RGBA').crop(tuple(2*v for v in [dx+rect[0]-sx,dy+rect[1]-sy,dx+rect[2]-sx,dy+rect[3]-sy]))
    assert source.tobytes()==target.tobytes(),p['id']
    readout_checks+=1

pdf=ROOT/'output/pdf/siu-tech-reborn-window-mockups.pdf';reader=PdfReader(pdf)
assert len(reader.pages)==63
for i,p in enumerate(manifest['pages'],start=2):
    text=reader.pages[i].extract_text()
    assert p['title']in text and 'ORIGINAL IN GAME / SIU ABSENT'in text and 'PROPOSED SIU WINDOW / MOCKUP'in text
assert len(list((ROOT/'tmp/pdfs/tech-reborn').glob('page-*.png')))==63

# The source/artwork task must not change the staged implementation.
staged=json.loads((ROOT/'output/extended-ae-modern-polish/verification.json').read_text())
assert all(sha(ROOT/t['jar'])==t['sha256']for t in staged['targets'])
assert sha(ROOT/'dist/salts-inventory-all-latest.zip')==staged['bundle_sha256']

report=dict(status='pass',pdf=str(pdf.relative_to(ROOT)),pdf_sha256=sha(pdf),pages=63,registered_machine_screens=51,
    comparison_plates=58,shared_configuration_designs=5,populated_examples=2,native_captures=67,formed_multiblocks=8,
    slot_pixel_checks=slot_checks,fractional_readout_pixel_checks=readout_checks,source_capture_hashes_unchanged=True,
    release_bundle_unchanged=True,visual_review='All 58 mockups and all 63 rendered pages reviewed; enlarged final popup/readout/details checked.',
    limits='Design/layout and opening/state observations only. No compatibility implementation or complete functional/gameplay suite.')
(OUT/'verification.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
