"""Third five-window proof, using the shared real-capture report layout."""
from pathlib import Path
import importlib.util

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('window_report',ROOT/'tools/build-window-details02-pdf.py')
report=importlib.util.module_from_spec(spec)
spec.loader.exec_module(report)
report.WORK=ROOT/'build/window-detail-fixes-03/pdf'
report.OUT=ROOT/'output/pdf/siu-window-detail-fixes-03.pdf'
report.PDF_TITLE='SIU - Mount, Pigment Shifter, baskets and Filing Cabinet fixes'
report.CASES=[
    ('pr-mythic-direwolf-inventory','Mount Inventory','Mythic Mounts','forge',
     'The existing integrated mount composition now recognizes the Forge menu ID. Native equipment guides and the complete portrait remain inside the SIU frame.',
     'The portrait and equipment cells retain their native appearance.',
     '1.20.1 Fabric / Forge.'),
    ('spawn-pigment-shifter','Pigment Shifter','Spawn','forge',
     'The missing layout is now registered on 1.20.1. Native input guides, the preview, result slot and rotation affordance remain inside one integrated window.',
     'Input guides and the complete preview rotation control are preserved.',
     '1.20.1 Forge; 1.21.1 NeoForge. The older Fabric release does not contain this editor.'),
    ('supplementaries-lunch-basket','Lunch Basket','Supplementaries','neoforge',
     'The newer dedicated menu ID now selects the integrated slot layout. All six native cells fit in a compact window instead of the fallback bars.',
     'Both complete rows are retained, with controls that fit the compact header.',
     '1.20.1 Fabric / Forge; 1.21.1 Fabric / NeoForge.'),
    ('supplementaries-sack','Sack','Supplementaries','neoforge',
     'The dedicated Sack menu now selects the same native slot composition as its older version. The complete three-by-three grid fits inside the SIU frame.',
     'All nine slots and their borders remain visible.',
     '1.20.1 Fabric / Forge; 1.21.1 Fabric / NeoForge.'),
    ('toms-filing-cabinet','Filing Cabinet',"Tom's Storage",'neoforge',
     'Duplicate padding is removed. The grid and scrollbar align, the full track fits inside the frame, and scrolling uses the native backing capacity instead of only the 45 visible cells.',
     'Wheel, track-click and thumb-drag checks reached cell 511. First- and last-page withdrawals recovered the correct items.',
     '1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3 Fabric / NeoForge.'),
]


def detail(row):
    b=row['state'].get('allWindows',row['state'].get('windows'))[0]['bounds']
    x,y,w,height=b['x'],b['y'],b['width'],b['height']
    box={
        'pr-mythic-direwolf-inventory':(x+5,y+19,x+w-5,y+height-4),
        'spawn-pigment-shifter':(x+5,y+40,x+w-5,y+height-4),
        'supplementaries-lunch-basket':(x+25,y+19,x+w-25,y+height-4),
        'supplementaries-sack':(x+5,y+19,x+w-5,y+height-4),
        'toms-filing-cabinet':(x+6,y+height-31,x+w-6,y+height-3),
    }[row['ui']]
    return report.crop_gui(row,box)


original_crop=report.native_crop
def native_crop(row):
    if row['ui']=='toms-filing-cabinet':
        # Its native scrollbar is painted outside imageBounds, not as a widget.
        b=row['state']['bounds']
        return report.crop_gui(row,(b['x']-8,b['y']-8,b['x']+b['width']+25,b['y']+b['height']+8))
    return original_crop(row)


report.detail=detail
report.native_crop=native_crop
if __name__=='__main__':report.main()
