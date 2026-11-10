"""Translate native GUI event/pose contracts without changing window composition."""
from pathlib import Path
import json,re,importlib.util
ROOT=Path(__file__).resolve().parents[1]

def calls(text,name,change):
    token='.'+name+'(';offset=0
    while (start:=text.find(token,offset))>=0:
        begin=start+len(token);end=begin;depth=1;quoted=False;escaped=False;args=[];last=begin
        while depth:
            c=text[end]
            if quoted:
                if escaped:escaped=False
                elif c=='\\':escaped=True
                elif c=='"':quoted=False
            else:
                if c=='"':quoted=True
                elif c in '([{':depth+=1
                elif c in ')]}':depth-=1
                elif c==',' and depth==1:args.append(text[last:end]);last=end+1
            end+=1
        args.append(text[last:end-1]);new=change(args)
        if new is not None:text=text[:start]+'.'+name+'('+new+')'+text[end:];offset=start+len(name)+len(new)+3
        else:offset=end
    return text

def main():
    for v in ['1.21.1','1.21.11','26.1.2','26.2','26.3']:
        root=ROOT/f'compatibility/versions/{v}/fabric/src/main/java/com/salts_inventory_update/compat'
        p=root/'travelersbackpack/TravelersPackets.java';s=p.read_text().replace('c.sendCustomPayload(','c.sendPayload(');p.write_text(s)
        if v=='1.21.1':continue
        # Botania has no exact publisher release on these targets. Its old GL
        # alpha helper is deliberately absent; native render-state capture owns guides.
        p=root/'nativewindows/ReviewedNativeWindows.java';s=p.read_text();s='\n'.join(l for l in s.splitlines() if 'ReviewedPouchFlowers.render' not in l)+'\n';p.write_text(s)
        for name in ['ReviewedPouchFlowers.java','RecessedNativeChrome.java']:
            p=root/'nativewindows'/name
            if p.exists():p.unlink() # Rollout-created files, retained in the 1.20.1 reference.
        p=root/'tieredz/GearClientMenus.java';p.parent.mkdir(exist_ok=True)
        p.write_bytes((ROOT/'compatibility/versions/1.21.1/fabric/src/main/java/com/salts_inventory_update/compat/tieredz/GearClientMenus.java').read_bytes())
        for p in root.rglob('*.java'):
            if p.parts[-2] not in ['nativewindows','mixin','tomstrading','tradingpost','travelersbackpack','aircraft','bumblezone','graves','tieredz']:continue
            s=p.read_text()
            s=re.sub(r'\.isClientSide(?![\w(])','.isClientSide()',s)
            s=s.replace('.getAllKeys()', '.keySet()')
            for method,default in [('Byte','(byte)0'),('Int','0'),('Long','0L'),('String','""'),('Boolean','false'),('Float','0f'),('Double','0d')]:
                s=re.sub(r'(\b(?:tag|state|data|result|entry|actionTag)\.get'+method+r')\(("[^"]*")\)',r'\1Or(\2,'+default+')',s)
            s=s.replace('tag.getCompound(', 'tag.getCompoundOrEmpty(')
            if p.name in ['OwoScissorMixin.java','ResourcefulScissorMixin.java']:
                s=s.replace('getGuiScale()D','getGuiScale()I').replace('double salts_','int salts_').replace('private static double scale(', 'private static int scale(')
                s=s.replace('.map(DesktopNativeRenderTarget::guiScale)', '.map(t->(int)Math.round(t.guiScale()))')
            s=s.replace('.pose().pushPose()', '.pose().pushMatrix()').replace('.pose().popPose()', '.pose().popMatrix()')
            s=re.sub(r'(\.pose\(\)\.translate\([^;\n]*),0\)',r'\1)',s)
            for name in ['mouseClicked','mouseReleased','mouseDragged']:
                def mouse(args):
                    n=3 if name!='mouseDragged' else 5
                    if len(args)!=n or args[2].strip()!='e.button()':return None
                    event='new net.minecraft.client.input.MouseButtonEvent('+args[0]+','+args[1]+',e.buttonInfo())'
                    return event+(',twice' if name=='mouseClicked' else ','+','.join(args[3:]) if name=='mouseDragged' else '')
                s=calls(s,name,mouse)
            s=calls(s,'keyPressed',lambda a:'e' if len(a)==3 and 'e.key()' in a[0] else None)
            s=calls(s,'charTyped',lambda a:'e' if len(a)==2 and 'e.codepoint()' in a[0] else None)
            s=s.replace('InventoryMenu.BLOCK_ATLAS','net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS')
            if v.startswith('26.'):
                s=s.replace('.render(c.nativeGraphics(),','.extractRenderState(c.nativeGraphics(),').replace('box.render(graphics,','box.extractRenderState(graphics,')
                s=s.replace('.render(c.nativeGraphics(),', '.extractRenderState(c.nativeGraphics(),')
            p.write_text(s)
        # Render-state capture uses the actual framebuffer, unlike legacy GL targets.
        p=ROOT/f'versions/{v}/fabric/src/main/java/com/salts_inventory_update/client/automatic/AutomaticNativeScope.java';s=p.read_text()
        a=s.index('        var b=f.owner().buffer;',s.index('captureTarget()'));b=s.index('\n    }',a)
        s=s[:a]+'''        var window=net.minecraft.client.Minecraft.getInstance().getWindow();
        return java.util.Optional.of(new com.salts_inventory_update.api.client.desktop.DesktopNativeRenderTarget(
            window.getWidth(),window.getHeight(),window.getGuiScale()));'''+s[b:];p.write_text(s)
    print('Updated event, NBT and render-state widget contracts.')

if __name__=='__main__':main()
