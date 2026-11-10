"""Compare all five rows of the live empty XP bar with the upstream sprite."""
from focused import Check
from pathlib import Path
from PIL import Image
from io import BytesIO
import json,sys,zipfile

c=Check(Path(sys.argv[1]))
c.call('wait',ticks=20)
c.capture('crystalline-flower-xp-bar-fixed-fullscreen')
point=c.point('crystalline_flower',11,99)
shot=Image.open(c.out/'screenshots/crystalline-flower-xp-bar-fixed-fullscreen.png').convert('RGB')
with zipfile.ZipFile(sys.argv[2]) as jar:
    atlas=Image.open(BytesIO(jar.read('assets/the_bumblezone/textures/gui/container/crystallized_flower.png'))).convert('RGBA')
sx=shot.width/c.state['guiWidth'];sy=shot.height/c.state['guiHeight']
rows=[]
for y in range(5):
    checked=0;mismatches=[]
    for x in range(54):
        expected=atlas.getpixel((176+x,187+y))
        if expected[3]!=255:continue
        pixel=(int((point['x']+x+.5)*sx),int((point['y']+y+.5)*sy))
        actual=shot.getpixel(pixel);checked+=1
        if actual!=expected[:3]:mismatches.append({'x':x,'actual':actual,'expected':expected[:3]})
    rows.append({'row':y,'opaque_pixels_checked':checked,'mismatches':mismatches})
(c.folder/'xp-bar-native-pixel-comparison.json').write_text(json.dumps(rows,indent=2))
c.passed('flower-all-five-xp-bar-rows-match-native-sprite',all(not row['mismatches'] for row in rows))
print(json.dumps(rows,indent=2))
