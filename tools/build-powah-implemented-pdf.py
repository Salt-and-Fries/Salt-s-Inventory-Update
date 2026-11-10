"""Actual native/SIU game comparisons for the approved Powah implementation."""
from pathlib import Path
from io import BytesIO
import importlib.util
import json

from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/powah-implemented'
PDF=ROOT/'output/pdf/siu-powah-implemented-windows.pdf'
N=Image.Resampling.NEAREST
spec=importlib.util.spec_from_file_location('powah_design',ROOT/'tools/build-powah-window-mockups.py')
design=importlib.util.module_from_spec(spec);spec.loader.exec_module(design)
BLOCKS=['reactor','furnator','magmator','thermo_generator','solar_panel','energy_cell','ender_cell','ender_gate','energy_hopper','energy_discharger','player_transmitter','energy_cable']


def capture_crop(run,name):
    journal=json.loads((run/'powah-checks/journal.json').read_text())
    row=next(r for r in journal['captures']if r['id']==name)
    im=Image.open(ROOT/row['screenshot']).convert('RGBA');b=row['bounds'];p=row['composition']
    l=t=0;r=b['width'];d=b['height']
    for fragment in p['regions']:
        if fragment.get('exterior'):
            l=min(l,fragment['x']);t=min(t,fragment['y']);r=max(r,fragment['x']+fragment['source']['width']);d=max(d,fragment['y']+fragment['source']['height'])
    return im.crop(tuple(2*v for v in [b['x']+l-5,b['y']+t-5,b['x']+r+5,b['y']+d+5]))


