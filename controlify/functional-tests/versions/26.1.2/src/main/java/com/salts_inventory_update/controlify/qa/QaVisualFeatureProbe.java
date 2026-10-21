package com.salts_inventory_update.controlify.qa;

import com.mojang.blaze3d.platform.NativeImage;
import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import com.salts_inventory_update.controlify.model.WindowPlacement;
import com.salts_inventory_update.controlify.model.WindowSize;
import com.salts_inventory_update.controlify.model.WorkspacePreviewGeometry;
import com.salts_inventory_update.controlify.model.AlphabeticalRecipeIndex;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Actual native item, texture and sound observations for the 26.3 visual feature pass. */
public final class QaVisualFeatureProbe {
    public record Preview(UUID id, String item, WindowPlacement placement, String mode, boolean active,
                          int nativeItemDraws, double left, double top, double right, double bottom) { }
    public record Tuck(WindowPlacement placement, List<String> textures, int frame, double size) { }
    public record Surface(boolean targetLive, double live) { }
    public record AlphabetBounds(String letter, int firstIndex, double left, double top, double right, double bottom) { }
    private static QaWorkspaceOwner owner;
    private static UUID iconId;
    private static ItemStack iconItem;
    private static WindowPlacement iconPlacement, tuckPlacement;
    private static DesktopWindowBounds iconBounds;
    private static int iconItemDraws;
    private static double iconLeft, iconTop, iconRight, iconBottom, tuckSize;
    private static int tuckFrame;
    private static final Map<UUID, Preview> previews = new LinkedHashMap<>();
    private static final Set<UUID> nativePanes = new LinkedHashSet<>();
    private static final Map<UUID, Surface> surfaceFrames = new LinkedHashMap<>();
    private static final Map<String, Map<String,Object>> decodedTextures = new LinkedHashMap<>();
    private static final List<String> violations = new ArrayList<>();
    private static final List<String> tuckTextures = new ArrayList<>();
    private static final Set<String> tuckStateTextures = new LinkedHashSet<>();
    private static final Set<Integer> tuckStateFrames = new LinkedHashSet<>();
    public static long workspaceFrames, previewFrames, previewItemDraws, nativePaneDraws, tuckFrames, tuckIntermediateFrames;
    public static Tuck tuck;
    private static String soundAction;
    private static long soundStarted;
    private static int soundCalls;
    private static boolean soundPreview;
    private static long soundGeneration;
    private static final List<Map<String,Object>> acceptedSounds = new ArrayList<>();
    private static final Map<SoundInstance,Long> soundInstances = new java.util.IdentityHashMap<>();
    private static final List<Map<String,Object>> nativeSoundStops = new ArrayList<>();
    private static long nativeSoundOrder, nextSoundInstance;
    private static final Map<String,Long> soundRequests = new LinkedHashMap<>(), soundAccepted = new LinkedHashMap<>();
    private static long rejectedSoundRequests;
    private static AlphabeticalRecipeIndex.Group alphabetGroup;
    private static int alphabetNativeTextCalls;
    private static int alphabetMarkerAnchorCalls;
    private static final List<AlphabeticalRecipeIndex.Group> alphabetGroups = new ArrayList<>();
    private static final List<AlphabetBounds> alphabetBounds = new ArrayList<>();
    public static final List<String> alphabetOverlapViolations = new ArrayList<>(), alphabetBoundsViolations = new ArrayList<>();
    private static int overlayWidth, overlayHeight;
    private static boolean alphabetAnchorScope, alphabetAnchorDrawn;
    private static int alphabetAnchorX, alphabetAnchorY;
    public static long alphabetFrames, alphabetMarkers, alphabetNativeTextDraws;
    public static long alphabetBoundsFrames, alphabetAnchorTicks;

