"""Two real original/fixed comparisons for the Configurator and South label."""
from pathlib import Path
import importlib.util

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('window_report',ROOT/'tools/build-window-details02-pdf.py')
report=importlib.util.module_from_spec(spec);spec.loader.exec_module(report)
report.WORK=ROOT/'build/window-detail-fixes-05/pdf'
report.OUT=ROOT/'output/pdf/siu-window-detail-fixes-05.pdf'
report.PDF_TITLE='SIU - Inventory Configurator layout and Chunk Loader South label'
report.CASES=[
    ('toms-inventory-configurator','Inventory Configurator',"Tom's Storage",'neoforge',
     'The native filter slot has its own clear space. Six controls keep their vertical order on the left, with Clear Configuration separately at the upper right. The incorrect Connector Filter label is removed.',
     'Filter insertion, recovery and right-click editing work. All seven visible native controls passed focused checks, including the world-selection and clear actions.',
     '1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3 Fabric / NeoForge. The different 1.20.1 nine-slot layout is retained.'),
    ('additionz-chunk-loader','Chunk Loader - South label','AdditionZ','fabric',
     'The window crop now includes the complete S below the chunk grid. It ends above the player-inventory border, keeping the other directions, slots and controls at their native positions.',
     'All eight rows of the native South glyph match the source texture. This was checked on both supported versions.',
     '1.20.1 Fabric; 1.21.1 Fabric.'),
]

def detail(row):
    state=row['state'];b=state.get('allWindows',state.get('windows'))[0]['bounds'];x,y=b['x'],b['y']
    if row['ui']=='toms-inventory-configurator':box=(x+8+79,y+24+41,x+8+106,y+24+68)
    else:box=(x+6+31,y+20+64,x+6+48,y+20+77)
    return report.crop_gui(row,box)

report.detail=detail
if __name__=='__main__':report.main()
