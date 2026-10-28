"""Build the illustrated 1.20.1 second-batch report from real in-game evidence."""
from pathlib import Path
import json
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor, Color
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase.pdfmetrics import stringWidth

ROOT=Path(__file__).resolve().parents[1]
EVIDENCE=ROOT/'build/project-everything-ten-1.20.1/gameplay'
TEMP=ROOT/'tmp/pdfs/project-everything-ten'
OUTPUT=ROOT/'output/pdf/siu-project-everything-ten-mods-1.20.1.pdf'
W,H=842,595
INK=HexColor('#113842'); TEAL=HexColor('#087d84'); MUTED=HexColor('#52686f')
PALE=HexColor('#edf3f3'); LINE=HexColor('#d4e0e1')


def records(loader):
    runs=['ten-final-fabric-09','ten-recipes-fabric-10'] if loader=='fabric' else ['ten-forge-03','ten-forge-04','ten-recipes-forge-05']
    result=[]
    for run in runs:
        folder=EVIDENCE/f'1.20.1-{loader}-{run}'
        for file in folder.glob('images-*.json'):
            for record in json.loads(file.read_text()):
                record['_time']=file.stat().st_mtime
                record['_run']=run
                record['_loader']=loader
                result.append(record)
    return result


RECORDS={loader:records(loader) for loader in ['fabric','forge']}


def shot(loader,case,state,report=None):
    candidates=[r for r in RECORDS[loader] if r['case']==case and r['state']==state]
    if report is not None:
        folder=EVIDENCE/f'1.20.1-{loader}-{report[0]}'
        return next(r for r in json.loads((folder/report[1]).read_text()) if r['case']==case and r['state']==state)
    if not candidates:raise ValueError((loader,case,state))
    # Repeated interaction checks may recapture the same named state. Match the
    # latest command as well as the report file, so its crop fits that image.
    return max(candidates,key=lambda r:(r['_time'],r['sequence']))


def crop(record,mode='window'):
    image=Image.open(record['file']).convert('RGB')
    client=record['client'];gw,gh=client['guiWidth'],client['guiHeight']
    scale=image.height/gh
    if mode=='screen':bounds=(0,0,gw,gh)
    elif mode=='editor':bounds=(max(0,gw/2-160),20,min(gw,gw/2+160),min(gh,340))
    else:
        boxes=[w['bounds'] for w in client['allWindows']] if mode=='group' else [client['window']]
        if mode=='group':
            if client.get('desktopWindows'):
                for window in client['desktopWindows']:
                    if window.get('hidden'):continue
                    boxes.append(window['bounds'])
                    if window.get('recipeVisible'):boxes.append(window['recipeBounds'])
            elif record['state']=='output-transferred':
                # Earlier records did not export ordinary player-window geometry.
                # Its visible default rectangle is independently checked in the
                # original full-size capture before using this crop.
                boxes.append(dict(x=gw/2-88,y=gh/2-43,width=176,height=86))
        boxes=[b for b in boxes if b]
        left=min(b['x'] for b in boxes);top=min(b['y'] for b in boxes)
        right=max(b['x']+b['width'] for b in boxes);bottom=max(b['y']+b['height'] for b in boxes)
        # Include real exterior recipe buttons and the forge's top reservoir.
        bounds=(max(0,left-27),max(0,top-20),min(gw,right+9),min(gh,bottom+9))
    result=image.crop(tuple(round(v*scale) for v in bounds))
    return result


def lines(text,font,size,width):
    result=[]
    for paragraph in text.split('\n'):
        line=''
        for word in paragraph.split():
            candidate=(line+' '+word).strip()
            if stringWidth(candidate,font,size)>width and line:
                result.append(line);line=word
            else:line=candidate
        result.append(line)
    return result