def main():
    proof=json.loads((OUT/'verification.json').read_text());assert proof['status']=='pass'
    pdfmetrics.registerFont(TTFont('Segoe','C:/Windows/Fonts/segoeui.ttf'))
    pdfmetrics.registerFont(TTFont('SegoeB','C:/Windows/Fonts/segoeuib.ttf'))
    W,H=1191,842;c=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1)
    c.setTitle('Powah! / Implemented SIU windows / Minecraft 1.20.1')
    c.setAuthor("Salt's Inventory Update / Project Everything")
    ink,teal,muted,light='#20373D','#197C7C','#5E7278','#EFF5F5';page=0
    def text(s,x,y,size=12,bold=False,color=ink):
        c.setFont('SegoeB'if bold else'Segoe',size);c.setFillColor(HexColor(color));c.drawString(x,y,s)
    def wrap(s,x,y,w,size=12,leading=18):
        line=''
        for word in s.split():
            trial=(line+' '+word).strip()
            if line and pdfmetrics.stringWidth(trial,'Segoe',size)>w:text(line,x,y,size);y-=leading;line=word
            else:line=trial
        if line:text(line,x,y,size);y-=leading
        return y
    def start(title,subtitle):
        nonlocal page
        page+=1;c.setFillColor(HexColor('#FFFFFF'));c.rect(0,0,W,H,fill=1,stroke=0)
        text('PROJECT EVERYTHING / POWAH! / IMPLEMENTED WINDOWS',42,H-42,10,True,teal)
        text(title,42,H-79,27,True);text(subtitle,42,H-104,11,False,muted)
        c.setStrokeColor(HexColor('#D6E3E3'));c.line(42,44,W-42,44)
        text('Minecraft 1.20.1 / Powah 5.0.11  |  All UI images are genuine game captures; no mockups are shown.',42,26,9,False,muted)
        text(str(page),W-60,26,10,True,muted)
    def panel(source,box,maxscale=None):
        x,y,w,h=box;c.setFillColor(HexColor(light));c.roundRect(x,y,w,h,9,fill=1,stroke=0)
        im=source.copy()if isinstance(source,Image.Image)else Image.open(source).convert('RGBA')
        scale=min((w-20)/im.width,(h-20)/im.height)
        if maxscale:scale=min(scale,maxscale)
        iw,ih=im.width*scale,im.height*scale
        im=im.resize((im.width*3,im.height*3),N)
        c.drawImage(ImageReader(im),x+(w-iw)/2,y+(h-ih)/2,iw,ih,mask='auto')
    def end():c.showPage()

    start('Powah! windows, implemented','Eleven concrete menu types and the shared Ender Gate opening route / Fabric and Forge.')
    text('The approved design, using Powah\'s live screens.',42,694,23,True)
    wrap('The native working panels, slot silhouettes, gauges and controls are retained inside matching SIU frames. The embedded player inventory is removed; SIU\'s player inventory remains a separate window.',42,660,1070,14,21)
    for i,block in enumerate(['reactor','ender_cell','energy_discharger']):
        panel(OUT/(block+'-fabric-window.png'),(42+i*371,367,357,220))
    text('Focused verification',42,322,17,True)
    wrap(f"{proof['live_assertions']} affected-window assertions passed across the recorded Fabric and Forge runs. All twelve opening variants were captured on both loaders. {proof['artwork']['compared_static_pixels']:,} stable native working-area pixels match the original references, with zero differences.",42,293,1070,14,21)
    wrap('Checks cover real controls and item movement, clicked cable faces, channel selection, valid and invalid card behavior, active machine displays and window lifecycle. Exact per-target counts, binaries and coverage limits are saved with the implementation evidence.',42,207,1070,12,18)
    text('Core and the public API are unchanged.',42,132,15,True,teal)
    wrap('This report covers the approved 1.20.1 baseline. Newer Minecraft targets are not included in this implementation pass. Guidebooks, recipe pages and world-only interfaces retain their native handling.',42,105,1070,11,17)
    end()

    for s,block in zip(design.SPECS,BLOCKS):
        start(s['title'],'Original reference and implemented SIU window / 1.20.1 Fabric / Powah 5.0.11.')
        text('ORIGINAL IN GAME / SIU ABSENT',42,695,11,True,teal)
        text('IMPLEMENTED SIU WINDOW / IN GAME',611,695,11,True,teal)
        panel(ROOT/'output/powah-window-design'/(s['representative']+'-original.png'),(42,277,538,400))
        panel(OUT/(block+'-fabric-window.png'),(611,277,538,400),maxscale=1.25)
        text('Native function',42,244,13,True);wrap(s['does'],42,221,333,12,17)
        keep=s['keep']if block!='energy_cell'else 'The two vertically stacked cells, lightning silhouettes, energy display and native proportions are retained. Creative variants use the same family layout.'
        text('Retained design',416,244,13,True);wrap(keep,416,221,333,12,17)
        detail=s['detail']if block!='energy_cell'else 'Charge and discharge cells keep the native restrictions. The energy display and external configuration controls stay synchronized with the real tile.'
        text('Important details',790,244,13,True);wrap(detail,790,221,355,12,17)
        text('PRESERVED: '+s['reserved'],42,91,10,True,teal)
        text('The displayed numeric values and animation state come from the running mod.',42,65,9,False,muted)
        end()

    start('Forge: every opening variant','Actual Forge captures of the same approved windows. Shared body artwork is identical between publisher loaders.')
    for i,(s,block)in enumerate(zip(design.SPECS,BLOCKS)):
        x=42+i%3*371;top=687-i//3*153
        text(s['title'],x,top,12,True)
        panel(OUT/(block+'-forge-window.png'),(x,top-136,357,124),maxscale=.76)
    end()

    run=ROOT/proof['advanced_runs']['fabric']
    start('Active machinery and complete tanks','Genuine live-state examples, with native animations, fluid rendering and resource instrumentation.')
    examples=[('furnator-active','Furnator: native fuel consumption and FE generation'),('reactor-active','Reactor: native input roles and generation controls'),('magmator-fluid-active','Magmator: native lava tank and full border'),('thermo_generator-fluid-active','Thermo Generator: native coolant tank and full border')]
    for i,(name,title)in enumerate(examples):
        x=42+i%2*565;top=685-i//2*300
        live=ROOT/proof['fluid_runs']['fabric']if name!='furnator-active'else run
        if name=='reactor-active':name='reactor-fluid-active'
        if name=='magmator-fluid-active':name='magmator-full-tank-moved'
        text(title,x,top,13,True);panel(capture_crop(live,name),(x,top-265,538,246),maxscale=1.15)
    end()

    for loader in ['fabric','forge']:
        run=ROOT/proof['presentation_runs'][loader]
        start(loader.title()+': a complete game screen','Moved Powah window, attached configuration controls and the independent player inventory.')
        panel(run/'screenshots/powah-moved-window-with-inventory.png',(42,87,1107,600))
        end()

    start('Verification and scope','Focused checks of the affected windows. This is not the complete Powah gameplay suite.')
    y=685
    for target in proof['targets']:
        text(target['loader'].title()+' / Powah 5.0.11',42,y,17,True)
        y=wrap(f"{target['live_assertions']} final affected-window assertions passed. Compatibility binary: {Path(target['jar']).name}. Every captured native machine slot and control has a retained region; embedded player cells are excluded.",42,y-29,1090,12,18)-30
    text('What is covered',42,y,16,True);y-=28
    y=wrap('Native face/all-face controls and redstone modes; correct clicked cable face; Ender channel selection; energy-item transfers; binding-card validation and NBT retention; active gauges and fluid tanks; complete slot and control geometry; parent movement, attached input, pin/unlock/close/reopen behavior; the Ender Cell capacity upgrade through SIU\'s authenticated quick-move route.',42,y,1090,12,18)-28
    text('Evidence limits',42,y,16,True);y-=28
    y=wrap('The native-menu implementations apply to every tier using these eleven registered menu types. Live checks exercise representative tiers and states, rather than every gameplay configuration. Dedicated multiplayer, optional recipe-viewer combinations, alternate resource packs, full automation throughput and newer Minecraft versions were not exercised.',42,y,1090,12,18)-28
    text('Native interfaces preserved',42,y,16,True);y-=28
    y=wrap('The Powah manual, recipe-viewer pages, Energizing Orb/Rod interactions and world overlays are outside this window integration. Their original flows remain available.',42,y,1090,12,18)-28
    text('Evidence package',42,y,16,True);y-=28
    wrap('output/powah-implemented contains verification records, exact staged JAR hashes, genuine window crops and the static-art comparison. The excluded observation helpers are not packaged in the Compatibility JARs.',42,y,1090,12,18)
    end();c.save()
    print(json.dumps(dict(pdf=str(PDF),pages=page),indent=2))


if __name__=='__main__':main()