    private QaVisualFeatureProbe() { }
    public static void install(QaWorkspaceOwner value) { owner = value; }
    public static void workspaceBegin() { previews.clear(); nativePanes.clear(); surfaceFrames.clear(); workspaceFrames++; }
    public static void surface(UUID id, boolean targetLive, double live) {
        surfaceFrames.put(id, new Surface(targetLive, live));
        if (!Double.isFinite(live) || live < 0 || live > 1) violation("invalid native surface blend="+id+":"+live);
    }
    public static void workspaceEnd() {
        for (var entry : surfaceFrames.entrySet()) {
            if (entry.getValue().live() >= .005 && !nativePanes.contains(entry.getKey()))
                violation("crossfading live surface omitted its native pane="+entry);
        }
    }
    public static Map<UUID, Preview> previews() { return Map.copyOf(previews); }
    public static Set<UUID> nativePanes() { return Set.copyOf(nativePanes); }
    public static List<String> violations() { return List.copyOf(violations); }
    public static Set<String> tuckTextures() { return Set.copyOf(tuckStateTextures); }
    public static Map<String, Map<String,Object>> textures() { return Map.copyOf(decodedTextures); }
    public static List<AlphabeticalRecipeIndex.Group> alphabetGroups() { return List.copyOf(alphabetGroups); }
    public static List<AlphabetBounds> alphabetBounds() { return List.copyOf(alphabetBounds); }
    private static void violation(String value) { if (violations.size() < 32) violations.add("scenario="+QaFeatureProbe.scenario+","+value); }