class Report:
    def __init__(self):
        TEMP.mkdir(parents=True,exist_ok=True);OUTPUT.parent.mkdir(parents=True,exist_ok=True)
        self.pdf=canvas.Canvas(str(OUTPUT),pagesize=(W,H))
        self.pdf.setTitle('Project Everything | Ten mod families on Minecraft 1.20.1')
        self.pdf.setAuthor("Salt's Inventory Update")
        self.page=0;self.crops=[]

    def text(self,text,x,y,size=11,color=INK,font='Helvetica',width=None,leading=None):
        self.pdf.setFont(font,size);self.pdf.setFillColor(color)
        content=lines(text,font,size,width) if width else text.split('\n')
        for line in content:
            self.pdf.drawString(x,y,line);y-=leading or size*1.35
        return y

    def start(self,title,kicker='PROJECT EVERYTHING / 1.20.1',subtitle=None):
        if self.page:self.pdf.showPage()
        self.page+=1
        self.pdf.setFillColor(Color(1,1,1));self.pdf.rect(0,0,W,H,fill=1,stroke=0)
        self.text(kicker,32,H-30,8.5,TEAL,'Helvetica-Bold')
        self.text(title,32,H-62,25,INK,'Helvetica-Bold')
        if subtitle:self.text(subtitle,32,H-84,10.5,MUTED,width=W-64)
        self.pdf.setStrokeColor(LINE);self.pdf.line(32,36,W-32,36)
        self.text("Core 0.1.6 + Compatibility 0.1.3 | Real in-game captures | 26 October 2026",32,22,8,MUTED)
        self.pdf.setFont('Helvetica-Bold',8);self.pdf.drawRightString(W-32,22,str(self.page))

    def picture(self,r,x,y,w,h,label,mode='window'):
        self.pdf.setFillColor(PALE);self.pdf.roundRect(x,y,w,h,7,fill=1,stroke=0)
        self.text(label,x+12,y+h-20,10,INK,'Helvetica-Bold',width=w-24)
        im=crop(r,mode)
        path=TEMP/f'crop-{len(self.crops):03}.png';im.save(path);self.crops.append(str(path))
        available_w,available_h=w-20,h-40
        factor=min(available_w/im.width,available_h/im.height)
        iw,ih=im.width*factor,im.height*factor
        self.pdf.drawImage(ImageReader(im),x+(w-iw)/2,y+8+(available_h-ih)/2,width=iw,height=ih)

    def note(self,text,y=58):self.text(text,32,y,10,MUTED,width=W-64,leading=13)

    def two(self,left,right,labels,mode_left='window',mode_right='window',y=112,height=370):
        self.picture(left,32,y,381,height,labels[0],mode_left)
        self.picture(right,429,y,381,height,labels[1],mode_right)


