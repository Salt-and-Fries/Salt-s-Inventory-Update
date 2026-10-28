from PIL import Image,ImageDraw,ImageFont
from pathlib import Path
from zipfile import ZipFile
import json
p=Path('output/simply-swords-design');a=p/'assets';o=p/'mockups';o.mkdir(exist_ok=True)
src=Image.open(a/'runic_forge.png').convert('RGBA');body=src.crop((0,16,200,74));# Replace only the inventory-bag lip in y=68..73 with nearby native stone;
# retain the original gold tablet pixels in those rows.
for yy in range(52,58):
 for xx in range(200):
  original=body.getpixel((xx,yy));r,g,b,al=original
  is_gold=any(29+18*i<=xx<45+18*i for i in range(8)) and r>110 and g>65 and r>b*1.3
  if not is_gold:body.putpixel((xx,yy),src.getpixel((xx,42+(yy-52))))
body.save(o/'native-body-cleaned.png')
font=ImageFont.truetype('C:/Windows/Fonts/consola.ttf',9); controls=Image.open('output/automatic-window-bars-design/assets/window-controls.png').convert('RGBA')
window=Image.open('artwork/shared-resources/core/assets/salts_inventory_update/textures/gui/window.png').convert('RGBA')
def frame(width,height,dark=False):
 image=window.copy()
 if dark:
  image.putdata([(int(r*.37),int(g*.36),int(b*.39),a) for r,g,b,a in image.getdata()])
 result=Image.new('RGBA',(width,height));xc=[0,4,7,11];yc=[0,4,7,11];dx=[0,4,width-4,width];dy=[0,4,height-4,height]
 for j in range(3):
  for i in range(3):result.alpha_composite(image.crop((xc[i],yc[j],xc[i+1],yc[j+1])).resize((dx[i+1]-dx[i],dy[j+1]-dy[j]),Image.Resampling.NEAREST),(dx[i],dy[j]))
 return result
def draw_top(im,title,dark):
 d=ImageDraw.Draw(im);d.text((7,5),title,font=font,fill='#f5e6cc' if dark else '#222222')
 for i,col in enumerate((3,2)):im.alpha_composite(controls.crop((col*11,0,col*11+11,11)),(im.width-26+i*12,4))
def produce(mode,seeded=False):
 width=212;height=88 if mode in ('bars','hybrid') else 84
 if mode in ('bars','hybrid'):
  im=Image.new('RGBA',(width,height));im.alpha_composite(body,(6,20));im.alpha_composite(frame(width,20,mode=='hybrid'),(0,0));im.alpha_composite(frame(width,10,mode=='hybrid'),(0,78))
 else:
  im=frame(width,height,mode=='dark');im.alpha_composite(body,(6,20))
 draw_top(im,'Runic Forge',mode in ('dark','hybrid'))
 if seeded:
  image=Image.open(p/'native/native-forge-seeded.png').convert('RGBA')
  for x,y in [(92,22),(33,22),(151,22),(29,56)]:
   crop=image.crop(((220+x)*2,(134+y)*2,(220+x+16)*2,(134+y+16)*2)).resize((16,16),Image.Resampling.NEAREST)
   im.alpha_composite(crop,(6+x,20+y-16))
 im.resize((width*3,height*3),Image.Resampling.NEAREST).save(o/(mode+('-seeded' if seeded else '-empty')+'.png'))
for name in ['gray','bars','dark']:produce(name)
produce('dark',True)
produce('hybrid')
produce('hybrid',True)
rows=[Image.open(o/(n+'-empty.png')) for n in ['gray','bars','dark']];contact=Image.new('RGBA',(636,264*3),(246,246,246,255))
for i,im in enumerate(rows):contact.alpha_composite(im,(0,i*264))
contact.convert('RGB').save(o/'alternatives-contact.png')
print('done')
