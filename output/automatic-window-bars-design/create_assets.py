from PIL import Image,ImageDraw,ImageFont
from pathlib import Path
import zipfile,json,shutil
out=Path('output/automatic-window-bars-design');assets=out/'assets';assets.mkdir(exist_ok=True)
root=Path('output/project-everything-eight-mockups'); chipped=Path('output/project-everything-chipped-design/native-crops/forge-forge-mason-clear.png')
sketch=Path('.codex-attachments/7ad23c5a-e0b0-403a-a589-707dc7c3024d/59653fc9-a356-4f48-88fa-df9705647ddb/40874.jpg')
shutil.copy2(sketch,assets/'user-sketch.jpg')
with zipfile.ZipFile('dist/core/salts_inventory_update-1.20.1-Fabric-0.1.6.jar') as z:(assets/'window-controls.png').write_bytes(z.read('assets/salts_inventory_update/textures/gui/window_controls.png'))
controls=Image.open(assets/'window-controls.png').convert('RGBA')
font=ImageFont.truetype('C:/Windows/Fonts/consola.ttf',20)
def make_bar(width,height):
 src=Image.open('artwork/shared-resources/core/assets/salts_inventory_update/textures/gui/window.png').convert('RGBA')
 # SIU's actual 11 x 11 corner texture, expanded with nearest-neighbor nine-slicing.
 w,h=width//2,height//2; dest=Image.new('RGBA',(w,h),(0,0,0,0)); xcuts=[0,4,7,11];ycuts=[0,4,7,11];dx=[0,4,w-4,w];dy=[0,4,h-4,h]
 for j in range(3):
  for i in range(3):
   piece=src.crop((xcuts[i],ycuts[j],xcuts[i+1],ycuts[j+1]));piece=piece.resize((dx[i+1]-dx[i],dy[j+1]-dy[j]),Image.Resampling.NEAREST);dest.alpha_composite(piece,(dx[i],dy[j]))
 return dest.resize((width,height),Image.Resampling.NEAREST)
def mock(name,source,crop,title):
 im=Image.open(source).convert('RGBA').crop(crop)
 # Native crop retains 2x image resolution; Chipped is 3x and is reduced uniformly.
 if name=='workshop':im=im.resize((512,252),Image.Resampling.NEAREST)
 w,h=im.size;over=12;barh=34;canvas=Image.new('RGBA',(w+2*over,h+2*barh),(0,0,0,0));canvas.alpha_composite(im,(over,barh));canvas.alpha_composite(make_bar(w+2*over,barh),(0,0));canvas.alpha_composite(make_bar(w+2*over,barh),(0,barh+h));d=ImageDraw.Draw(canvas);d.text((14,7),title,font=font,fill='#171717',stroke_width=0)
 # Unmodified SIU atlas controls; magnified exactly 2x.
 for i,col in enumerate((3,2)):
  icon=controls.crop((col*11,0,col*11+11,11)).resize((22,22),Image.Resampling.NEAREST);canvas.alpha_composite(icon,(w+2*over-54+i*25,6))
 canvas.save(assets/(name+'-mockup.png'));im.save(assets/(name+'-native-body.png'))
 return {'source':str(source),'source_crop':crop,'title':title,'body_size':list(im.size),'proposal':str(assets/(name+'-mockup.png'))}
# Screenshots are visual references; these named integrations are not targeted for changes.
rows=[]
rows.append(mock('workshop',chipped,(0,72,768,450),'Example Workshop'))
rows.append(mock('ossuary',root/'ossuary-recipes-native-crop.png',(8,36,360,150),'Example Carving'))
rows.append(mock('smelter',root/'end-smelter-closed-native-crop.png',(8,36,360,150),'Example Smelter'))
kiln=Path('build/native-container-autohost/gameplay/1.20.1-forge-automatic-forge-a3/screenshots/core-only-native-kiln.png');im=Image.open(kiln);im.crop((462,169,818,391)).save(assets/'kiln-current.png')
rows.append(mock('kiln',kiln,(464,202,816,385),'Kiln'))
(assets/'manifest.json').write_text(json.dumps({'sketch':str(sketch),'current_kiln':str(kiln),'proposals':rows,'note':'Composites are static design mockups, not implemented or evidence of live behavior.'},indent=2))
print('assets complete')
