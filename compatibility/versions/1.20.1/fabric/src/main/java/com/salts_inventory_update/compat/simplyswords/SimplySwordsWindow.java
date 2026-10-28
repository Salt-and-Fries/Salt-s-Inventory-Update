package com.salts_inventory_update.compat.simplyswords;

import java.util.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Approved two charcoal bars around the native torn slab, using only the public desktop API. */
public final class SimplySwordsWindow implements DesktopWindowDefinition<AbstractContainerMenu,SimplySwordsWindow.State> {
    private static final ResourceLocation ART=new ResourceLocation("simplyswords","textures/gui/container/runic_forge.png");
    private static final ResourceLocation FRAME=new ResourceLocation("salts_inventory_compat","textures/gui/simplyswords/window.png");
    private static final ResourceLocation CONTROLS=new ResourceLocation("salts_inventory_update","textures/gui/window_controls.png");
    private static final DesktopWindowSurface SURFACE=new DesktopWindowSurface(DesktopSurfaceMode.SALT,new DesktopInsets(6,20,6,10),true,List.of(),DesktopViewportPolicy.CLAMP);
    private static final Set<State> STATES=Collections.newSetFromMap(new WeakHashMap<>());
    public static final class State {
        public final int[] counts=new int[11];
        public final long[] starts=new long[11];
        private ResourceLocation body;
        private int artWidth=200,artHeight=58;
        State(){Arrays.fill(counts,-1);}
        void release(){if(body!=null){Minecraft.getInstance().getTextureManager().release(body);body=null;}}
        ResourceLocation body(Minecraft client){
            if(body!=null)return body;
            try(var stream=client.getResourceManager().getResource(ART).orElseThrow().open()){
                byte[] bytes=stream.readAllBytes();
                // Unknown resource-pack artwork stays intact: the decorative cleanup
                // was reviewed against this native asset, not arbitrary recolored icons.
                boolean clean=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes))
                    .equals("df365f6a0ebf8b23eab4184153ab4a2adf7452109a417ca9d9965a628a275a1f");
                try(var source=NativeImage.read(new java.io.ByteArrayInputStream(bytes))){
                    float scaleX=source.getWidth()/256f,scaleY=source.getHeight()/256f;
                    artWidth=Math.max(1,Math.round(200*scaleX));artHeight=Math.max(1,Math.round(58*scaleY));
                    var image=new NativeImage(artWidth,artHeight,true);
                    for(int y=0;y<artHeight;y++)for(int x=0;x<artWidth;x++){
                        int nativeY=Math.min(source.getHeight()-1,y+Math.round(16*scaleY));
                        int color=source.getPixelRGBA(x,nativeY);
                        if(clean&&y>=52&&!SimplySwordsLayout.keepTabletPixel(x,color))color=source.getPixelRGBA(x,42+y-52);
                        image.setPixelRGBA(x,y,color);
                    }
                    body=client.getTextureManager().register("siu_runic_forge_body",new DynamicTexture(image));return body;
                }
            }catch(java.io.IOException|java.security.NoSuchAlgorithmException e){throw new IllegalStateException("Runic Forge native artwork unavailable",e);}
        }
    }
    static void reloadArt(){for(var s:List.copyOf(STATES))s.release();}
    @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c){var s=new State();STATES.add(s);return s;}
    @Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c){STATES.remove(c.state());c.state().release();}
    @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,State> c){return SURFACE;}
    @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractContainerMenu> c){return DesktopWindowSize.of(212,88);}
    @Override public DesktopWindowSize minSize(DesktopWindowContext<AbstractContainerMenu,State> c){return DesktopWindowSize.of(212,88);}
    @Override public DesktopWindowSize maxSize(DesktopWindowContext<AbstractContainerMenu,State> c){return minSize(c);}
    @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,State> c){return "simply-swords-forge-1";}
    @Override public DesktopWindowDecoration<AbstractContainerMenu,State> decoration(DesktopWindowContext<AbstractContainerMenu,State> c){return this::chrome;}
    private void chrome(DesktopRenderContext<AbstractContainerMenu,State> c,List<DesktopControlState> controls){
        c.windowNineSlice(FRAME,c.windowX(),c.windowY(),c.windowWidth(),20);
        c.windowNineSlice(FRAME,c.windowX(),c.windowY()+c.windowHeight()-10,c.windowWidth(),10);
        int right=controls.stream().filter(s->s.control()!=DesktopWindowControl.RESIZE_GRIP).mapToInt(s->s.bounds().x()-3).min().orElse(c.windowX()+c.windowWidth()-6);
        c.text(c.trimToWidth(c.originalTitle().getString(),Math.max(0,right-c.windowX()-7)),c.windowX()+7,c.windowY()+5,0xFFF5E6CC,false);
        boolean shift=c instanceof DesktopInputContext<?,?> input&&input.shiftDown();
        for(var s:controls){
            int column=switch(s.control()){case FOCUS->0;case SORT_INVENTORY->1;case CLOSE->2;case ELLIPSIS->3;case LOCK->s.selected()?5:4;case PIN->s.selected()?7:6;case LINK->9;case SORT_WINDOWS->shift?11:10;default->-1;};
            if(column<0)continue;var b=s.bounds();int row=s.pressed()||s.selected()&&s.control()!=DesktopWindowControl.LOCK?2:s.hovered()||s.keyboardFocused()?1:0;
            c.texture(CONTROLS,b.x(),b.y(),column*11,row*11,b.width(),b.height(),11,11,132,33);
        }
    }
    @Override public void tick(DesktopWindowContext<AbstractContainerMenu,State> c){
        var s=c.state();long now=Util.getMillis();
        for(int i=0;i<11;i++){int count=c.model().getSlot(i).getItem().getCount();if(s.counts[i]>=0&&count>s.counts[i]&&i!=0)s.starts[i]=now;s.counts[i]=count;}
    }
    @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> c){
        int x=c.contentX(),y=c.contentY();var s=c.state();long now=Util.getMillis();
        var body=s.body(c.minecraft());
        c.texture(body,x,y,0,0,200,58,s.artWidth,s.artHeight,s.artWidth,s.artHeight);
        for(int i=1;i<11;i++){
            long elapsed=now-s.starts[i];if(s.starts[i]==0||elapsed<0||elapsed>800)continue;
            int sx=SimplySwordsLayout.slotX(i)+8,sy=SimplySwordsLayout.slotY(i)+8;
            if(i>=3)channel(c,elapsed/800f,x,y-16,sx,sy,sx,50,100,50,100,30);
            else channel(c,elapsed/800f,x,y-16,sx,sy,100,30);
        }
        for(int i=0;i<11;i++)c.texturelessSlot(i,x+SimplySwordsLayout.slotX(i),y+SimplySwordsLayout.slotY(i)-16);
        c.rawGraphics().nextStratum();boolean weaponGlow=false;
        for(int i=1;i<11;i++){
            long elapsed=now-s.starts[i];if(s.starts[i]==0||elapsed<0||elapsed>800)continue;
            float pulse=.45f+.55f*(float)Math.sin(elapsed*.025);
            outline(c,x+SimplySwordsLayout.slotX(i)-1,y+SimplySwordsLayout.slotY(i)-17,alpha(0xCC74E7FF,pulse));weaponGlow|=elapsed>440;
        }
        if(weaponGlow)outline(c,x+91,y+5,0xCC74E7FF);
    }
    // Mirrors the upstream screen's 800ms channel path and pulse; the handler still owns all game state.
    private static void channel(DesktopRenderContext<?,?> c,float progress,int ox,int oy,int... points){
        var samples=new ArrayList<int[]>();
        for(int p=0;p+3<points.length;p+=2){int dx=points[p+2]-points[p],dy=points[p+3]-points[p+1],steps=Math.max(Math.abs(dx),Math.abs(dy));
            for(int i=p==0?0:1;i<=steps;i++){float t=steps==0?1:i/(float)steps;samples.add(new int[]{Math.round(points[p]+dx*t),Math.round(points[p+1]+dy*t)});}}
        if(samples.isEmpty())return;int head=Math.max(0,Math.min(samples.size()-1,Math.round((samples.size()-1)*progress))),tail=Math.max(0,head-16);
        for(int i=tail;i<=head;i++){var p=samples.get(i);int x=ox+p[0],y=oy+p[1];float a=(i-tail+1)/(float)Math.max(1,head-tail+1);c.fill(x-1,y-1,x+2,y+2,alpha(0x5574E7FF,a));c.fill(x,y,x+1,y+1,alpha(0xCC74E7FF,a));}
    }
    private static int alpha(int color,float factor){return Math.max(0,Math.min(255,Math.round((color>>>24)*factor)))<<24|color&0xFFFFFF;}
    private static void outline(DesktopRenderContext<?,?> c,int x,int y,int color){c.fill(x,y,x+18,y+1,color);c.fill(x,y+17,x+18,y+18,color);c.fill(x,y,x+1,y+18,color);c.fill(x+17,y,x+18,y+18,color);}
    @Override public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu,State> c,double x,double y){
        for(int i=0;i<11;i++){int sx=c.contentX()+SimplySwordsLayout.slotX(i),sy=c.contentY()+SimplySwordsLayout.slotY(i)-16;
            if(x>=sx-1&&x<sx+17&&y>=sy-1&&y<sy+17)return DesktopSlotHit.of(i,sx,sy);}
        return null;
    }
    @Override public boolean appendTooltip(DesktopRenderContext<AbstractContainerMenu,State> c,int x,int y){
        for(int i=0;i<11;i++){
            int sx=c.contentX()+SimplySwordsLayout.slotX(i),sy=c.contentY()+SimplySwordsLayout.slotY(i)-16;
            if(x<sx-1||x>=sx+17||y<sy-1||y>=sy+17)continue;
            var stack=i==0?(ItemStack)ForeignMenuAccess.invoke(c.model(),"getPreviewStack"):c.model().getSlot(i).getItem();
            if(stack.isEmpty())stack=c.model().getSlot(i).getItem();if(stack.isEmpty())return false;c.tooltip(stack,x,y);return true;
        }return false;
    }
}
