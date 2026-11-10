"""Compare extracted artwork with the exact modern publisher textures."""
from pathlib import Path
from io import BytesIO
import argparse,json,zipfile
from PIL import Image
from verify_artwork import CASES


def main():
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);p.add_argument('--jar',required=True,type=Path);a=p.parse_args();checks=[]
    with zipfile.ZipFile(a.jar)as jar:
        def texture(path):return Image.open(BytesIO(jar.read('assets/aether/textures/'+path+'.png'))).convert('RGBA')
        for name,rects in CASES.items():
            case='book_of_lore'if name=='lore'else name;capture='aether-lore-empty'if name=='lore'else'aether-'+name+'-empty'
            d=json.loads((a.output/'aether-checks'/f'{capture}-state.json').read_text());w=next(w for w in d['allWindows']if w.get('menuId')=='aether:'+case);plan=w['composition'];image=Image.open(a.output/'screenshots'/f'{capture}.png').convert('RGBA');source=texture('gui/menu/'+('lore_book'if name=='lore'else name))
            for sx,sy,width,height in rects:
                r=next(r for r in plan['regions']if r['source']['x']<=sx and r['source']['y']<=sy and r['source']['x']+r['source']['width']>=sx+width and r['source']['y']+r['source']['height']>=sy+height)
                x=w['bounds']['x']+7+plan['padding']+r['x']+sx-r['source']['x'];y=w['bounds']['y']+26+plan['padding']+r['y']+sy-r['source']['y']
                actual=image.crop((x*2,y*2,(x+width)*2,(y+height)*2)).resize((width,height),Image.Resampling.NEAREST);ox,oy=(12,2)if name=='lore'else(0,0);expected=source.crop((sx-ox,sy-oy,sx-ox+width,sy-oy+height))
                deltas=[max(abs(v-u)for v,u in zip(p,q))for p,q in zip(actual.getdata(),expected.getdata())];checks.append(dict(case=case,rect=[sx,sy,width,height],pixels=width*height,mismatches=sum(v>1 for v in deltas),maximum_delta=max(deltas)))
        d=json.loads((a.output/'aether-checks/character-aether-empty-state.json').read_text());w=next(w for w in d['workspaceWindows']if w['kind']=='CHARACTER');c=w['content'];image=Image.open(a.output/'screenshots/character-aether-empty.png').convert('RGBA');body=texture('gui/inventory/accessories')
        for cell in w['aetherCells']:
            sx,sy=76+cell['x'],7+cell['y'];expected=body.crop((sx,sy,sx+18,sy+18))
            icon=cell['icon'];path=next(v for v in icon.values()if isinstance(v,str)and'/'in v);expected.alpha_composite(texture(path),(1,1))
            x,y=c['x']+sx,c['y']+sy;actual=image.crop((x*2,y*2,(x+18)*2,(y+18)*2)).resize((18,18),Image.Resampling.NEAREST)
            # The live native checkbox overlaps the frame's upper-right corner;
            # inspect that native widget separately, rather than comparing it to
            # the bare slot atlas. Everything else includes the complete glyph.
            toggles=[(88+other['x'],5+other['y'])for other in w['aetherCells']if other['toggle']]
            deltas=[max(abs(v-u)for v,u in zip(actual.getpixel((x,y)),expected.getpixel((x,y))))for y in range(18)for x in range(18)if not any(tx<=sx+x<tx+5 and ty<=sy+y<ty+5 for tx,ty in toggles)]
            checks.append(dict(case='character',slot=cell['identifier'],index=cell['index'],pixels=len(deltas),mismatches=sum(v>1 for v in deltas),maximum_delta=max(deltas)))
    result=dict(scope='Nine complete workstation/Lore slot frames, full Incubator meter, and eight Character equipment frames/glyphs; overlapping native visibility widgets excluded',channel_tolerance=1,checks=checks,pixels=sum(c['pixels']for c in checks),failures=sum(c['mismatches']>0 for c in checks))
    (a.output/'aether-checks/modern-artwork.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2));assert result['failures']==0


if __name__=='__main__':main()