    public static void iconBefore(UUID id, ItemStack item, WindowPlacement placement, DesktopWindowBounds bounds) {
        iconId = id; iconItem = item.copy(); iconPlacement = placement; iconBounds = bounds; iconItemDraws = 0;
        if (item.isEmpty()) violation("empty associated preview icon="+id);
        if (!ItemStack.isSameItemSameComponents(item, owner.desktop().previewIcon(id))) violation("preview differs from actual associated Core item="+id);
    }
    public static void iconAfter() {
        if (iconId == null) return;
        boolean active = !owner.tuckedAway() && (iconId.equals(owner.model().focused())
            || owner.radialActive() && owner.model().highlighted().filter(iconId::equals).isPresent()
            || !owner.radialActive() && iconId.equals(owner.model().selected()));
        String mode = owner.tuckedAway() ? "TUCKED" : owner.radialActive() ? "LT_RADIAL" : "SIDE";
        var preview = new Preview(iconId, BuiltInRegistries.ITEM.getKey(iconItem.getItem()).toString(), iconPlacement, mode, active,
            iconItemDraws, iconLeft, iconTop, iconRight, iconBottom);
        previews.put(iconId, preview); previewFrames++;
        var surface = surfaceFrames.get(iconId);
        // Active targets retain a fading icon while their measured native surface blends in.
        // A settled active pane must never keep an icon; every icon still draws its exact native item once.
        if (iconItemDraws != 1 || active && (surface == null || !surface.targetLive() || surface.live() >= .995))
            violation("invalid inactive or crossfading native item preview="+preview+",surface="+surface);
        var expected = WorkspacePreviewGeometry.icon(iconPlacement,new WindowSize(iconBounds.width(),iconBounds.height()));
        if (Math.abs(iconLeft-expected.x()) > .05 || Math.abs(iconTop-expected.y()) > .05
                || Math.abs(iconRight-iconLeft-expected.size()) > .05 || Math.abs(iconBottom-iconTop-expected.size()) > .05)
            violation("native preview icon transform differs from animated geometry="+preview);
        iconId = null; iconItem = null;
    }
    public static void item(GuiGraphicsExtractor graphics, ItemStack item, int x, int y) {
        if (iconId == null) return;
        iconItemDraws++; previewItemDraws++;
        if (!ItemStack.isSameItemSameComponents(item, iconItem)) violation("preview native item differs from associated icon="+iconId);
        var pose = graphics.pose();
        iconLeft = pose.m00()*x+pose.m10()*y+pose.m20(); iconTop = pose.m01()*x+pose.m11()*y+pose.m21();
        iconRight = pose.m00()*(x+16)+pose.m10()*(y+16)+pose.m20(); iconBottom = pose.m01()*(x+16)+pose.m11()*(y+16)+pose.m21();
    }
    public static void nativePane(UUID id) { nativePanes.add(id); nativePaneDraws++; }
    public static void tuckBefore(WindowPlacement placement) { tuckPlacement = placement; tuckTextures.clear(); tuckFrame = -1; tuckSize = 0; }
    public static void tuckAfter() {
        if (tuckPlacement == null) return;
        tuck = new Tuck(tuckPlacement, List.copyOf(tuckTextures),tuckFrame,tuckSize); tuckFrames++;
        if (tuckPlacement.highlight() > .02 && tuckPlacement.highlight() < .98) tuckIntermediateFrames++;
        if (tuckTextures.isEmpty()) violation("tuck rendered without a decoded state texture="+tuckPlacement);
        tuckPlacement = null;
    }
    public static void texture(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, float u, float v,
                               int width, int height, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
        if (tuckPlacement == null) return;
        String id = texture.toString(); tuckTextures.add(id); tuckStateTextures.add(id);
        tuckFrame = (int)(u/32); tuckStateFrames.add(tuckFrame);
        var pose = graphics.pose(); tuckSize = Math.abs(pose.m00()*width);
        var expected = WorkspacePreviewGeometry.tuck(tuckPlacement);
        if (tuckFrame != (tuckPlacement.highlight() >= .5 ? 1 : 0) || v != 0 || sourceWidth != 32 || sourceHeight != 32
                || textureWidth != 64 || textureHeight != 32 || Math.abs(tuckSize-expected.size()) > .05
                || Math.abs(pose.m00()*x+pose.m10()*y+pose.m20()-expected.x()) > .05
                || Math.abs(pose.m01()*x+pose.m11()*y+pose.m21()-expected.y()) > .05)
            violation("invalid actual animated tuck atlas frame="+id+":"+tuckFrame+":"+tuckPlacement);
        if (decodedTextures.containsKey(id)) return;
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(texture).orElseThrow();
            try (var stream = resource.open(); var image = NativeImage.read(stream)) {
                int[] pixels = image.getPixels(); int visible = 0, transparent = 0;
                var bytes = java.nio.ByteBuffer.allocate(pixels.length*4); bytes.asIntBuffer().put(pixels);
                for (int pixel : pixels) { if ((pixel >>> 24) == 0) transparent++; else visible++; }
                String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes.array()));
                var frames = new ArrayList<String>();
                if (image.getWidth()==64 && image.getHeight()==32) for (int frame = 0; frame < 2; frame++) {
                    var framePixels = java.nio.ByteBuffer.allocate(32*32*4);
                    for (int py=0;py<32;py++) for (int px=0;px<32;px++) framePixels.putInt(image.getPixel(frame*32+px,py));
                    frames.add(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(framePixels.array())));
                }
                decodedTextures.put(id, Map.of("width",image.getWidth(),"height",image.getHeight(),"visiblePixels",visible,
                    "transparentPixels",transparent,"decodedPixelSha256",hash,"decodedFrameSha256",List.copyOf(frames)));
                if (visible == 0 || transparent == 0 || frames.size()!=2 || frames.getFirst().equals(frames.getLast())) violation("invalid two-state tuck texture="+id);
            }
        } catch (Exception failure) { throw new IllegalStateException("Decode actual runtime tuck texture "+id,failure); }
    }

    public static void soundRequested(String action, boolean retained) {
        soundRequests.merge(action,1L,Long::sum);
        if (!retained) rejectedSoundRequests++;
    }
    public static void soundBefore(String action, boolean preview, long generation) {
        soundAction = action; soundStarted = System.nanoTime(); soundCalls = 0;
        soundPreview = preview; soundGeneration = generation;
    }
    public static void nativeSound(SoundInstance sound) {
        if (soundAction == null) return;
        soundCalls++;
        var request = (com.salts_inventory_update.controlify.qa.mixin.FeatureSoundRequestAccessor)sound;
        float volume = request.saltsQaRequestVolume(), pitch = request.saltsQaRequestPitch();
        var row = new LinkedHashMap<String,Object>();
        row.put("action",soundAction); row.put("scenario",QaFeatureProbe.scenario); row.put("atNanos",soundStarted);
        var action = com.salts_inventory_update.controlify.WorkspaceUiSounds.Action.valueOf(soundAction);
        row.put("preview",soundPreview); row.put("generation",soundGeneration); row.put("priority",action.priority().name());
        row.put("instance",soundInstances.computeIfAbsent(sound, ignored -> ++nextSoundInstance));
        row.put("nativeOrder",++nativeSoundOrder);
        row.put("identifier",sound.getIdentifier().toString()); row.put("volume",volume); row.put("pitch",pitch);
        row.put("measurement","native-request-fields");
        row.put("source",sound.getSource().name()); row.put("relative",sound.isRelative()); row.put("looping",sound.isLooping());
        row.put("delay",sound.getDelay()); acceptedSounds.add(Map.copyOf(row)); soundAccepted.merge(soundAction,1L,Long::sum);
        if (!sound.getIdentifier().equals(action.event()) || volume <= 0 || volume > action.volume()+.000001
                || Math.abs(pitch-1)> .000001 || !sound.getSource().name().equals("UI")
                || !sound.isRelative() || sound.isLooping() || sound.getDelay()!=0) violation("unexpected actual native workspace sound="+row);
    }
    public static void nativeStop(SoundInstance sound) {
        Long instance = soundInstances.get(sound);
        if (instance == null) return;
        nativeSoundStops.add(Map.of("instance",instance,"nativeOrder",++nativeSoundOrder,"atNanos",System.nanoTime(),
            "identifier",sound.getIdentifier().toString(),"scenario",QaFeatureProbe.scenario,
            "duringAction",soundAction == null ? "" : soundAction));
    }
    public static boolean navigationRestartsAreOrdered(List<Map<String,Object>> burst) {
        if (burst.size() < 2) return false;
        for (int index=1;index<burst.size();index++) {
            var before=burst.get(index-1); var after=burst.get(index);
            long beforeOrder=((Number)before.get("nativeOrder")).longValue(),afterOrder=((Number)after.get("nativeOrder")).longValue();
            if (nativeSoundStops.stream().noneMatch(stop -> stop.get("instance").equals(before.get("instance"))
                    && ((Number)stop.get("nativeOrder")).longValue()>beforeOrder && ((Number)stop.get("nativeOrder")).longValue()<afterOrder
                    && stop.get("scenario").equals(after.get("scenario")))) return false;
        }
        return true;
    }
    public static void soundAfter(boolean played) {
        if (soundAction == null) return;
        if (played && soundCalls != 1 || !played && soundCalls != 0) violation("sound helper/native dispatch mismatch="+soundAction+":"+played+":"+soundCalls);
        if (!played) rejectedSoundRequests++; soundAction = null;
    }
    public static List<Map<String,Object>> sounds() { return List.copyOf(acceptedSounds); }
    public static long acceptedSoundCount() { return soundAccepted.values().stream().mapToLong(Long::longValue).sum(); }
    public static long rejectedSoundCount() { return rejectedSoundRequests; }
    public static boolean soundPolicyBounded() {
        Long generation=null, previous=null, navigation=null, result=null;
        com.salts_inventory_update.controlify.model.UiSoundPolicy.Priority previousPriority=null;
        var previousKinds = new LinkedHashMap<String,Long>();
        for (var row : acceptedSounds) {
            if (Boolean.TRUE.equals(row.get("preview"))) continue;
            long epoch=((Number)row.get("generation")).longValue(),at=((Number)row.get("atNanos")).longValue();
            if (generation==null || generation!=epoch) { previous=navigation=result=null;previousPriority=null;previousKinds.clear();generation=epoch; }
            var action=com.salts_inventory_update.controlify.WorkspaceUiSounds.Action.valueOf((String)row.get("action"));
            Long prior=previousKinds.put(action.name(),at);
            if (prior!=null && at-prior<(action.intervalMillis()-2)*1_000_000L) return false;
            if (action.priority()==com.salts_inventory_update.controlify.model.UiSoundPolicy.Priority.RESULT) {
                if(result!=null && at-result<88_000_000L) return false;
                result=at;
            } else {
                boolean fast=action.priority()==com.salts_inventory_update.controlify.model.UiSoundPolicy.Priority.NAVIGATION && action.intervalMillis()==0;
                if(previous!=null && !(fast && previousPriority==com.salts_inventory_update.controlify.model.UiSoundPolicy.Priority.NAVIGATION)
                    && at-previous<88_000_000L) return false;
                if(action.priority()==com.salts_inventory_update.controlify.model.UiSoundPolicy.Priority.NAVIGATION) {
                    if(!fast && navigation!=null && at-navigation<198_000_000L)return false;
                    navigation=at;
                }
            }
            previous=at; previousPriority=action.priority();
        }
        return !acceptedSounds.isEmpty();
    }
    public static void overlayViewport(int width,int height) { overlayWidth = width; overlayHeight = height; }
    public static void alphabetBegin() { alphabetGroups.clear(); alphabetBounds.clear(); }
    private static final List<Map<String,Object>> alphabetLanes=new ArrayList<>();
    public static long alphabetInsideLabels,alphabetOutsideLabels;
    public static void alphabetBefore(AlphabeticalRecipeIndex.Group group,com.salts_inventory_update.controlify.model.AlphabeticalMarkerLayout.Placement placement) {
        alphabetGroup = group; alphabetNativeTextCalls = alphabetMarkerAnchorCalls = 0;
        double radius=Math.hypot(placement.labelX()-380,placement.labelY()-221);
        boolean inside=radius<144;
        if(inside) alphabetInsideLabels++;else alphabetOutsideLabels++;
        if(alphabetLanes.size()<26 || alphabetLanes.stream().anyMatch(row -> row.get("letter").equals(group.letter()))) {
            alphabetLanes.removeIf(row -> row.get("letter").equals(group.letter()));
            alphabetLanes.add(Map.of("letter",group.letter(),"firstIndex",group.firstIndex(),"inside",inside,"radius",radius,
                "labelX",placement.labelX(),"labelY",placement.labelY(),"anchorX",placement.anchorX(),"anchorY",placement.anchorY()));
        }
        if(group.firstIndex()==0 && (Math.abs(placement.labelX()-380)>16 || Math.abs(placement.labelY()-89)>16))
            violation("first alphabetical label displaced too far from its north inside-ring position="+placement);
    }
    public static void alphabetText(GuiGraphicsExtractor graphics, Font font, Component text,int x,int y,int color) {
        if (alphabetGroup != null && text.getString().equals(alphabetGroup.letter())) {
            if ((color>>>24)==0) { violation("alphabet label submitted as invisible native text="+alphabetGroup); return; }
            alphabetNativeTextCalls++; alphabetNativeTextDraws++;
            // Native centered text includes a one-pixel shadow; include it in the visible bounds.
            var pose = graphics.pose(); int width=font.width(text)+1,height=font.lineHeight+1;
            double x1=pose.m00()*x+pose.m10()*y+pose.m20(),y1=pose.m01()*x+pose.m11()*y+pose.m21();
            double x2=pose.m00()*(x+width)+pose.m10()*(y+height)+pose.m20(),y2=pose.m01()*(x+width)+pose.m11()*(y+height)+pose.m21();
            var bounds = new AlphabetBounds(alphabetGroup.letter(),alphabetGroup.firstIndex(),Math.min(x1,x2),Math.min(y1,y2),Math.max(x1,x2),Math.max(y1,y2));
            alphabetBounds.add(bounds);
            if (QaContextualPresentationProbe.settled() && (bounds.left()<-.5 || bounds.top()<-.5 || bounds.right()>overlayWidth+.5 || bounds.bottom()>overlayHeight+.5)
                    && alphabetBoundsViolations.size()<32) alphabetBoundsViolations.add("scenario="+QaFeatureProbe.scenario+",label="+bounds+",content="+overlayWidth+","+overlayHeight);
        }
    }
    public static void alphabetAnchorBefore(AlphabeticalRecipeIndex.Group group,int count,int x,int y) {
        alphabetMarkerAnchorCalls++;
        if (!group.equals(alphabetGroup) || count<=0 || group.firstIndex()<0 || group.firstIndex()>=count)
            violation("alphabet native anchor is not associated with its current rendered group="+group+",marker="+alphabetGroup+",count="+count);
        double angle=group.firstIndex()*Math.PI*2/count;
        int expectedX=380+(int)Math.round(Math.sin(angle)*144),expectedY=221-(int)Math.round(Math.cos(angle)*144);
        if (x!=expectedX || y!=expectedY) violation("alphabet first-recipe anchor moved="+group+":"+x+","+y+" expected="+expectedX+","+expectedY);
        alphabetAnchorX=x;alphabetAnchorY=y;alphabetAnchorScope=true;alphabetAnchorDrawn=false;
    }
    public static void alphabetAnchorTexture(Identifier texture,int x,int y,int width,int height,int color) {
        if (alphabetAnchorScope && texture.equals(com.salts_inventory_update.controlify.render.ContextualRadialAssets.textureId(
                com.salts_inventory_update.controlify.render.ContextualRadialAssets.Element.ALPHABET_ANCHOR))
                && width==2 && height==2 && x==alphabetAnchorX-1 && y==alphabetAnchorY-1 && (color>>>24)>0) alphabetAnchorDrawn=true;
    }
    public static void alphabetAnchorAfter() {
        if (!alphabetAnchorDrawn) violation("alphabet anchor did not draw actual native first-recipe tick");
        else alphabetAnchorTicks++;
        alphabetAnchorScope=false;
    }
    public static void alphabetAfter() {
        if (alphabetGroup == null) return;
        alphabetGroups.add(alphabetGroup); alphabetMarkers++;
        if (alphabetNativeTextCalls != 1 || alphabetMarkerAnchorCalls != 1 || !alphabetGroup.letter().matches("[A-Z]"))
            violation("alphabet group must draw exactly one native visible letter and associated anchor="+alphabetGroup+":"+alphabetNativeTextCalls+":"+alphabetMarkerAnchorCalls);
        alphabetGroup = null;
    }
    public static void alphabetEnd() {
        alphabetFrames++; alphabetBoundsFrames++;
        if (alphabetBounds.size()!=alphabetGroups.size()) violation("actual alphabet text count differs from preserved groups");
        for (int a=0;a<alphabetBounds.size();a++) for (int b=a+1;b<alphabetBounds.size();b++) {
            var first=alphabetBounds.get(a);var second=alphabetBounds.get(b);
            if (Math.min(first.right(),second.right())-Math.max(first.left(),second.left())>.25
                    && Math.min(first.bottom(),second.bottom())-Math.max(first.top(),second.top())>.25
                    && alphabetOverlapViolations.size()<32) alphabetOverlapViolations.add("scenario="+QaFeatureProbe.scenario+",labels="+first+","+second);
        }
    }
    public static Map<String,Object> snapshot() {
        var result = new LinkedHashMap<String,Object>();
        result.put("visualWorkspaceFrames",workspaceFrames); result.put("inactivePreviewFrames",previewFrames);
        result.put("inactiveNativeItemDraws",previewItemDraws); result.put("activeNativePaneDraws",nativePaneDraws);
        result.put("texturedTuckFrames",tuckFrames); result.put("texturedTuckIntermediateFrames",tuckIntermediateFrames);
        result.put("texturedTuckStateTextures",List.copyOf(tuckStateTextures)); result.put("decodedTuckTextures",Map.copyOf(decodedTextures));
        result.put("texturedTuckAtlasFrames",List.copyOf(tuckStateFrames));
        result.put("workspaceUiSoundRequests",Map.copyOf(soundRequests)); result.put("workspaceUiSoundAccepted",Map.copyOf(soundAccepted));
        result.put("workspaceUiSoundSuppressedRequests",rejectedSoundRequests); result.put("workspaceUiNativeSounds",List.copyOf(acceptedSounds));
        result.put("workspaceUiNativeSoundStops",List.copyOf(nativeSoundStops));
        result.put("nativeAlphabetFrames",alphabetFrames); result.put("nativeAlphabetMarkers",alphabetMarkers);
        result.put("nativeAlphabetTextDraws",alphabetNativeTextDraws); result.put("lastNativeAlphabetGroups",List.copyOf(alphabetGroups));
        result.put("nativeAlphabetLabelBoundsFrames",alphabetBoundsFrames); result.put("nativeAlphabetAnchorTicks",alphabetAnchorTicks);
        result.put("nativeAlphabetInsideLabels",alphabetInsideLabels);result.put("nativeAlphabetOutsideLabels",alphabetOutsideLabels);
        result.put("lastNativeAlphabetLabelLanes",List.copyOf(alphabetLanes));
        result.put("nativeAlphabetLabelOverlapViolations",List.copyOf(alphabetOverlapViolations));
        result.put("nativeAlphabetLabelBoundsViolations",List.copyOf(alphabetBoundsViolations));
        result.put("lastNativeAlphabetLabelBounds",List.copyOf(alphabetBounds));
        result.put("visualFeatureViolations",List.copyOf(violations)); return result;
    }
}
