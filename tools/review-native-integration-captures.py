"""Build contact sheets from actual, unaltered gameplay captures for visual review."""
import argparse,json
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont

ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('records',type=Path);p.add_argument('--prefix',required=True);args=p.parse_args()
rows=json.loads(args.records.read_text(encoding='utf-8-sig'))
dest=ROOT/'build/project-everything-56-implementation/reviews';dest.mkdir(parents=True,exist_ok=True)
font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',18)
for start in range(0,len(rows),8):
    sheet=Image.new('RGB',(1400,1120),'#eef3f3');draw=ImageDraw.Draw(sheet)
    for n,r in enumerate(rows[start:start+8]):
        x=(n%2)*700;y=(n//2)*280;draw.text((x+12,y+8),r['id'],font=font,fill='#123c42')
        if not r.get('capture'):draw.text((x+12,y+45),r.get('error','No capture')[:75],font=font,fill='#9d2828');continue
        path=Path(r['capture']);im=Image.open(path).convert('RGB');state=r['observation']['client'];wins=state.get('allWindows')or[]
        rect=(0,0,im.width,im.height)
        if wins:
            b=wins[-1]['bounds'];sx=im.width/state['guiWidth'];sy=im.height/state['guiHeight'];rect=(max(0,int(b['x']*sx)-8),max(0,int(b['y']*sy)-8),min(im.width,int((b['x']+b['width'])*sx)+8),min(im.height,int((b['y']+b['height'])*sy)+8))
        crop=im.crop(rect);crop.thumbnail((676,235),Image.Resampling.NEAREST);sheet.paste(crop,(x+(700-crop.width)//2,y+38))
    target=dest/f'{args.prefix}-{start//8+1:02}.png';sheet.save(target);print(target)
