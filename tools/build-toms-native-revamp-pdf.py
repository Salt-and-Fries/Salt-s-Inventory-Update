"""Four original/publisher-matched real-game comparisons for Tom's Storage."""
from pathlib import Path
import importlib.util

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('toms_window_report',ROOT/'tools/build-window-details02-pdf.py')
report=importlib.util.module_from_spec(spec);spec.loader.exec_module(report)
report.WORK=ROOT/'build/toms-native-window-revamp/pdf'
report.OUT=ROOT/'output/pdf/siu-toms-storage-native-window-revamp.pdf'
report.PDF_TITLE="SIU - Tom's Storage native window revamp"
PORTS='All 12 targets: 1.20.1 Fabric / Forge; 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3 Fabric / NeoForge.'
report.CASES=[
    ('toms-tag-item-filter','Tag Item Filter',"Tom's Storage",'fabric',
     'The original gray panels, row buttons, selection styling and small scrollbar thumbs replace the brown lists. The sample slot and +/- buttons follow the native arrangement; the mode toggle stays attached on the left.',
     'Real sample insertion, add, remove and mode changes passed on legacy Fabric and modern NeoForge. Full tag IDs remain available in tooltips. The SIU example is populated.',PORTS),
    ('toms-level-emitter','Level Emitter',"Tom's Storage",'fabric',
     'Both native button banks are restored: +/-1, 10, 100 and 1000. The sample slot and editable quantity stay between them, with the threshold toggle outside the frame. Original panel and button textures are retained.',
     'All eight buttons, editing a complete 4242 value and the threshold toggle reached the native server state on both tested releases. Native Shift amounts are retained; physical Shift was not live-tested.',PORTS),
    ('toms-inventory-link','Inventory Link',"Tom's Storage",'fabric',
     'The Beacon Level label, gray framed channel list, search/name field and original control order are restored. Native row styling, lock icons and small scrollbar replace the improvised segmented list.',
     'Creating eight channels, selecting, changing privacy, deleting, wheel scrolling and dragging the thumb passed on both tested releases. The SIU example shows populated channels.',PORTS),
    ('toms-storage-terminal','Storage Terminal',"Tom's Storage",'fabric',
     'The existing terminal design is retained, now opening with nine columns and six rows. A layout migration prevents another menu at the same position from shrinking it. The Crafting Terminal receives the same default-size correction.',
     'Default grid sizes and text-field ownership were checked on both releases. User-sized grids remain supported when the optional resize setting is enabled. No complete gameplay suite was run.',PORTS),
]

original_crop=report.h.atlas.crop
def main_crop(row,side):
    if side!='siu':return original_crop(row,side)
    b=row['state'].get('allWindows',row['state'].get('windows'))[0]['bounds']
    left=26 if row['ui'] in ['toms-tag-item-filter','toms-level-emitter'] else 8
    s=row['state']
    picture,pixels=report.crop_gui(row,(max(0,b['x']-left),max(0,b['y']-8),min(s['guiWidth'],b['x']+b['width']+8),min(s['guiHeight'],b['y']+b['height']+7)))
    return picture,None,pixels
report.h.atlas.crop=main_crop

def detail(row):
    s=row['state'];b=s.get('allWindows',s.get('windows'))[0]['bounds'];x,y=b['x'],b['y']
    if row['ui']=='toms-tag-item-filter':box=(x+7,y+23,x+173,y+84)
    elif row['ui']=='toms-level-emitter':box=(x+20,y+24,x+150,y+87)
    elif row['ui']=='toms-inventory-link':box=(x+12,y+39,x+121,y+73)
    else:box=(x+6,y+132,x+198,y+153)
    return report.crop_gui(row,box)

report.detail=detail
if __name__=='__main__':report.main()
