package com.salts_inventory_update.controlify.qa;

import com.mojang.blaze3d.platform.NativeImage;
import com.salts_inventory_update.controlify.ContextualRadialOverlay;
import com.salts_inventory_update.controlify.model.ContextualPresentationLayout;
import com.salts_inventory_update.controlify.render.ContextualRadialAssets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Read-only observations of artwork extraction and the separately rendered real native menu. */
public final class QaContextualPresentationProbe {
    public record Draw(UUID source, UUID destination, String kind, ContextualPresentationLayout.Frame frame,
                       ContextualPresentationLayout.Rect origin, ContextualPresentationLayout.Rect destinationOrigin,
                       int width, int height, boolean held) { }
    public record NativeSlot(int index,String item,int count) { }
    public record Cooking(String input,int inputCount,String fuel,int fuelCount,String output,int outputCount,
                          double burn,double lit) { }
    private static QaWorkspaceOwner owner;
    private static ContextualRadialOverlay overlay;
    private static boolean artwork, sourceScope, guardControl;
    private static boolean destinationScope,nativeFrameScope,nativeFrameWheel,recipesScope,openingDrawn;
    private static boolean gateStratumAfterCards,navigationStratumAfterGate;
    private static int recipeNavigationDraws;
    private static UUID destination;
    private static int destinationCalls,destinationSlotCalls,destinationItemCalls,frameApiCalls,frameCoreCalls,frameTextureCalls,frameTitleCalls;
    private static ContextualPresentationLayout.Rect nativeFrameVirtual;
    private static String nativeFrameTitle="";
    private static int recipeCardDraws,recipeOpeningDraws;
    private static final List<NativeSlot> destinationSlots=new ArrayList<>();
    private static final Map<String,Map<String,Object>> frameExamples=new LinkedHashMap<>();
    private static boolean releaseArmed,releasePassed;
    private static UUID releaseSource;
    private static int releaseSlot;
    private static ContextualRadialAssets.Element requested;
    private static int requestedX,requestedY,requestedWidth,requestedHeight,requestedColor,requestBlits;
    private static int requestedSourceX,requestedSourceY,requestedSourceWidth,requestedSourceHeight;
    private static UUID source;
    private static int sourceCalls,slotCalls,itemCalls;
    private static int currentFlame,currentArrow,currentWidgets;
    private static final List<NativeSlot> slots = new ArrayList<>();
    private static final List<String> sourceTextures = new ArrayList<>();
    private static final Map<String,Map<String,Object>> textures = new LinkedHashMap<>();
    private static final Map<String,Long> blits = new LinkedHashMap<>();
    private static final List<String> violations = new ArrayList<>(),procedural = new ArrayList<>();
    private static final List<Map<String,Object>> transitions = new ArrayList<>();
    private static final Map<String,Map<String,Object>> nativeDimensions = new LinkedHashMap<>();
    private static Draw last;
    private static Cooking cooking;
    private static double ringX,ringY,ringScale,lastSectorAngle=Double.NaN;
    private static final float[] ringPose=new float[6];
    private static boolean sectorScope;
    private static int sectorX,sectorY,sectorWidth,sectorHeight;
    private static double sectorAngle,sectorHalfAngle,validatedAngle=Double.NaN,validatedHalfAngle=Double.NaN;
    private static int sectorRecipeCount;
    private static double minimumSectorHalfAngle=Double.POSITIVE_INFINITY,maximumSectorHalfAngle;
    private static final Map<Integer,Map<String,Object>> sectorExamples=new LinkedHashMap<>();
    private record Crop(int x,int y,int width,int height) { }
    private static final List<Crop> sectorCrops=new ArrayList<>();
    private static List<Crop> validatedCrops=List.of();
    private static int validatedWidth,validatedHeight;
    private static long maximumSectorCalls,maximumSectorPixels;
    private static boolean ringSeen;
    private static int backdropCalls;
    private static final Map<String,Long> backdropModes=new LinkedHashMap<>();
    private static final List<Map<String,Object>> readableTextExamples=new ArrayList<>();
    private static int nativeItemDecorationDepth;
    public static long nativeItemCountTextDraws;
    private static long nativeSourcePoseViolations;
    private static final List<String> textContrastViolations=new ArrayList<>();
    public static boolean sourcePoseValid() { return nativeCalls>0 && nativeSourcePoseViolations==0; }
    public static boolean textContrastValid() { return !readableTextExamples.isEmpty() && textContrastViolations.isEmpty(); }
    public static long backdropFrames;
    private static boolean ghostScope;
    private static final Map<Integer,Map<String,Object>> expectedGhosts=new LinkedHashMap<>();
    private static final List<Map<String,Object>> ghostExamples=new ArrayList<>();
    private static int ghostItemDraws,ghostRedDraws;
    public static long ghostFrames,ghostNativeItems,ghostNativeRedWashes;
    public static long recipeRejectionReads;
    public static void recipeRejected(boolean rejected) { if(rejected) recipeRejectionReads++; }
    public static long frames,openingFrames,settledFrames,closingFrames,liveFrames,nativeCalls,nativeSlots,nativeItems,
        cookingFrames,litFrames,outputFrames,ringAlignedFrames,ringSectorChanges,ringRegionBlits,ringCoverageFrames,controlCalls,releaseActions,
        nativeCookingWidgets,nativeCookingFlames,nativeCookingArrows;
    public static long liveDestinationFrames,nativeDestinationCalls,nativeDestinationSlots,nativeDestinationItems,
        nativeFrameCalls,nativeFrameCoreCalls,nativeFrameTextureBlits,nativeFrameTitleDraws,recipeGateFrames,recipeGateCardDraws,recipeGateStratumFrames,
        recipeNavigationStratumFrames,recipeNavigationDrawsTotal;
    private QaContextualPresentationProbe() { }
    public static void install(QaWorkspaceOwner value) { owner=value; }
    public static void workspaceBefore() { backdropCalls=0; }
    public static void backdrop(int x1,int y1,int x2,int y2,int color) {
        var window=Minecraft.getInstance().getWindow();
        if(x1==0 && y1==0 && x2==window.getGuiScaledWidth() && y2==window.getGuiScaledHeight()) {
            backdropCalls++;
            if(color!=0x66000000) violation("actual workspace full-screen dim differs from constant 0x66000000="+Integer.toHexString(color));
        }
    }
    public static void workspaceAfter(ContextualRadialOverlay value) {
        backdropFrames++;
        if(backdropCalls!=1) violation("workspace must submit exactly one full-screen dim through all wheel transitions="+backdropCalls);
        String mode=value.active()?"CONTEXTUAL_HELD":value.presenting()?"CONTEXTUAL_CLOSING":owner.radialActive()?"CONTAINER_WHEEL":"SIDE";
        backdropModes.merge(mode,1L,Long::sum);
    }
    public static void artworkText(String text,int color,boolean shadow) {
        if(!artwork || nativeFrameScope || text.isBlank()) return;
        if(nativeItemDecorationDepth>0) { nativeItemCountTextDraws++;return; }
        String normalized=text.toLowerCase(java.util.Locale.ROOT);
        if(normalized.equals("crafted item") || normalized.equals("craft requested") || normalized.equals("item selected") || normalized.equals("tab selected"))
            violation("redundant contextual status caption was rendered="+text);
        // Current Salt chrome center is a light tan; check actual native text arguments against it.
        double textLuminance=luminance(color),backgroundLuminance=luminance(0xFFCEBFA4);
        double contrast=(Math.max(textLuminance,backgroundLuminance)+.05)/(Math.min(textLuminance,backgroundLuminance)+.05);
        if((color>>>24)!=255 || contrast<4.5) {
            String error="native contextual text lacks opaque high contrast against window chrome="+text+":"+Integer.toHexString(color)+":"+contrast;
            if(textContrastViolations.size()<32) textContrastViolations.add(QaFeatureProbe.scenario+":"+error);
            violation(error);
        }
        if(readableTextExamples.size()<64 && readableTextExamples.stream().noneMatch(row -> row.get("text").equals(text)))
            readableTextExamples.add(Map.of("text",text,"color",Integer.toUnsignedLong(color),"shadow",shadow));
    }
    public static void itemDecorationsBefore() { nativeItemDecorationDepth++; }
    public static void itemDecorationsAfter() { nativeItemDecorationDepth--; }
    private static double luminance(int color) {
        double[] channels={(color>>>16&255)/255.0,(color>>>8&255)/255.0,(color&255)/255.0};
        for(int i=0;i<3;i++) channels[i]=channels[i]<=.04045?channels[i]/12.92:Math.pow((channels[i]+.055)/1.055,2.4);
        return .2126*channels[0]+.7152*channels[1]+.0722*channels[2];
    }
    public static void ghostsBefore(UUID id,ContextualPresentationLayout.Rect rect) {
        ghostScope=true;ghostItemDraws=ghostRedDraws=0;expectedGhosts.clear();
        if(last==null || !id.equals(last.source()) || !same(rect,last.frame().container())) violation("missing ghost overlay did not address actual moving native source");
        if(last==null || !last.held()) return;
        for(var ghost:owner.desktop().recipeHoverGhosts(id)) {
            var hit=owner.desktop().visibleSlots(id).stream().filter(slot -> slot.menuSlotId()==ghost.slotIndex()).findFirst().orElse(null);
            if(hit==null || ghost.item().isEmpty()) continue;
            var nativeSlot=owner.desktop().hoveredSlot(id,hit.x()+8,hit.y()+8);
            // Inventory and ghost snapshots can arrive on adjacent client frames. The renderer
            // safely skips a stale ghost if its real slot has already acquired an owned item.
            if(nativeSlot==null || nativeSlot.hasItem()) continue;
            expectedGhosts.put(ghost.slotIndex(),Map.of("slot",ghost.slotIndex(),"item",itemId(ghost.item()),"x",hit.x(),"y",hit.y()));
        }
    }
    public static void ghostItem(GuiGraphicsExtractor graphics,ItemStack item,int x,int y,int seed) {
        if(!ghostScope) return;
        var expected=expectedGhosts.get(seed);
        float opacity=com.salts_inventory_update.controlify.render.WorkspaceOpacity.current();
        if(expected==null || !expected.get("item").equals(itemId(item)) || !expected.get("x").equals(x) || !expected.get("y").equals(y)
            || Math.abs(opacity-.55f)>.00001) violation("actual missing ingredient icon differs from native slot/item/translucent opacity="+seed+":"+item+":"+x+","+y+":"+opacity);
        var bounds=owner.desktop().presentationBounds(last.source());var pose=graphics.pose();var rect=last.frame().container();
        double left=pose.m00()*bounds.x()+pose.m10()*bounds.y()+pose.m20(),top=pose.m01()*bounds.x()+pose.m11()*bounds.y()+pose.m21();
        if(Math.abs(left-rect.x())>.1 || Math.abs(top-rect.y())>.1 || Math.abs(pose.m00()*bounds.width()-rect.width())>.1)
            violation("missing ingredient icon transformed outside live container pose");
        ghostItemDraws++;ghostNativeItems++;
        if(expected!=null && ghostExamples.size()<32 && ghostExamples.stream().noneMatch(row -> row.get("item").equals(itemId(item)) && row.get("slot").equals(seed))) {
            var example=new LinkedHashMap<String,Object>(expected);example.put("opacity",opacity);example.put("scenario",QaFeatureProbe.scenario);ghostExamples.add(Map.copyOf(example));
        }
    }
    public static void ghostFill(int x1,int y1,int x2,int y2,int color) {
        if(!ghostScope) return;
        if(color!=0x88EF3535 || x2-x1!=16 || y2-y1!=16 || expectedGhosts.values().stream().noneMatch(row -> row.get("x").equals(x1) && row.get("y").equals(y1)))
            violation("actual missing ingredient wash lacks red transparent native-slot coverage="+Integer.toHexString(color));
        ghostRedDraws++;ghostNativeRedWashes++;
    }
    public static void ghostsAfter() {
        if(!expectedGhosts.isEmpty()) {
            ghostFrames++;
            if(ghostItemDraws!=expectedGhosts.size() || ghostRedDraws!=expectedGhosts.size()) violation("missing ingredients do not each render one transparent native icon and red wash");
        }
        ghostScope=false;
    }
    private static void violation(String message) { if(violations.size()<32) violations.add(QaFeatureProbe.scenario+":"+message); }
    public static List<String> violations() { return List.copyOf(violations); }
    public static List<String> proceduralViolations() { return List.copyOf(procedural); }
    public static Map<String,Long> blits() { return Map.copyOf(blits); }
    public static Map<String,Map<String,Object>> textures() { return Map.copyOf(textures); }
    public static Draw last() { return last; }
    public static Cooking cooking() { return cooking; }
    public static boolean cookingWidgetsCurrent() { return currentFlame>0 && currentArrow>0; }
    public static List<NativeSlot> slots() { return List.copyOf(slots); }
    public static List<NativeSlot> destinationSlots() { return List.copyOf(destinationSlots); }
    public static boolean currentDestination(UUID id) { return last!=null && id.equals(last.destination()) && destinationCalls==1 && destinationSlotCalls>0; }
    public static boolean settled() { return last!=null && last.frame().progress()>=.999999; }
    public static double clippedBottom(double bottom) { return artwork && last!=null ? Math.min(bottom,last.height()) : bottom; }
    public static boolean currentLive(UUID id) { return last!=null && last.source().equals(id) && sourceCalls==1 && slotCalls>0; }
    public static void armRelease(UUID id,int slot) { releaseArmed=true;releasePassed=false;releaseSource=id;releaseSlot=slot; }
    public static void releaseResult(boolean held,boolean consumed,ContextualRadialOverlay value) {
        if(releaseArmed && !held && !value.active() && value.presenting()) {
            releasePassed=!consumed;
            if(consumed) violation("RT release consumed the same native input sample");
        }
    }
    public static void click(Slot slot,UUID hovered,ContextualRadialOverlay value) {
        if(!releaseArmed) return;
        if(releasePassed && !value.active() && value.presenting() && releaseSource.equals(hovered)
                && slot!=null && slot.index==releaseSlot) { releaseActions++;releaseArmed=false; }
        else violation("same-release native select did not address actual moving source slot="+hovered+":"+slot);
    }
    public static void artworkBefore(ContextualRadialOverlay value,GuiGraphicsExtractor graphics,int width,int height) {
        overlay=value; artwork=value.presenting() && value.presentationFrame()!=null; ringSeen=false;
        frameCallsBeforeArtwork=nativeFrameCalls;
        if(!artwork) return;
        // Observe the already prepared frame; this getter never samples an animator.
        var frame=value.presentationFrame();
        last=new Draw(value.presentationSource(),value.presentationDestination(),value.kind().name(),frame,value.originalSourceRect(),
            value.originalDestinationRect(),width,height,value.active());
        frames++;
        if(value.active()) { if(frame.progress()<.999999) openingFrames++; else settledFrames++; }
        else if(frame.progress()>0) closingFrames++;
        if(frame.progress()<0 || frame.progress()>1 || last.origin()==null) violation("invalid cached presentation frame="+last);
        if(transitions.size()<64 && (frame.progress()<.999999 || !value.active())) {
            var row=new LinkedHashMap<String,Object>();row.put("scenario",QaFeatureProbe.scenario);row.put("held",value.active());
            row.put("progress",frame.progress());row.put("source",last.source().toString());row.put("container",rect(frame.container()));
            row.put("materials",rect(frame.materials()));row.put("wheel",rect(frame.wheel()));row.put("origin",rect(last.origin()));
            row.put("destination",rect(frame.destination()));row.put("destinationId",last.destination().toString());
            transitions.add(Map.copyOf(row));
        }
        if(frame.progress()>=.999999) {
            var bounds=owner.desktop().presentationBounds(value.presentationSource());
            var destinationBounds=owner.desktop().presentationBounds(value.presentationDestination());
            var expected=ContextualPresentationLayout.frame(width,height,last.origin(),last.destinationOrigin(),1,
                bounds.width(),bounds.height(),destinationBounds.width(),destinationBounds.height());
            if(!same(frame.container(),expected.container()) || !same(frame.materials(),expected.materials()) || !same(frame.wheel(),expected.wheel())
                    || !same(frame.destination(),expected.destination()))
                violation("settled native source/materials/wheel differ from shared frame target="+last);
            if(frame.materials().x()+frame.materials().width()>frame.wheel().x()+.1
                || frame.wheel().x()+frame.wheel().width()>frame.container().x()+.1) violation("settled panel left/center/right ordering="+last);
            if(frame.destination().y()<frame.materials().bottom() || frame.destination().x()+frame.destination().width()>frame.wheel().x()+.1)
                violation("actual craft destination is not below ingredients and left of wheel="+last);
        }
    }
    private static long frameCallsBeforeArtwork;
    public static void artworkAfter() {
        if(artwork && nativeFrameCalls-frameCallsBeforeArtwork!=2) violation("contextual artwork must render exactly two native Core decorative frames");
        artwork=false;overlay=null;requested=null;
    }
    public static void runGuardControl(GuiGraphicsExtractor graphics) {
        if(!artwork || controlCalls>0) return;
        guardControl=true;
        try { graphics.fill(0,0,1,1,0); } finally { guardControl=false; }
    }
    public static void procedural(String method) {
        if(!artwork || nativeFrameScope) return;
        if(guardControl) { controlCalls++; return; }
        if(procedural.size()<32) procedural.add(QaFeatureProbe.scenario+":"+method);
    }
    public static void assetBefore(GuiGraphicsExtractor graphics,ContextualRadialAssets.Element element,
                                   int x,int y,int width,int height,int color) {
        var sprite=ContextualRadialAssets.theme().sprites().get(element);
        assetRegionBefore(graphics,element,x,y,width,height,0,0,sprite.width(),sprite.height(),color);
    }
    public static void assetRegionBefore(GuiGraphicsExtractor graphics,ContextualRadialAssets.Element element,
        int x,int y,int width,int height,int sourceX,int sourceY,int sourceWidth,int sourceHeight,int color) {
        if(!artwork) return;
        requested=element;requestedX=x;requestedY=y;requestedWidth=width;requestedHeight=height;requestedColor=color;requestBlits=0;
        requestedSourceX=sourceX;requestedSourceY=sourceY;requestedSourceWidth=sourceWidth;requestedSourceHeight=sourceHeight;
        QaNativeGuideProbe.surface(graphics,x,y,width,height);
    }
    public static void assetAfter() {
        if(requested!=null && requestBlits!=1) violation("PNG helper must dispatch exactly one native blit="+requested+":"+requestBlits);
        requested=null;
    }
    public static void texture(GuiGraphicsExtractor graphics,Identifier id,int x,int y,float u,float v,int width,int height,
                               int sourceWidth,int sourceHeight,int textureWidth,int textureHeight,int color) {
        if(nativeFrameScope && id.toString().equals("salts_inventory_update:textures/gui/window.png")) {
            frameTextureCalls++;nativeFrameTextureBlits++;
            if(textureWidth!=11 || textureHeight!=11) violation("native Core chrome used a different frame texture contract");
        }
        if(sourceScope) {
            if(sourceTextures.size()<24) sourceTextures.add(id.toString());
            if(id.toString().equals("salts_inventory_update:textures/gui/container_widgets.png")) {
                currentWidgets++;nativeCookingWidgets++;
                if(u==14 && width==14 && height>0) { currentFlame++;nativeCookingFlames++; }
                if(u==24 && v==14 && width>0 && height==16) { currentArrow++;nativeCookingArrows++; }
            }
        }
        if(!artwork || requested==null) return;
        requestBlits++;
        var sprite=ContextualRadialAssets.theme().sprites().get(requested);
        if(!sprite.texture().equals(id) || x!=requestedX || y!=requestedY || width!=requestedWidth || height!=requestedHeight
                || u!=requestedSourceX || v!=requestedSourceY || sourceWidth!=requestedSourceWidth || sourceHeight!=requestedSourceHeight
                || textureWidth!=sprite.width() || textureHeight!=sprite.height() || color!=requestedColor)
            violation("actual native PNG blit differs from manifest/helper request="+requested+":"+id);
        blits.merge(requested.name(),1L,Long::sum); decode(id,sprite.width(),sprite.height());
        if(recipesScope && (requested==ContextualRadialAssets.Element.RECIPE_CARD || requested==ContextualRadialAssets.Element.UNAVAILABLE_RECIPE_CARD
                || requested==ContextualRadialAssets.Element.HOVERED_CARD)) {
            recipeCardDraws++;recipeGateCardDraws++;gateStratumAfterCards=false;
            if(openingDrawn) violation("native recipe card surface was drawn above carousel opening gate");
        }
        if(recipesScope && requested==ContextualRadialAssets.Element.CAROUSEL_OPENING) {
            recipeOpeningDraws++;openingDrawn=true;
            if(recipeCardDraws>0 && !gateStratumAfterCards) violation("carousel opening did not enter a later native render stratum than recipe cards");
            if(recipeCardDraws>0 && gateStratumAfterCards) recipeGateStratumFrames++;
        }
        if(recipesScope && switch(requested) {
            case WHEEL_RING,WHEEL_RING_HIGHLIGHT,ALPHABET_LABEL,HIGHLIGHTED_ALPHABET_LABEL,ALPHABET_ANCHOR,ALPHABET_LEADER -> true;
            default -> false;
        }) {
            recipeNavigationDraws++;recipeNavigationDrawsTotal++;
            if(!openingDrawn || !navigationStratumAfterGate) violation("native ring/alphabet navigation was not submitted above the carousel opening="+requested);
        }
        var pose=graphics.pose();
        if(requested==ContextualRadialAssets.Element.WHEEL_RING) {
            ringX=pose.m00()*(x+width*.5)+pose.m10()*(y+height*.5)+pose.m20();
            ringY=pose.m01()*(x+width*.5)+pose.m11()*(y+height*.5)+pose.m21();
            ringScale=Math.hypot(pose.m00(),pose.m01())*width;ringSeen=true;
            ringPose[0]=pose.m00();ringPose[1]=pose.m01();ringPose[2]=pose.m10();ringPose[3]=pose.m11();ringPose[4]=pose.m20();ringPose[5]=pose.m21();
        } else if(requested==ContextualRadialAssets.Element.WHEEL_RING_HIGHLIGHT) {
            if(!sectorScope || Math.abs(pose.m00()-ringPose[0])>.00001 || Math.abs(pose.m01()-ringPose[1])>.00001
                    || Math.abs(pose.m10()-ringPose[2])>.00001 || Math.abs(pose.m11()-ringPose[3])>.00001
                    || Math.abs(pose.m20()-ringPose[4])>.00001 || Math.abs(pose.m21()-ringPose[5])>.00001)
                violation("cropped highlight changed the base ring pose/lighting");
            int left=(int)Math.round(u*288/textureWidth),top=(int)Math.round(v*288/textureHeight);
            int right=(int)Math.round((u+sourceWidth)*288/textureWidth),bottom=(int)Math.round((v+sourceHeight)*288/textureHeight);
            if(x!=sectorX+left || y!=sectorY+top || width!=right-left || height!=bottom-top
                    || u<0 || v<0 || u+sourceWidth>textureWidth || v+sourceHeight>textureHeight || sourceWidth<=0 || sourceHeight<=0)
                violation("highlight source UV and destination pixel offsets do not preserve authored ring artwork");
            sectorCrops.add(new Crop((int)u,(int)v,sourceWidth,sourceHeight));ringRegionBlits++;
        }
    }
    public static void sectorBefore(GuiGraphicsExtractor graphics,int x,int y,double angle,double halfAngle) {
        if(!artwork) return;
        sectorScope=true;sectorCrops.clear();sectorX=x;sectorY=y;sectorAngle=angle;sectorHalfAngle=halfAngle;
        var sprite=ContextualRadialAssets.theme().sprites().get(ContextualRadialAssets.Element.WHEEL_RING_HIGHLIGHT);
        sectorWidth=sprite.width();sectorHeight=sprite.height();var pose=graphics.pose();
        double cx=pose.m00()*(x+144)+pose.m10()*(y+144)+pose.m20(),cy=pose.m01()*(x+144)+pose.m11()*(y+144)+pose.m21();
        if(!Double.isFinite(angle) || !ringSeen || Math.abs(cx-ringX)>.1 || Math.abs(cy-ringY)>.1
                || Math.abs(Math.hypot(pose.m00(),pose.m01())*288-ringScale)>.1) violation("fixed highlight canvas differs from native ring");
        try {
            var selectionField=ContextualRadialOverlay.class.getDeclaredField("selection");selectionField.setAccessible(true);
            var entriesField=ContextualRadialOverlay.class.getDeclaredField("indexedEntries");entriesField.setAccessible(true);
            var selection=(com.salts_inventory_update.controlify.model.ContextualWheelSelection)selectionField.get(overlay);
            int count=((List<?>)entriesField.get(overlay)).size();sectorRecipeCount=count;double expected=selection.positionAngle(count);
            if(Math.abs(Math.atan2(Math.sin(angle-expected),Math.cos(angle-expected)))>.000001)
                violation("highlight crop angle differs from continuous recipe position");
            double expectedHalfAngle=Math.PI*Math.min(10,count)/count;
            if(count<=0 || !Double.isFinite(halfAngle) || Math.abs(halfAngle-expectedHalfAngle)>.000001)
                violation("highlight width does not represent the visible recipe range="+count+":"+halfAngle+":"+expectedHalfAngle);
        } catch(ReflectiveOperationException error) { violation("native highlight selection observation="+error); }
    }
    public static void sectorAfter() {
        if(!sectorScope) return;
        long pixels=sectorCrops.stream().mapToLong(crop -> (long)crop.width()*crop.height()).sum();
        maximumSectorCalls=Math.max(maximumSectorCalls,sectorCrops.size());maximumSectorPixels=Math.max(maximumSectorPixels,pixels);
        if(sectorCrops.isEmpty() || sectorCrops.size()>2*sectorHeight || pixels>sectorWidth*(long)sectorHeight
                || sectorHalfAngle<=0 || sectorHalfAngle>Math.PI)
            violation("native variable highlight is empty, invalid or exceeds bounded crop calls");
        // Independently check the pixels actually sent through native blits, not the mask helper's result.
        if(sectorAngle!=validatedAngle || sectorHalfAngle!=validatedHalfAngle || sectorWidth!=validatedWidth || sectorHeight!=validatedHeight || !sectorCrops.equals(validatedCrops)) {
            boolean[] coverage=new boolean[sectorWidth*sectorHeight];
            for(var crop:sectorCrops) {
                if(crop.x()<0 || crop.y()<0 || crop.x()+crop.width()>sectorWidth || crop.y()+crop.height()>sectorHeight) continue;
                for(int y=crop.y();y<crop.y()+crop.height();y++) for(int x=crop.x();x<crop.x()+crop.width();x++) {
                    int index=y*sectorWidth+x;if(coverage[index]) violation("native highlight crop repeats source pixel");coverage[index]=true;
                }
            }
            boolean correct=true;
            for(int y=0;y<sectorHeight;y++) for(int x=0;x<sectorWidth;x++) {
                double dx=x+.5-sectorWidth/2.0,dy=y+.5-sectorHeight/2.0;
                double difference=Math.abs(Math.atan2(Math.sin(Math.atan2(dx,-dy)-sectorAngle),Math.cos(Math.atan2(dx,-dy)-sectorAngle)));
                double margin=difference-sectorHalfAngle;
                if(Math.abs(margin)>1e-12 && coverage[y*sectorWidth+x] != (margin<0 && (dx!=0 || dy!=0))) correct=false;
            }
            if(!correct) violation("actual fixed-UV source coverage differs from selected variable-width wedge");
            validatedAngle=sectorAngle;validatedHalfAngle=sectorHalfAngle;validatedWidth=sectorWidth;validatedHeight=sectorHeight;validatedCrops=List.copyOf(sectorCrops);
        }
        minimumSectorHalfAngle=Math.min(minimumSectorHalfAngle,sectorHalfAngle);maximumSectorHalfAngle=Math.max(maximumSectorHalfAngle,sectorHalfAngle);
        if(sectorExamples.size()<16 || sectorExamples.containsKey(sectorRecipeCount))
            sectorExamples.put(sectorRecipeCount,Map.of("recipeCount",sectorRecipeCount,"visibleCount",Math.min(10,sectorRecipeCount),
                "halfAngle",sectorHalfAngle,"angle",sectorAngle,"pixels",pixels,"nativeRegions",List.copyOf(sectorCrops)));
        ringAlignedFrames++;ringCoverageFrames++;
        if(Double.isFinite(lastSectorAngle) && Math.abs(Math.atan2(Math.sin(sectorAngle-lastSectorAngle),Math.cos(sectorAngle-lastSectorAngle)))>.001) ringSectorChanges++;
        lastSectorAngle=sectorAngle;sectorScope=false;
    }
    private static void decode(Identifier id,int width,int height) {
        if(textures.containsKey(id.toString())) return;
        try(var stream=Minecraft.getInstance().getResourceManager().getResource(id).orElseThrow().open();var image=NativeImage.read(stream)) {
            int visible=0,transparent=0;int[] pixels=image.getPixels();
            for(int pixel:pixels) { if((pixel>>>24)==0) transparent++;else visible++; }
            var bytes=java.nio.ByteBuffer.allocate(pixels.length*4);bytes.asIntBuffer().put(pixels);
            String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes.array()));
            textures.put(id.toString(),Map.of("width",image.getWidth(),"height",image.getHeight(),"visible",visible,"transparent",transparent,"pixelSha256",hash));
            if(image.getWidth()!=width || image.getHeight()!=height || visible==0
                    && !id.equals(ContextualRadialAssets.textureId(ContextualRadialAssets.Element.BACKDROP)))
                violation("decoded native PNG missing/wrong dimensions/empty required artwork="+id);
        } catch(Exception error) { violation("native PNG decode="+id+":"+error); }
    }
    public static void sourceBefore(UUID id,ContextualPresentationLayout.Rect rect) {
        sourceScope=true;source=id;sourceCalls=slotCalls=itemCalls=0;slots.clear();sourceTextures.clear();cooking=null;
        currentFlame=currentArrow=currentWidgets=0;
        if(last==null || !id.equals(last.source()) || !same(rect,last.frame().container())) violation("live native render not using actual cached source frame="+id);
    }
    public static void nativeSource(GuiGraphicsExtractor graphics,UUID id) {
        if(destinationScope) {
            destinationCalls++;nativeDestinationCalls++;
            if(!id.equals(destination)) violation("native Core rendered a different paired destination");
            var bounds=owner.desktop().presentationBounds(id);var pose=graphics.pose();var rect=last.frame().destination();
            double left=pose.m00()*bounds.x()+pose.m10()*bounds.y()+pose.m20(),top=pose.m01()*bounds.x()+pose.m11()*bounds.y()+pose.m21();
            if(Math.abs(left-rect.x())>.1 || Math.abs(top-rect.y())>.1 || Math.abs(pose.m00()*bounds.width()-rect.width())>.1
                    || Math.abs(pose.m11()*bounds.height()-rect.height())>.1) violation("actual native destination pose differs from paired frame");
            return;
        }
        if(!sourceScope) return;
        sourceCalls++;nativeCalls++;
        if(!id.equals(source)) { nativeSourcePoseViolations++;violation("native desktop rendered different contextual source"); }
        var bounds=owner.desktop().presentationBounds(id);var pose=graphics.pose();var rect=last.frame().container();
        double left=pose.m00()*bounds.x()+pose.m10()*bounds.y()+pose.m20();
        double top=pose.m01()*bounds.x()+pose.m11()*bounds.y()+pose.m21();
        if(Math.abs(left-rect.x())>.1 || Math.abs(top-rect.y())>.1 || Math.abs(pose.m00()*bounds.width()-rect.width())>.1) {
            nativeSourcePoseViolations++;violation("native source draw pose differs from moving input rectangle="+rect);
        }
        if(Math.abs(pose.m11()*bounds.height()-rect.height())>.1) {
            nativeSourcePoseViolations++;violation("actual native source height/aspect differs from shared container frame="+rect+",native="+bounds);
        }
        String key=id+":"+bounds.width()+"x"+bounds.height();
        if(nativeDimensions.size()<32 || nativeDimensions.containsKey(key)) {
            var row=new LinkedHashMap<String,Object>();row.put("source",id.toString());row.put("kind",last.kind());
            row.put("nativeWidth",bounds.width());row.put("nativeHeight",bounds.height());row.put("progress",last.frame().progress());
            row.put("actualWidth",pose.m00()*bounds.width());row.put("actualHeight",pose.m11()*bounds.height());
            row.put("frameWidth",rect.width());row.put("frameHeight",rect.height());nativeDimensions.put(key,Map.copyOf(row));
        }
    }
    public static void slot(Slot slot) {
        if(destinationScope) {
            destinationSlotCalls++;nativeDestinationSlots++;var item=slot.getItem();
            destinationSlots.add(new NativeSlot(slot.index,itemId(item),item.getCount()));return;
        }
        if(!sourceScope) return;
        slotCalls++;nativeSlots++;var item=slot.getItem();slots.add(new NativeSlot(slot.index,itemId(item),item.getCount()));
    }
    public static void nativeItem(ItemStack item) {
        if(recipesScope && !item.isEmpty()) {
            gateStratumAfterCards=false;
            if(openingDrawn) violation("native recipe icon was drawn above carousel opening");
        }
        if(destinationScope && !item.isEmpty()) { destinationItemCalls++;nativeDestinationItems++; }
        if(sourceScope && !item.isEmpty()) { itemCalls++;nativeItems++; }
    }
    public static void cooking(AbstractFurnaceMenu menu) {
        if(!sourceScope) return;
        var input=menu.getSlot(0).getItem();var fuel=menu.getSlot(1).getItem();var output=menu.getSlot(2).getItem();
        cooking=new Cooking(itemId(input),input.getCount(),itemId(fuel),fuel.getCount(),itemId(output),output.getCount(),menu.getBurnProgress(),menu.getLitProgress());
        cookingFrames++;if(menu.isLit() && cooking.burn()>0 && cooking.lit()>0) litFrames++;if(!output.isEmpty()) outputFrames++;
    }
    public static void sourceAfter() {
        liveFrames++;
        if(sourceCalls!=1 || slotCalls==0) violation("actual live Core source render missing native render/slots="+source+":"+sourceCalls+":"+slotCalls);
        if(cooking!=null && currentWidgets<2) violation("native furnace render lacks actual empty flame/arrow widget textures");
        sourceScope=false;source=null;
    }
    public static void destinationBefore(UUID id,ContextualPresentationLayout.Rect rect) {
        destinationScope=true;destination=id;destinationCalls=destinationSlotCalls=destinationItemCalls=0;destinationSlots.clear();
        if(last==null || !id.equals(last.destination()) || !same(rect,last.frame().destination()))
            violation("actual destination render differs from cached paired frame="+id);
    }
    public static void destinationAfter() {
        liveDestinationFrames++;
        if(destinationCalls!=1 || destinationSlotCalls==0) violation("native paired destination render/slots missing="+destination);
        destinationScope=false;destination=null;
    }
    public static void nativeFrameBefore(ContextualPresentationLayout.Rect virtual,boolean wheel) {
        nativeFrameScope=true;nativeFrameVirtual=virtual;nativeFrameWheel=wheel;
        frameApiCalls=frameCoreCalls=frameTextureCalls=frameTitleCalls=0;nativeFrameTitle="";
    }
    public static void nativeFrameApi(GuiGraphicsExtractor graphics,com.salts_inventory_update.api.client.desktop.DesktopWindowBounds bounds,
        net.minecraft.network.chat.Component title) {
        if(!nativeFrameScope) return;
        frameApiCalls++;nativeFrameCalls++;var shown=nativeFrameWheel?last.frame().wheel():last.frame().materials();var pose=graphics.pose();
        double left=pose.m00()*bounds.x()+pose.m10()*bounds.y()+pose.m20(),top=pose.m01()*bounds.x()+pose.m11()*bounds.y()+pose.m21();
        if(Math.abs(left-shown.x())>.1 || Math.abs(top-shown.y())>.1 || Math.abs(pose.m00()*bounds.width()-shown.width())>.1
            || Math.abs(pose.m11()*bounds.height()-shown.height())>.1) violation("native dynamic chrome differs from actual emerging panel rect");
        String key=nativeFrameWheel?"WHEEL":"MATERIALS";
        frameExamples.put(key,Map.of("kind",key,"title",title.getString(),"width",bounds.width(),"height",bounds.height(),
            "shown",rect(shown)));
    }
    public static void nativeFrameCore(int x,int y,int width,int height,String title) {
        if(!nativeFrameScope) return;
        frameCoreCalls++;nativeFrameCoreCalls++;nativeFrameTitle=title;
        if(x!=nativeFrameVirtual.x() || y!=nativeFrameVirtual.y() || width!=nativeFrameVirtual.width() || height!=nativeFrameVirtual.height())
            violation("decorative frame did not use the exact ordinary Core chrome helper dimensions");
    }
    public static void nativeFrameText(String text,int x,int y,int color) {
        if(!nativeFrameScope || text.isEmpty()) return;
        frameTitleCalls++;nativeFrameTitleDraws++;
        if(x!=nativeFrameVirtual.x()+6 || y!=nativeFrameVirtual.y()+5 || (color&0xFFFFFF)!=0x111111)
            violation("native Core frame title differs from ordinary black title pose/color="+text);
    }
    public static void nativeFrameAfter() {
        if(frameApiCalls!=1 || frameCoreCalls!=1 || frameTextureCalls!=9 || (!nativeFrameTitle.isEmpty() && frameTitleCalls!=1))
            violation("dynamic panel frame lacks actual Core API/helper/nine-slice/title calls="+frameApiCalls+":"+frameCoreCalls+":"+frameTextureCalls+":"+frameTitleCalls);
        nativeFrameScope=false;
    }
    public static void recipesBefore() { recipesScope=true;openingDrawn=gateStratumAfterCards=navigationStratumAfterGate=false;recipeCardDraws=recipeOpeningDraws=recipeNavigationDraws=0; }
    public static void nextStratum() {
        if(recipesScope && recipeCardDraws>0 && !openingDrawn) gateStratumAfterCards=true;
        if(recipesScope && openingDrawn) navigationStratumAfterGate=true;
    }
    public static void recipesAfter() {
        recipeGateFrames++;
        if(recipeOpeningDraws!=1 || (recipeCardDraws>0 && !openingDrawn)) violation("recipe cards lack later native carousel opening mask");
        if(recipeNavigationDraws>0 && navigationStratumAfterGate) recipeNavigationStratumFrames++;
        if(recipeCardDraws>0 && recipeNavigationDraws==0) violation("recipe navigation layer missing from native render");
        recipesScope=false;
    }
    private static String itemId(ItemStack item) { return BuiltInRegistries.ITEM.getKey(item.getItem()).toString(); }
    private static boolean same(ContextualPresentationLayout.Rect a,ContextualPresentationLayout.Rect b) {
        return Math.abs(a.x()-b.x())<.001 && Math.abs(a.y()-b.y())<.001 && Math.abs(a.width()-b.width())<.001 && Math.abs(a.height()-b.height())<.001;
    }
    private static Map<String,Object> rect(ContextualPresentationLayout.Rect value) { return Map.of("x",value.x(),"y",value.y(),"width",value.width(),"height",value.height()); }
    public static Map<String,Object> snapshot() {
        var result=new LinkedHashMap<String,Object>();result.put("contextualPresentationFrames",frames);result.put("contextualOpeningFrames",openingFrames);
        result.put("contextualSettledFrames",settledFrames);result.put("contextualClosingFrames",closingFrames);result.put("contextualLiveSourceFrames",liveFrames);
        result.put("contextualNativeSourceCalls",nativeCalls);result.put("contextualNativeSlotDraws",nativeSlots);result.put("contextualNativeSourceItemDraws",nativeItems);
        result.put("contextualNativeCookingFrames",cookingFrames);result.put("contextualNativeCookingLitFrames",litFrames);result.put("contextualNativeCookingOutputFrames",outputFrames);
        result.put("contextualDecodedTextures",Map.copyOf(textures));result.put("contextualTextureBlits",Map.copyOf(blits));
        result.put("contextualRingAlignedFrames",ringAlignedFrames);result.put("contextualRingSectorChanges",ringSectorChanges);
        result.put("contextualRingRegionBlits",ringRegionBlits);result.put("contextualRingCoverageFrames",ringCoverageFrames);
        result.put("contextualMaximumRingRegionCalls",maximumSectorCalls);result.put("contextualMaximumRingSectorPixels",maximumSectorPixels);
        result.put("contextualLastRingSector",Map.of("angle",sectorAngle,"halfAngle",sectorHalfAngle,"recipeCount",sectorRecipeCount,"sourceWidth",sectorWidth,"sourceHeight",sectorHeight,
            "nativeRegions",List.copyOf(sectorCrops)));
        result.put("contextualMinimumRingHalfAngle",minimumSectorHalfAngle);result.put("contextualMaximumRingHalfAngle",maximumSectorHalfAngle);
        result.put("contextualVariableRingSectors",List.copyOf(sectorExamples.values()));
        result.put("contextualProceduralGuardControlCalls",controlCalls);result.put("contextualProceduralShapeViolations",List.copyOf(procedural));
        result.put("contextualSameReleaseNativeActions",releaseActions);
        result.put("contextualNativeCookingWidgetBlits",nativeCookingWidgets);result.put("contextualNativeCookingFlameBlits",nativeCookingFlames);
        result.put("contextualNativeCookingArrowBlits",nativeCookingArrows);
        result.put("contextualNativeSourceDimensions",List.copyOf(nativeDimensions.values()));
        result.put("contextualLiveDestinationFrames",liveDestinationFrames);result.put("contextualNativeDestinationCalls",nativeDestinationCalls);
        result.put("contextualNativeDestinationSlotDraws",nativeDestinationSlots);result.put("contextualNativeDestinationItemDraws",nativeDestinationItems);
        result.put("contextualNativeFrameCalls",nativeFrameCalls);result.put("contextualNativeFrameCoreCalls",nativeFrameCoreCalls);
        result.put("contextualNativeFrameTextureBlits",nativeFrameTextureBlits);result.put("contextualNativeFrameTitleDraws",nativeFrameTitleDraws);
        result.put("contextualNativeFrameExamples",List.copyOf(frameExamples.values()));
        result.put("contextualRecipeGateFrames",recipeGateFrames);result.put("contextualRecipeGateCardDraws",recipeGateCardDraws);
        result.put("contextualRecipeGateStratumFrames",recipeGateStratumFrames);
        result.put("contextualRecipeNavigationStratumFrames",recipeNavigationStratumFrames);result.put("contextualRecipeNavigationDraws",recipeNavigationDrawsTotal);
        result.put("contextualBackdropFrames",backdropFrames);result.put("contextualBackdropModes",Map.copyOf(backdropModes));
        result.put("contextualReadableTextExamples",List.copyOf(readableTextExamples));
        result.put("contextualNativeItemCountTextDraws",nativeItemCountTextDraws);
        result.put("contextualNativeSourcePoseViolations",nativeSourcePoseViolations);result.put("contextualTextContrastViolations",List.copyOf(textContrastViolations));
        result.put("contextualMissingGhostFrames",ghostFrames);result.put("contextualMissingGhostNativeItems",ghostNativeItems);
        result.put("contextualMissingGhostNativeRedWashes",ghostNativeRedWashes);result.put("contextualMissingGhostExamples",List.copyOf(ghostExamples));
        result.put("contextualRecipeRejectionReads",recipeRejectionReads);
        result.put("contextualPresentationViolations",List.copyOf(violations));result.put("contextualPresentationTransitions",List.copyOf(transitions));return result;
    }
}
