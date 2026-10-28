package com.salts_inventory_update.client.automatic;

import java.util.*;
import java.util.function.Supplier;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.*;
import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.mixin.client.AutomaticContainerScreenAccessor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Opt-in wrapper: ordinary API definitions and unconfigured automatic windows never enter this renderer. */
@SuppressWarnings({"rawtypes","unchecked"})
public final class NativeTemplateWindow extends AutomaticNativeWindow {
    final DesktopNativeTemplate template;
    final State initial;
    Object consumerState;
    DesktopNativeView view;
    DesktopNativeLayout plan;
    int scrollX,scrollY;
    boolean failed;
    String layoutError="";
    DesktopNativeLayout.Point pointerSource;
    boolean hostScroll;
    boolean capturedBehindFrame;
    public boolean inspecting;
    final NativeWindowInspector inspector = new NativeWindowInspector(this);

    @Override protected boolean usesAutomaticBars() { return false; }

    public NativeTemplateWindow(State state,DesktopNativeTemplate template) { super(state);initial=state;this.template=template;view=snapshot(state); }
    public void enableInspector(){inspector.start();}
    private DesktopNativeView snapshot(State s) {
        var a=(AutomaticContainerScreenAccessor)s.screen;
        var slots=new ArrayList<DesktopNativeView.Slot>();
        for(int i=0;i<s.screen.getMenu().slots.size();i++){
            var slot=s.screen.getMenu().getSlot(i);
            slots.add(new DesktopNativeView.Slot(i,new DesktopWindowBounds(slot.x-1,slot.y-1,18,18),
                com.salts_inventory_update.api.desktop.DesktopMenuBindings.isPlayerInventorySlot(s.screen.getMenu(),slot,net.minecraft.client.Minecraft.getInstance().player.getInventory()),slot.isActive()));
        }
        var widgets=new ArrayList<DesktopNativeView.Widget>();int index=0;
        for(var child:s.screen.children())if(child instanceof AbstractWidget w){
            if(w.getWidth()>0&&w.getHeight()>0)widgets.add(new DesktopNativeView.Widget("widget/"+index,w.getClass().getName(),w.getMessage().copy(),new DesktopWindowBounds(
                w.getX()-a.salts_inventory_update$nativeLeft(),w.getY()-a.salts_inventory_update$nativeTop(),w.getWidth(),w.getHeight()),w.visible,w.active));index++;
        }
        var l=s.layout;
        return new DesktopNativeView(s.screen.getClass().getName(),new DesktopWindowBounds(0,0,Math.max(1,a.salts_inventory_update$nativeWidth()),Math.max(1,a.salts_inventory_update$nativeHeight())),
            new DesktopWindowBounds(l.x(),l.y(),l.width(),l.height()),slots,widgets);
    }
    private <C> C extension(DesktopWindowContext<?,?> c) { return NativeTemplateContexts.view(c,consumerState,view); }
    private <R> R call(Supplier<R> callback,R fallback) {
        if(template==null||failed)return fallback;
        try{return callback.get();}catch(RuntimeException failure){failed=true;DesktopDebug.warn("native template callback failed screen={} reason={}",view.screenType(),failure.toString());return fallback;}
    }
    private void run(Runnable callback){call(()->{callback.run();return true;},false);}
    @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c) {
        State s=super.createState(c);if(template!=null)consumerState=call(()->template.createState(c),null);return s;
    }
    @Override public void prepareLayout(DesktopWindowContext<AbstractContainerMenu,State> c) {
        super.prepareLayout(c);view=snapshot(c.state());
        run(()->template.prepareLayout(extension(c)));
        try {
            DesktopNativeLayout next=inspector.override(view);
            if(next==null)next=call(()->template.nativeLayout(extension(c)),DesktopNativeLayout.automatic(view.automaticBounds()));
            plan=Objects.requireNonNull(next,"native layout");
            for(var slot:view.slots())if(!slot.player()&&slot.active())for(var region:plan.regions())
                if(!region.covers(slot.bounds())&&region.pieces().stream().anyMatch(piece->DesktopNativeRegion.intersects(piece,slot.bounds())))
                    throw new IllegalArgumentException("Region "+region.id()+" partially clips real slot "+slot.menuSlotId()+"; include or hide the complete cell");
            layoutError="";
        } catch(RuntimeException failure) {
            if(!layoutError.equals(failure.toString()))DesktopDebug.warn("native template layout declined: {}",failure.toString());
            layoutError=failure.toString();plan=DesktopNativeLayout.automatic(view.automaticBounds());
        }
        var viewport=viewport(c);
        scrollX=Math.max(0,Math.min(scrollX,plan.width()-viewport.width()));
        scrollY=Math.max(0,Math.min(scrollY,plan.height()-viewport.height()));
        if(c.state().screen.getFocused() instanceof AbstractWidget widget){
            var a=(AutomaticContainerScreenAccessor)c.state().screen;
            if(widget.getWidth()>0&&widget.getHeight()>0){var bounds=new DesktopWindowBounds(widget.getX()-a.salts_inventory_update$nativeLeft(),widget.getY()-a.salts_inventory_update$nativeTop(),widget.getWidth(),widget.getHeight());
                if(!widget.visible||!widget.active||plan.placements(bounds).stream().noneMatch(DesktopNativeLayout.Placement::interactive))c.state().screen.setFocused(null);
            }
        }
    }
    DesktopNativeLayout layout() { return plan==null?DesktopNativeLayout.automatic(view.automaticBounds()):plan; }
    private NativeTemplateViewport viewport(DesktopWindowContext<?,?> c) {
        var l=layout();return NativeTemplateViewport.resolve(l.width(),l.height(),c.contentWidth(),c.contentHeight(),l.scrollable());
    }
    private DesktopWindowSize size(DesktopWindowContext<?,?> c) {
        var l=layout();var inset=c.surface().insets();
        int width=l.width(),height=l.height();
        if(l.scrollable()){
            int top=0,bottom=0,left=0,right=0;
            for(var r:externalRegions(c))if(r.constrainsWindow()){
                top=Math.max(top,-r.y());left=Math.max(left,-r.x());
                bottom=Math.max(bottom,r.y()+r.height()-c.windowHeight());right=Math.max(right,r.x()+r.width()-c.windowWidth());
            }
            int[] fit=NativeTemplateViewport.fit(width,height,c.environment().viewportWidth()-inset.horizontal()-left-right,
                c.environment().viewportHeight()-inset.vertical()-top-bottom);
            width=fit[0];height=fit[1];
        }
        return inset.surfaceSize(width,height);
    }
    @Override public DesktopWindowSize minSize(DesktopWindowContext<AbstractContainerMenu,State> c){return size(c);}
    @Override public DesktopWindowSize maxSize(DesktopWindowContext<AbstractContainerMenu,State> c){return size(c);}
    @Override public DesktopWindowSize desiredWindowSize(DesktopWindowContext<?,?> c){return size(c);}
    @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,State> c){return "native-template-1:"+layout().hashCode()+":"+inspecting+":"+call(()->template.layoutRevision(extension(c)),"");}
    @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,State> c){return call(()->template.surface(extension(c)),DesktopWindowSurface.SALT);}
    @Override public void tick(DesktopWindowContext<AbstractContainerMenu,State> c){super.tick(c);run(()->template.tick(extension(c)));}
    @Override public void opened(DesktopWindowContext<AbstractContainerMenu,State> c){run(()->template.opened(extension(c)));}
    @Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c){try{if(template!=null)template.closed(extension(c));}finally{super.closed(c);}}
    @Override public void moved(DesktopWindowContext<AbstractContainerMenu,State> c){run(()->template.moved(extension(c)));}
    @Override public void resized(DesktopWindowContext<AbstractContainerMenu,State> c){run(()->template.resized(extension(c)));}
    @Override public void loadLocalState(DesktopWindowContext<AbstractContainerMenu,State> c,CompoundTag tag){run(()->template.loadLocalState(extension(c),tag));}
    @Override public void saveLocalState(DesktopWindowContext<AbstractContainerMenu,State> c,CompoundTag tag){run(()->template.saveLocalState(extension(c),tag));}
    @Override public Component title(DesktopWindowContext<AbstractContainerMenu,State> c){return call(()->template.title(extension(c)),c.originalTitle());}
    @Override public DesktopWindowOptions options(DesktopWindowContext<AbstractContainerMenu,State> c){return call(()->template.options(extension(c)),DesktopWindowOptions.DEFAULT);}
    @Override public boolean modal(DesktopWindowContext<AbstractContainerMenu,State> c){return call(()->template.modal(extension(c)),false);}
    @Override public DesktopWindowDecoration decoration(DesktopWindowContext<AbstractContainerMenu,State> c){return call(()->template.decoration(extension(c)),null);}
    @Override public DesktopWindowAttachment attachment(DesktopWindowContext<AbstractContainerMenu,State> c){return call(()->template.attachment(extension(c)),null);}
    @Override public List<DesktopPopup> popups(DesktopWindowContext<AbstractContainerMenu,State> c){return call(()->template.popups(extension(c)),List.of());}
    @Override public boolean dismissPopup(DesktopWindowContext<AbstractContainerMenu,State> c,String id,DesktopPopup.Dismissal why){return call(()->template.dismissPopup(extension(c),id,why),false);}
    @Override public List<DesktopSurfaceControl> managementControls(DesktopWindowContext<AbstractContainerMenu,State> c){return call(()->template.managementControls(extension(c)),List.of());}
    @Override public void renderManagementControls(DesktopRenderContext<AbstractContainerMenu,State> c,List<DesktopControlState> controls){run(()->template.renderManagementControls(extension(c),controls));}
    @Override public void renderOverlay(DesktopRenderContext<AbstractContainerMenu,State> c){run(()->template.renderOverlay(extension(c)));if(inspecting)inspector.render(c);}
    @Override public List<DesktopWindowRegion> externalRegions(DesktopWindowContext<?,?> c){
        var list=new ArrayList<DesktopWindowRegion>();
        for(var r:layout().regions())if(r.exterior())for(var piece:r.pieces()){
            int x=r.x()+piece.x()-r.source().x(),y=r.y()+piece.y()-r.source().y();
            list.add(r.interactive()?DesktopWindowRegion.visualAndInteractive(x,y,piece.width(),piece.height()):DesktopWindowRegion.visual(x,y,piece.width(),piece.height()));
        }
        if(template instanceof DesktopWindowPresentation p)list.addAll(call(()->p.externalRegions(extension(c)),List.of()));
        if(inspecting)list.addAll(inspector.regions(c));return List.copyOf(list);
    }
    @Override public void renderBehindFrame(DesktopRenderContext<?,?> c){
        if(layout().regions().stream().anyMatch(DesktopNativeRegion::exterior)){
            capture((DesktopRenderContext)c);capturedBehindFrame=true;paintFragments((DesktopRenderContext)c,true);
        }
        if(template instanceof DesktopWindowPresentation p)run(()->p.renderBehindFrame(extension(c)));
    }
    @Override public void updateTooltipState(DesktopWindowContext<?,?> c,int x,int y,boolean active){if(template instanceof DesktopWindowPresentation p)run(()->p.updateTooltipState(extension(c),x,y,active));}
    @Override public void environmentChanged(DesktopWindowContext<AbstractContainerMenu,State> c,DesktopWindowEnvironment environment){run(()->template.environmentChanged(extension(c),environment));}
    @Override public void windowStateChanged(DesktopWindowContext<AbstractContainerMenu,State> c,DesktopWindowState state){run(()->template.windowStateChanged(extension(c),state));}
    @Override public void gestureChanged(DesktopWindowContext<AbstractContainerMenu,State> c,DesktopGestureEvent event){run(()->template.gestureChanged(extension(c),event));}
    @Override public void focusChanged(DesktopWindowContext<AbstractContainerMenu,State> c,boolean focused){run(()->template.focusChanged(extension(c),focused));}
    @Override public void keyboardFocusChanged(DesktopWindowContext<AbstractContainerMenu,State> c,boolean focused){super.keyboardFocusChanged(c,focused);run(()->template.keyboardFocusChanged(extension(c),focused));}
    @Override public boolean focusNext(DesktopWindowContext<AbstractContainerMenu,State> c,boolean forward){
        if(call(()->template.focusNext(extension(c),forward),false))return true;
        var a=(AutomaticContainerScreenAccessor)c.state().screen;var available=new ArrayList<AbstractWidget>();
        for(var child:c.state().screen.children())if(child instanceof AbstractWidget w&&w.visible&&w.active&&w.getWidth()>0&&w.getHeight()>0){
            var b=new DesktopWindowBounds(w.getX()-a.salts_inventory_update$nativeLeft(),w.getY()-a.salts_inventory_update$nativeTop(),w.getWidth(),w.getHeight());
            if(layout().placements(b).stream().anyMatch(DesktopNativeLayout.Placement::interactive))available.add(w);
        }
        if(available.isEmpty())return false;int i=available.indexOf(c.state().screen.getFocused());c.state().screen.setFocused(available.get(Math.floorMod(i+(forward?1:-1),available.size())));return true;
    }
    @Override public void narrate(DesktopWindowContext<AbstractContainerMenu,State> c,net.minecraft.client.gui.narration.NarrationElementOutput output){if(template==null)super.narrate(c,output);else run(()->template.narrate(extension(c),output));}
    @Override public void cancelPointer(DesktopWindowContext<?,?> c){super.cancelPointer(c);pointerSource=null;hostScroll=false;inspector.cancel();if(template instanceof DesktopWindowPresentation p)run(()->p.cancelPointer(extension(c)));}
    private AutomaticNativeScope.Frame inputFrame(DesktopWindowContext<?,State> c,DesktopInputContext<?,?> input) {
        var a=(AutomaticContainerScreenAccessor)c.state().screen;var l=c.state().layout;
        return new AutomaticNativeScope.Frame(c.state().screen,c.state(),null,input,l,0,0);
    }
    Optional<DesktopNativeLayout.Point> sourceAt(DesktopWindowContext<?,?> c,double x,double y) {
        var external=layout().exteriorSourceAt(x-c.windowX(),y-c.windowY());if(external.isPresent())return external;
        var vp=viewport(c);
        if(x<c.contentX()||y<c.contentY()||x>=c.contentX()+vp.width()||y>=c.contentY()+vp.height())return Optional.empty();
        return layout().sourceAt(x-c.contentX()+scrollX,y-c.contentY()+scrollY);
    }
    private int regionX(DesktopWindowContext<?,?> c,DesktopNativeRegion r){return r.exterior()?c.windowX()+r.x():c.contentX()+layout().padding()+r.x()-scrollX;}
    private int regionY(DesktopWindowContext<?,?> c,DesktopNativeRegion r){return r.exterior()?c.windowY()+r.y():c.contentY()+layout().padding()+r.y()-scrollY;}
    private double nativeX(State s,double x){return ((AutomaticContainerScreenAccessor)s.screen).salts_inventory_update$nativeLeft()+x;}
    private double nativeY(State s,double y){return ((AutomaticContainerScreenAccessor)s.screen).salts_inventory_update$nativeTop()+y;}
    @Override public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu,State> c,double x,double y){
        if(inspecting)return null;DesktopSlotHit custom=call(()->template.slotAt(extension(c),x,y),null);if(custom!=null)return custom;
        var point=sourceAt(c,x,y);if(point.isEmpty())return null;
        for(var slot:view.slots())if(!slot.player()&&slot.active()&&DesktopNativeRegion.contains(slot.bounds(),point.get().x(),point.get().y())){
            var region=layout().regions().stream().filter(r->r.id().equals(point.get().regionId())).findFirst().orElseThrow();
            if(!region.covers(slot.bounds()))return null;
            return DesktopSlotHit.of(slot.menuSlotId(),regionX(c,region)+slot.bounds().x()-region.source().x()+1,
                regionY(c,region)+slot.bounds().y()-region.source().y()+1);
        }return null;
    }
    private void capture(DesktopRenderContext<AbstractContainerMenu,State> c) {
        State s=c.state();s.tooltip=null;s.tooltipFont=null;s.beginObservation();
        var a=(AutomaticContainerScreenAccessor)s.screen;
        var sourceBounds=new ArrayList<DesktopWindowBounds>();sourceBounds.add(view.imageBounds());for(var r:layout().regions())sourceBounds.add(r.source());
        var capture=DesktopNativeView.union(sourceBounds).orElseThrow();
        var point=inspecting?Optional.<DesktopNativeLayout.Point>empty():sourceAt(c,c.mouseX(),c.mouseY());
        int mx=point.map(p->(int)nativeX(s,p.x())).orElse(-10000),my=point.map(p->(int)nativeY(s,p.y())).orElse(-10000);
        var frame=new AutomaticNativeScope.Frame(s.screen,s,c,null,s.layout,-a.salts_inventory_update$nativeLeft()-capture.x(),-a.salts_inventory_update$nativeTop()-capture.y());
        try{s.buffer.capture(c,s,frame,capture,mx,my);}finally{s.endObservation();}
    }
    private void paintFragments(DesktopRenderContext<AbstractContainerMenu,State> c,boolean exterior) {
        for(var r:layout().regions())if(r.exterior()==exterior){
            var painted=r;
            for(var slot:view.slots())if(!slot.player()&&slot.active()&&r.covers(slot.bounds())
                &&!call(()->template.nativeSlotBackground(extension(c),slot.menuSlotId()),true))painted=painted.cut(slot.bounds());
            for(var piece:painted.pieces())c.state().buffer.paint(c.nativeGraphics(),piece,
                regionX(c,r)+piece.x()-r.source().x(),regionY(c,r)+piece.y()-r.source().y());
            for(var slot:view.slots())if(!slot.player()&&slot.active()&&r.covers(slot.bounds())){
                var b=slot.bounds();int x=regionX(c,r)+b.x()-r.source().x()+1,y=regionY(c,r)+b.y()-r.source().y()+1;
                if(call(()->template.nativeSlotBackground(extension(c),slot.menuSlotId()),true))c.texturelessSlot(slot.menuSlotId(),x,y);
                else c.slot(slot.menuSlotId(),x,y);
            }
        }
    }
    @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> c) {
        if(!capturedBehindFrame)capture(c);capturedBehindFrame=false;
        run(()->template.renderNativeUnderlay(extension(c)));
        var vp=viewport(c);
        c.withClip(c.contentX(),c.contentY(),vp.width(),vp.height(),()->{
            paintFragments(c,false);
            run(()->template.render(extension(c)));
            if(inspecting)inspector.renderSelection(c);
        });
        renderScroll(c);
    }
    private void renderScroll(DesktopRenderContext<?,?> c){
        var l=layout();var vp=viewport(c);int w=vp.width(),h=vp.height();
        if(vp.vertical()){int thumb=Math.min(12,h);int y=c.contentY()+(int)((long)scrollY*Math.max(0,h-thumb)/Math.max(1,l.height()-h));c.fill(c.contentX()+w+2,c.contentY(),c.contentX()+w+7,c.contentY()+h,0xFF52606A);c.fill(c.contentX()+w+2,y,c.contentX()+w+7,y+thumb,0xFFE0E8EB);}
        if(vp.horizontal()){int thumb=Math.min(12,w);int x=c.contentX()+(int)((long)scrollX*Math.max(0,w-thumb)/Math.max(1,l.width()-w));c.fill(c.contentX(),c.contentY()+h+2,c.contentX()+w,c.contentY()+h+7,0xFF52606A);c.fill(x,c.contentY()+h+2,x+thumb,c.contentY()+h+7,0xFFE0E8EB);}
    }
    private boolean scrollPointer(DesktopWindowContext<?,?> c,double x,double y){
        var l=layout();var vp=viewport(c);int w=vp.width(),h=vp.height();
        if(x>=c.contentX()+w&&x<c.contentX()+c.contentWidth()&&y>=c.contentY()&&y<c.contentY()+h&&l.height()>h){scrollY=(int)Math.max(0,Math.min(l.height()-h,(y-c.contentY()-6)/Math.max(1,h-12)*(l.height()-h)));return true;}
        if(y>=c.contentY()+h&&y<c.contentY()+c.contentHeight()&&x>=c.contentX()&&x<c.contentX()+w&&l.width()>w){scrollX=(int)Math.max(0,Math.min(l.width()-w,(x-c.contentX()-6)/Math.max(1,w-12)*(l.width()-w)));return true;}return false;
    }
    @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e,boolean twice){
        if(inspecting&&inspector.clicked(c,e))return true;
        if(scrollPointer(c,e.x(),e.y())){hostScroll=true;return true;}
        if(call(()->template.mouseClicked(extension(c),e,twice),false))return true;
        if(slotAt((DesktopSlotContext)c,e.x(),e.y())!=null)return false;
        var point=sourceAt(c,e.x(),e.y());if(point.isEmpty())return false;var p=point.get();
        var f=inputFrame(c,c);boolean consumed=AutomaticNativeScope.call(f,()->c.state().screen.mouseClicked(nativeX(c.state(),p.x()),nativeY(c.state(),p.y()),e.button()));
        if(consumed)pointerSource=p;return consumed;
    }
    @Override public boolean mouseReleased(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e){
        if(inspecting&&inspector.released(c,e))return true;if(hostScroll){hostScroll=false;return true;}
        if(call(()->template.mouseReleased(extension(c),e),false))return true;
        var p=pointerSource;pointerSource=null;if(p==null)return false;
        var r=layout().regions().stream().filter(v->v.id().equals(p.regionId())).findFirst().orElse(null);if(r==null)return true;
        double sx=e.x()-regionX(c,r)+r.source().x(),sy=e.y()-regionY(c,r)+r.source().y();
        return AutomaticNativeScope.call(inputFrame(c,c),()->c.state().screen.mouseReleased(nativeX(c.state(),sx),nativeY(c.state(),sy),e.button()));
    }
    @Override public boolean mouseDragged(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e,double dx,double dy){
        if(inspecting&&inspector.dragged(c,e))return true;if(hostScroll){scrollPointer(c,e.x(),e.y());return true;}
        if(call(()->template.mouseDragged(extension(c),e,dx,dy),false))return true;
        var p=pointerSource;if(p==null)return false;var r=layout().regions().stream().filter(v->v.id().equals(p.regionId())).findFirst().orElse(null);if(r==null)return true;
        double sx=e.x()-regionX(c,r)+r.source().x(),sy=e.y()-regionY(c,r)+r.source().y();
        return AutomaticNativeScope.call(inputFrame(c,c),()->c.state().screen.mouseDragged(nativeX(c.state(),sx),nativeY(c.state(),sy),e.button(),dx,dy));
    }
    @Override public boolean mouseScrolled(DesktopInputContext<AbstractContainerMenu,State> c,double x,double y,double sx,double sy){
        if(call(()->template.mouseScrolled(extension(c),x,y,sx,sy),false))return true;var p=sourceAt(c,x,y);
        if(!inspecting&&p.isPresent()&&AutomaticNativeScope.call(inputFrame(c,c),()->c.state().screen.mouseScrolled(nativeX(c.state(),p.get().x()),nativeY(c.state(),p.get().y()),sx,sy)))return true;
        var l=layout();if(c.shiftDown()||sx!=0)scrollX+=(int)((sx!=0?-sx:-sy)*18);else scrollY-=(int)(sy*18);
        var vp=viewport(c);scrollX=Math.max(0,Math.min(scrollX,l.width()-vp.width()));scrollY=Math.max(0,Math.min(scrollY,l.height()-vp.height()));
        return vp.horizontal()||vp.vertical();
    }
    @Override public void mouseMoved(DesktopInputContext<AbstractContainerMenu,State> c,double x,double y){
        run(()->template.mouseMoved(extension(c),x,y));
        var p=inspecting?Optional.<DesktopNativeLayout.Point>empty():sourceAt(c,x,y);
        AutomaticNativeScope.call(inputFrame(c,c),()->{c.state().screen.mouseMoved(p.map(v->nativeX(c.state(),v.x())).orElse(-10000d),p.map(v->nativeY(c.state(),v.y())).orElse(-10000d));return null;});
    }
    @Override public boolean keyPressed(DesktopInputContext<AbstractContainerMenu,State> c,KeyEvent e){if(inspecting&&inspector.key(e))return true;return call(()->template.keyPressed(extension(c),e),false)||super.keyPressed(c,e);}
    @Override public boolean keyReleased(DesktopInputContext<AbstractContainerMenu,State> c,KeyEvent e){
        if(call(()->template.keyReleased(extension(c),e),false))return true;
        var focused=c.state().screen.getFocused();return focused!=null&&AutomaticNativeScope.call(inputFrame(c,c),()->focused.keyReleased(e.key(),e.scancode(),e.modifiers()));
    }
    @Override public boolean charTyped(DesktopInputContext<AbstractContainerMenu,State> c,CharacterEvent e){return call(()->template.charTyped(extension(c),e),false)||super.charTyped(c,e);}
    @Override public boolean wantsTextInput(DesktopWindowContext<AbstractContainerMenu,State> c){return call(()->template.wantsTextInput(extension(c)),false)||super.wantsTextInput(c);}
    @Override public boolean appendTooltip(DesktopRenderContext<AbstractContainerMenu,State> c,int x,int y){return call(()->template.appendTooltip(extension(c),x,y),false)||super.appendTooltip(c,x,y);}
    @Override public RecipeBookComponent createRecipeBook(DesktopWindowContext<AbstractContainerMenu,State> c){var custom=call(()->template.createRecipeBook(extension(c)),null);return custom!=null?custom:super.createRecipeBook(c);}
    @Override public void customPayload(DesktopWindowContext<AbstractContainerMenu,State> c,ResourceLocation channel,byte[] data){run(()->template.customPayload(extension(c),channel,data));}
    @Override public List<DesktopWindowBounds> textInputBounds(DesktopWindowContext<AbstractContainerMenu,State> c){
        var result=new ArrayList<DesktopWindowBounds>();result.addAll(call(()->template.textInputBounds(extension(c)),List.of()));
        var a=(AutomaticContainerScreenAccessor)c.state().screen;
        for(var child:c.state().screen.children())if(child instanceof net.minecraft.client.gui.components.EditBox w&&w.visible&&w.isFocused()&&w.getWidth()>0&&w.getHeight()>0){
            var b=new DesktopWindowBounds(w.getX()-a.salts_inventory_update$nativeLeft(),w.getY()-a.salts_inventory_update$nativeTop(),w.getWidth(),w.getHeight());
            for(var placement:layout().placements(b))if(placement.interactive()){
                var p=placement.bounds();
                if(placement.exterior()){result.add(new DesktopWindowBounds(c.windowX()+p.x(),c.windowY()+p.y(),p.width(),p.height()));continue;}
                var vp=viewport(c);int x=Math.max(c.contentX(),c.contentX()+p.x()-scrollX),y=Math.max(c.contentY(),c.contentY()+p.y()-scrollY);
                int r=Math.min(c.contentX()+vp.width(),c.contentX()+p.x()-scrollX+p.width()),d=Math.min(c.contentY()+vp.height(),c.contentY()+p.y()-scrollY+p.height());
                if(r>x&&d>y)result.add(new DesktopWindowBounds(x,y,r-x,d-y));
            }
        }return List.copyOf(result);
    }
    @Override public List<DesktopScrollRegion> controllerScrollRegions(DesktopWindowContext<AbstractContainerMenu,State> c){
        var result=new ArrayList<DesktopScrollRegion>();result.addAll(call(()->template.controllerScrollRegions(extension(c)),List.of()));
        var vp=viewport(c);if(vp.vertical())result.add(new DesktopScrollRegion(c.contentX(),c.contentY(),vp.width(),vp.height(),18,scrollY>0,scrollY<layout().height()-vp.height()));
        return List.copyOf(result);
    }
}