def main():
    r=Report()
    r.start('Ten mod families. One desktop.',subtitle='Minecraft 1.20.1 | Fabric and Forge | Compact native layouts, original controls and real inventory behavior')
    cover=[('fabric','bartering','bartered-loot','Bartering Station'),('fabric','alloy-forge','smelting-active','Alloy Forgery'),
           ('fabric','heater','three-modules','Seasons: Extras'),('forge','copper-pot','cooking-active',"Miner's Delight"),
           ('fabric','identifying','identifying-active','Better Archeology'),('forge','sawmill-wide','search-result','Sawmill')]
    for i,(loader,case,state,label) in enumerate(cover):
        r.picture(shot(loader,case,state),32+(i%3)*264,295-(i//3)*192,250,180,label)
    r.note('17 available mod/loader combinations reviewed. Alloyed 1.20.1 has no separate screen; its original Create-based workflow remains native.',y=76)

    r.start('Coverage at a glance',subtitle='Each available 1.20.1 release was inspected. An unavailable release is not advertised as support.')
    rows=[('Bartering Station','8.0.0','8.0.0','Station inventory'),('Alloy Forgery','2.1.2+1.20','Unavailable','Forge menu and lava reservoir'),
          ('Fabric Seasons: Extras','1.3.2-BETA+1.20','Unavailable','Heater and chiller'),('Alloyed','2.1.5 release','2.1.4 release','No owned screen/menu in 1.20.1'),
          ("Farmer's Delight",'Refabricated 2.5.7','1.3.4','Cooking pot, cabinets and sign editors'),("Miner's Delight",'Unavailable','1.4.5-backport','Copper pot'),
          ('Better Archeology','1.2.1','1.2.1','Identifying table and villager fossil'),('Illager Invasion','8.0.7','8.0.7','Imbuing table'),
          ('Galosphere','1.5.2','1.5.2','Combustion table'),('Sawmill','1.4.11','1.4.11','Recipe list, search and both widths')]
    xs=[42,248,421,566];widths=[198,164,137,230]
    for i,row in enumerate([('Family','Fabric','Forge','UI reviewed')]+rows):
        y=467-i*32
        if i%2==0:r.pdf.setFillColor(PALE);r.pdf.rect(32,y-10,778,32,fill=1,stroke=0)
        for x,width,cell in zip(xs,widths,row):r.text(cell,x,y,10 if i else 10.5,INK,'Helvetica' if i else 'Helvetica-Bold',width=width)
    r.note('Alloyed uses Create on 1.20.1. Its later standalone forge UI is outside this version. Exact release IDs and hashes are retained in the repository inventory.',y=79)

    r.start('Bartering Station',subtitle='Native piglin preview, nearby count, cooldown arrows, gold inputs and collected loot.')
    r.two(shot('fabric','bartering','bartering-active'),shot('forge','bartering','bartered-loot'),['Fabric / active bartering','Forge / native loot collected'])
    r.note('Checked: pickup and return, moving and reopening the window, piglin availability and native loot delivery. A real mouse also picked up a Forge output.',y=78)

    r.start('Alloy Forgery',subtitle='The native machine panel is fitted closely; its lava reservoir remains above the main frame.')
    r.picture(shot('fabric','alloy-forge','smelting-active'),32,258,381,226,'Fabric / native smelting animation')
    r.picture(shot('fabric','alloy-forge','smelted'),429,258,381,226,'Fabric / completed native output')
    r.picture(shot('fabric','alloy-forge','lava-reservoir'),32,65,778,180,'Fabric / filled exterior lava reservoir')

    r.start('All six forge controller materials',subtitle='The shared native interface follows each installed forge definition and temperature tier.')
    materials=[('bricks','Bricks'),('stone_bricks','Stone bricks'),('deepslate_bricks','Deepslate bricks'),
               ('polished_blackstone','Polished blackstone'),('prismarine_bricks','Prismarine bricks'),('end_stone_bricks','End stone bricks')]
    for i,(key,label) in enumerate(materials):r.picture(shot('fabric','alloy-'+key,'active'),32+(i%3)*264,284-(i//3)*207,250,195,label)

    r.start('Fabric Seasons: Extras',subtitle='Both native machine skins, three fuel modules and the original valve and particle controls.')
    r.picture(shot('fabric','heater','three-modules'),32,277,381,207,'Heater / active fuel and modules')
    r.picture(shot('fabric','chiller','three-modules'),429,277,381,207,'Chiller / active fuel and modules')
    r.picture(shot('fabric','seasons-two-windows','fuel-transferred'),32,61,778,204,'Two independent heater windows / fuel transferred','group')

    r.start("Farmer's Delight / Refabricated",subtitle='Cooking progress, heat, serving container and native output; the recipe button sits outside the frame.')
    r.picture(shot('fabric','cooking-pot','cooking-active'),32,322,381,162,'Fabric / cooking pot')
    r.picture(shot('forge','cooking-pot','cooking-active'),429,322,381,162,'Forge / cooking pot')
    r.picture(shot('fabric','cooking-pot','recipe-book'),32,61,778,249,'Native recipe component inside the desktop workflow','group')

    r.start("Miner's Delight",subtitle='Forge only on 1.20.1. The copper pot retains its four ingredients and copper-cup serving behavior.')
    r.picture(shot('forge','copper-pot','cooking-active'),32,261,381,223,'Forge / native cooking state')
    r.picture(shot('forge','copper-pot','recipe-book'),429,261,381,223,'Forge / recipe book open','group')
    r.picture(shot('forge','copper-pot','output-transferred'),32,61,778,188,'Prepared soup transferred into the player window','group')

    r.start('Recipe selection and ghost previews',subtitle='The original search and recipe buttons retain native ingredient placement and missing-ingredient previews.')
    r.picture(shot('forge','cooking-pot','unavailable-recipe-ghost'),32,277,778,207,'Forge / native ghost preview for an unavailable recipe','group')
    r.picture(shot('forge','copper-pot','recipe-selected'),32,61,778,204,'Forge / available soup ingredients placed through the recipe book','group')

    r.start('Better Archeology',subtitle='Both menu families are compact; the villager fossil keeps its entire decorative socket.')
    for x,y,loader,case,state,label in [(32,277,'fabric','identifying','identifying-active','Fabric / identifying in progress'),
            (429,277,'forge','identifying','identified','Forge / identified result'),(32,61,'fabric','fossil','ready','Fabric / villager fossil socket'),
            (429,61,'forge','fossil','ready','Forge / villager fossil socket')]:
        r.picture(shot(loader,case,state),x,y,381,207,label)

    r.start('Illager Invasion',subtitle='The Imbuing illustration and all slot roles are retained, including a valid above-maximum enchantment result.')
    r.picture(shot('fabric','imbuing','valid-upgrade'),32,265,381,219,'Fabric / valid native upgrade')
    r.picture(shot('forge','imbuing','valid-upgrade'),429,265,381,219,'Forge / valid native upgrade')
    r.picture(shot('fabric','imbuing','output-transferred'),32,61,778,192,'Unbreaking IV item transferred to the player window','group')

    r.start('Galosphere',subtitle='The native Combustion Table retains its bomb and modifier slots without the unused inventory bands.')
    r.picture(shot('fabric','combustion','configured-bomb'),32,265,381,219,'Fabric / slime, string and gunpowder modifiers')
    r.picture(shot('forge','combustion','configured-bomb'),429,265,381,219,'Forge / all three native bomb modifiers')
    r.picture(shot('fabric','combustion','output-transferred'),32,61,778,192,'Configured bomb transferred to the player window','group')

    r.start('Sawmill: search and both widths',subtitle='Native search focus, filtered recipe selection and results are preserved on both loaders.')
    for x,y,loader,case,label in [(32,277,'fabric','sawmill-narrow','Fabric / narrow, filtered result'),
            (429,277,'forge','sawmill-narrow','Forge / narrow, filtered result'),(32,61,'fabric','sawmill-wide','Fabric / wide, filtered result'),
            (429,61,'forge','sawmill-wide','Forge / wide, filtered result')]:
        r.picture(shot(loader,case,'search-result'),x,y,381,207,label)

    r.start('Cabinets stay ordinary storage',subtitle="Farmer's Delight cabinets use the standard desktop storage window and original inventory contents.")
    r.two(shot('fabric','farmers-cabinet','native-storage'),shot('forge','farmers-cabinet','native-storage'),['Fabric / cabinet','Forge / cabinet'])
    r.note('Checked: opening, stored contents and slot-to-slot movement. These cabinets do not need a custom machine template.',y=78)

    r.start('Canvas sign editors stay native',subtitle='Standalone text editors are preserved as original screens, rather than being forced into inventory windows.')
    r.two(shot('fabric','canvas-sign','native-editor'),shot('fabric','hanging-canvas-sign','native-editor'),
          ['Fabric / canvas sign editor','Fabric / hanging canvas sign editor'],'editor','editor',y=80,height=404)
    r.note('Both editor screens also opened and exited on Forge. Native sign artwork and controls are retained.',y=60)

    r.start('Alloyed: no standalone UI in 1.20.1',subtitle='These legacy releases add Create materials and recipes. They own no container screen to redesign.')
    r.two(shot('fabric','alloyed','items-in-storage'),shot('forge','alloyed','items-in-storage'),['Fabric / Alloyed materials in storage','Forge / Alloyed materials in storage'])
    r.note('Bronze and steel ingots use the normal storage and player windows. Their transfer was checked. This does not claim complete support for Create machinery.',y=78)

    r.start('Fit and scale',subtitle='Compact bodies and attached exterior parts are bounded together by the desktop.')
    r.picture(shot('fabric','alloy-forge','scale-three'),32,83,500,401,'GUI scale 3 / real viewport','screen')
    r.picture(shot('fabric','fossil','moved'),548,290,262,194,'Small socket after moving')
    r.picture(shot('fabric','alloy-forge','moved'),548,83,262,194,'Forge with its attached reservoir')
    r.note('Focused bounds checks passed at the left, right, top and bottom walls, including the forge reservoir.',y=60)

    r.start('Validation and practical limits',subtitle='Focused affected-menu checks, not the complete functional or gameplay suite.')
    items=[('Built','Both 1.20.1 Core 0.1.6 and Compatibility 0.1.3 loader modules built successfully; targeted native-layout regression tests passed.'),
           ('Reviewed','Every unique adapted menu, both Seasons skins, all six Alloy Forgery materials, both Sawmill widths and standalone sign editors were captured in game.'),
           ('Exercised','Native item pickup/return, move/reopen, processing and result transfer. Seasons controls address independent sources; Sawmill search selects real native recipes.'),
           ('Core fixes','Preserved native click-time carried items and Forge opening data. Supplied recipe books fit beside windows and their exterior controls, and toggle reliably after reopening.'),
           ('Boundary','Integration classes consume public Core APIs. Optional upstream mods, QA classes and another copy of Core are not embedded in Compatibility.'),
           ('Upstream conflict','Forge Create 6.0.8 references a silver ingot removed by Galosphere 1.5.2, preventing world loading together. Separate unmodified fixtures were used.'),
           ('Not covered','Dedicated multiplayer, Spatial GUI and Controlify interaction were not exercised in this batch. Other Minecraft versions have not received these nine adapters.')]
    y=464
    for label,body in items:
        r.text(label,32,y,11,TEAL,'Helvetica-Bold');r.text(body,157,y,11,INK,width=649,leading=14);y-=54
    r.note('Original evidence: build/project-everything-ten-1.20.1/gameplay. Pinned inventory and implementation notes are retained in the repository.',y=66)

    r.start('Upstream references and evidence',subtitle='UI inventory came from the pinned upstream release JARs and observed Minecraft screens.')
    lock=json.loads((ROOT/'compatibility/native-windows-1.20.1.json').read_text())
    names={'bartering-station':'Bartering Station','alloy-forgery':'Alloy Forgery','fabric-seasons-extras':'Fabric Seasons: Extras',
           'alloyed':'Alloyed','farmers-delight':"Farmer's Delight / Refabricated",'miners-delight':"Miner's Delight",'better-archeology':'Better Archeology',
           'illager-invasion':'Illager Invasion','galosphere':'Galosphere','sawmill':'Sawmill'}
    urls={}
    for target in lock['targets']:
        for integration in target['integrations']:
            urls.setdefault(integration['family'],[]).append((target['loader'],integration))
    y=463
    for family,entries in urls.items():
        r.text(names[family],32,y,11,INK,'Helvetica-Bold');x=300
        for loader,entry in entries:
            label=loader.title()+' / '+entry['version'];r.text(label,x,y,10,TEAL)
            link='https://modrinth.com/mod/'+entry['project']+'/version/'+entry['version_id']
            r.pdf.linkURL(link,(x,y-3,min(W-32,x+stringWidth(label,'Helvetica',10)),y+12),relative=0)
            x+=246
        y-=30
    r.text('Build and screenshot records',32,135,12,INK,'Helvetica-Bold')
    r.text('Fabric: ten-final-fabric-09 and ten-recipes-fabric-10. Forge: ten-forge-03, ten-forge-04 and ten-recipes-forge-05. An additional Fabric fixture checked startup without the optional targets. Earlier failures remain in the local logs.',32,114,10,MUTED,width=778)
    r.text('Repository guide: docs/markdown/project-everything-ten-1.20.1.md\nRelease inventory: compatibility/native-windows-1.20.1.json',32,76,10,MUTED,width=778)
    r.pdf.save()
    (TEMP/'image-crops.json').write_text(json.dumps(r.crops,indent=2)+'\n')
    print(json.dumps({'pdf':str(OUTPUT),'pages':r.page,'real_image_crops':len(r.crops)}))


if __name__=='__main__':main()
