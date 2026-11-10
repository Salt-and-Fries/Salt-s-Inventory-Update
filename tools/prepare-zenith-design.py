"""Native-only capture observer; move the real GUI pointer clear of the artwork."""
from pathlib import Path
import importlib.util,sys
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('zenith_native',ROOT/'tools/prepare-production-fabric-audit.py');prep=importlib.util.module_from_spec(spec);spec.loader.exec_module(prep)
original=prep.qa_source
mapping=prep.MC_CACHE/'loom.mappings.1_20_1.layered+hash.2198-v2/mappings.tiny'
lines=mapping.read_text().splitlines();fields={};inside=False
for line in lines:
    s=line.split('\t')
    if s[0]=='c':inside=s[-1]=='net/minecraft/client/MouseHandler'
    elif inside and len(s)>4 and s[1]=='f':fields[s[-1]]=s[-2]


def source(side):
    text=original(side);needle='case "move"->{mc.screen.mouseMoved('
    replacement='''case "move"->{
        try{
            var px=mc.mouseHandler.getClass().getDeclaredField("XFIELD");px.setAccessible(true);
            var py=mc.mouseHandler.getClass().getDeclaredField("YFIELD");py.setAccessible(true);
            px.setDouble(mc.mouseHandler,c.get("x").getAsDouble()*mc.getWindow().getWidth()/mc.getWindow().getGuiScaledWidth());
            py.setDouble(mc.mouseHandler,c.get("y").getAsDouble()*mc.getWindow().getHeight()/mc.getWindow().getGuiScaledHeight());
        }catch(Exception e){throw new IllegalStateException(e);}
        mc.screen.mouseMoved('''
    assert needle in text;return text.replace(needle,replacement.replace('XFIELD',fields['xpos']).replace('YFIELD',fields['ypos']))


if __name__=='__main__':prep.qa_source=source;prep.main()
