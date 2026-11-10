"""Four real original/fixed MEGA Cells UI comparisons."""
from pathlib import Path
import importlib.util

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('window_report',ROOT/'tools/build-window-details02-pdf.py')
report=importlib.util.module_from_spec(spec);spec.loader.exec_module(report)
report.WORK=ROOT/'build/window-detail-fixes-04/pdf'
report.OUT=ROOT/'output/pdf/siu-window-detail-fixes-04.pdf'
report.PDF_TITLE='SIU - MEGA Cells borders, portable workbench and Cell Dock title'
report.CASES=[
    ('megacells-mega-interface','MEGA Interface','MEGA Cells','neoforge',
     'The desktop background now samples the complete native lower rim. Two transparent rows in the modern source rectangle no longer replace half of the bottom border.',
     'All four footer rows match the installed native artwork exactly.',
     'Modern correction: 1.21.1 NeoForge. Vintage 1.20.1 Fabric / Forge geometry retained.'),
    ('megacells-mega-pattern-provider','MEGA Pattern Provider','MEGA Cells','neoforge',
     'The full native bottom bevel now closes the window beneath Return Inventory. Native pattern cells, return slots, labels and toolbar remain in their original positions.',
     'The complete footer and return-slot borders remain visible.',
     'Modern correction: 1.21.1 NeoForge. Vintage 1.20.1 Fabric / Forge geometry retained.'),
    ('megacells-portable-cell-workbench','Portable Cell Workbench','MEGA Cells','neoforge',
     'The missing desktop style is added. The original native workbench, cell slot, full filter grid and toolbar now render as the AE2 window instead of the small error panel.',
     'Cell insertion, ghost-filter editing, copy-mode toggling, clearing and exact item recovery passed focused in-game checks.',
     '1.21.1 NeoForge. This workbench is absent from the older 1.20.1 MEGA Cells release.'),
    ('own-ae-part-cell-dock','ME Cell Dock','MEGA Cells','neoforge',
     'An unknown synthetic SIU title no longer overrides the publisher title. The dock displays its own localized ME Cell Dock label; custom names are retained by the title resolver.',
     'The header uses the native localized title rather than a raw translation key.',
     '1.20.1 Fabric / Forge; 1.21.1 NeoForge.'),
]

def detail(row):
    state=row['state'];b=state.get('allWindows',state.get('windows'))[0]['bounds']
    x,y,w,h=b['x'],b['y'],b['width'],b['height']
    if row['ui'] in ['megacells-mega-interface','megacells-mega-pattern-provider']:
        box=(x,y+h-24,x+w,y+h+1)
    elif row['ui']=='megacells-portable-cell-workbench':
        box=(x+5,y+26,x+w-5,y+82)
    else:box=(x+2,y+2,x+w-2,y+19)
    return report.crop_gui(row,box)

report.detail=detail
if __name__=='__main__':report.main()
