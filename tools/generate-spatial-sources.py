"""Adapt the shared Spatial GUI implementation to pinned Minecraft/loader APIs.

The 26.3 implementation is the source of truth for projection, transitions and
input ownership. Adapters cover renamed vanilla APIs and native HUD composition;
generated files are build outputs, never a second implementation to maintain.
"""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'com.salts_inventory_update.spatial'
BASE = ROOT / 'core-integrations/spatial-gui/src/main'


def replace_once(source, old, new):
    if source.count(old) != 1:
        raise ValueError(f'Spatial template drift: expected one {old!r}')
    return source.replace(old, new)


def native_hud(mc, loader):
    legacy = mc == '1.20.1'
    wrapped = mc in ('1.20.1', '1.21.1', '1.21.11')
    separate = mc in ('26.2', '26.3')
    graphics = 'GuiGraphics' if wrapped else 'GuiGraphicsExtractor'
    hud = 'Hud' if separate else 'Gui'
    target = 'net.minecraftforge.client.gui.overlay.ForgeGui' if loader == 'forge' else hud
    tracker = 'float' if legacy else 'DeltaTracker'
    method = 'render' if wrapped else 'extractRenderState'
    grouped = 'renderHotbarAndDecorations' if wrapped else 'extractHotbarAndDecorations'
    raw = f'com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor.wrap(graphics)' if wrapped else 'graphics'
    extra = ''
    if loader == 'neoforge':
        capture = '''
    @Shadow @Final private net.neoforged.neoforge.client.gui.GuiLayerManager layerManager;
    public void salts$extractHotbarAndDecorations(GRAPHICS graphics, TRACKER tracker) {
        boolean previous=SpatialHudPass.capturing;
        SpatialHudPass.capturing=true;
        try {layerManager.render(graphics,tracker);}finally {SpatialHudPass.capturing=previous;}
    }
'''
    elif loader == 'forge':
        capture = '''
    @Shadow public abstract void render(GRAPHICS graphics, TRACKER tracker);
    public void salts$extractHotbarAndDecorations(GRAPHICS graphics, TRACKER tracker) {
        boolean previous=SpatialHudPass.capturing;
        SpatialHudPass.capturing=true;
        try {render(graphics,tracker);}finally {SpatialHudPass.capturing=previous;}
    }
'''
    elif legacy:
        capture = '''
    @Shadow private int screenWidth,screenHeight;
    @Invoker("renderHotbar") protected abstract void salts$hotbar(float tick,GuiGraphics graphics);
    @Invoker("renderPlayerHealth") protected abstract void salts$health(GuiGraphics graphics);
    @Invoker("renderVehicleHealth") protected abstract void salts$vehicle(GuiGraphics graphics);
    @Invoker("renderExperienceBar") protected abstract void salts$experience(GuiGraphics graphics,int x);
    @Invoker("renderJumpMeter") protected abstract void salts$jump(net.minecraft.world.entity.PlayerRideableJumping mount,GuiGraphics graphics,int x);
    @Invoker("renderSelectedItemName") protected abstract void salts$name(GuiGraphics graphics);
    public void salts$extractHotbarAndDecorations(GuiGraphics graphics,float tracker) {
        var mc=net.minecraft.client.Minecraft.getInstance();
        screenWidth=mc.getWindow().getGuiScaledWidth();screenHeight=mc.getWindow().getGuiScaledHeight();
        if(mc.options.hideGui||mc.player==null||mc.player.isSpectator())return;
        salts$hotbar(tracker,graphics);
        if(mc.gameMode.canHurtPlayer()){salts$health(graphics);salts$vehicle(graphics);}
        var jump=mc.player.jumpableVehicle();
        if(jump!=null)salts$jump(jump,graphics,screenWidth/2-91);
        else if(mc.gameMode.hasExperience())salts$experience(graphics,screenWidth/2-91);
        salts$name(graphics);
    }
'''
        extra = '''
    @Inject(method={"renderHotbar","renderPlayerHealth","renderVehicleHealth","renderExperienceBar","renderJumpMeter","renderSelectedItemName"},at=@At("HEAD"),cancellable=true)
    private void salts$nativeHotbar(CallbackInfo ci) {
        if(SpatialGuiIntegration.active()&&!SpatialGuiIntegration.movement()&&!SpatialGUIRenderer.isExtractingScreen)ci.cancel();
    }
'''
    else:
        capture = '''
    @Invoker("GROUPED") public abstract void salts$extractHotbarAndDecorations(GRAPHICS graphics,TRACKER tracker);
    @Inject(method="GROUPED",at=@At("HEAD"),cancellable=true)
    private void salts$nativeHotbar(CallbackInfo ci) {
        if(SpatialGuiIntegration.active()&&!SpatialGuiIntegration.movement()&&!SpatialGUIRenderer.isExtractingScreen)ci.cancel();
    }
'''
    capture = capture.replace('GROUPED', grouped).replace('GRAPHICS', graphics).replace('TRACKER', tracker)
    return f'''package {PACKAGE}.mixin;
import {PACKAGE}.*;
import net.minecraft.client.gui.{graphics};
import net.minecraft.client.gui.{hud};
{'' if legacy else 'import net.minecraft.client.DeltaTracker;'}
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;

/** Native HUD composition is preserved, including Forge/NeoForge's independent layers. */
@Mixin(value={target}.class,priority=1500)
public abstract class SpatialNativeHudMixin implements SpatialHudAccess {{
    @Inject(method="{method}",at=@At("TAIL"))
    private void salts$unclippedCarriedFeedback({graphics} graphics,{tracker} tracker,CallbackInfo ci) {{
        if(!SpatialGuiIntegration.active()||SpatialGuiIntegration.movement()||SpatialGUIRenderer.isExtractingScreen||SpatialHudPass.capturing)return;
        var mc=net.minecraft.client.Minecraft.getInstance();double scale=mc.getWindow().getGuiScale();
        int x=(int)net.minecraft.util.Mth.clamp(org.tastytrash.spatialGUI.util.MouseHandlerUtil.getSourceX()/scale,9,mc.getWindow().getGuiScaledWidth()-9);
        int y=(int)net.minecraft.util.Mth.clamp(org.tastytrash.spatialGUI.util.MouseHandlerUtil.getSourceY()/scale,9,mc.getWindow().getGuiScaledHeight()-9);
        SpatialGuiIntegration.desktop().workspaceAccess().renderCarried({raw},x,y);
    }}
{capture}{extra}
}}
'''


def generate(mc, loader, output):
    pins = json.loads((ROOT / 'gradle/spatial-gui-targets.json').read_text(encoding='utf-8'))['targets']
    assert any(t['minecraft'] == mc and t['loader'] == loader for t in pins), (mc, loader)
    output = output.resolve()
    allowed_builds = ((ROOT / 'versions' / mc / loader / 'build').resolve(), (ROOT / 'build').resolve())
    if not any(output.is_relative_to(build) and output != build for build in allowed_builds):
        raise ValueError('Spatial adapters must be generated inside this target or root build directory')
    legacy = mc in ('1.20.1', '1.21.1')
    wrapped = legacy or mc == '1.21.11'
    old_camera = wrapped
    old_screen = mc not in ('26.2', '26.3')
    sources = {}
    for path in (BASE / 'java').rglob('*.java'):
        name = path.name
        if name == 'SpatialNativeHudMixin.java':
            continue  # HUD composition is adapted to each Minecraft/loader below.
        source = path.read_text(encoding='utf-8')
        source = source.replace('Math.clamp(', 'net.minecraft.util.Mth.clamp(')
        source = source.replace('camera.getFov()', f'{PACKAGE}.SpatialPlatform.fov()')
        if mc not in ('26.2', '26.3'):
            source = source.replace('.gameRenderer.mainCamera()', '.gameRenderer.getMainCamera()')
        if old_screen:
            source = source.replace('mc.gui != null && mc.gui.screen()', 'mc.screen')
            source = source.replace('mc.gui.screen()', 'mc.screen').replace('mc.gui.hud', 'mc.gui')
        if legacy:
            source = source.replace('camera.position()', 'camera.getPosition()').replace('camera.xRot()', 'camera.getXRot()').replace('camera.yRot()', 'camera.getYRot()')
            source = source.replace('event.buttonInfo()', 'event.button()')
            source = source.replace('net.minecraft.client.input.MouseButtonEvent', 'com.salts_inventory_update.api.client.input.MouseButtonEvent')
            source = source.replace('Direction.getApproximateNearest(', 'Direction.getNearest(')
            source = source.replace('EntitySelector.CAN_BE_PICKED', 'EntitySelector.CAN_BE_COLLIDED_WITH')
        if mc == '1.20.1':
            source = source.replace('p.blockInteractionRange()', '4.5D').replace('p.entityInteractionRange()', '3.0D')
            source = source.replace('Direction.getNearest(direction)', 'Direction.getNearest(direction.x,direction.y,direction.z)')
            source = source.replace('matrices.mulPose(', 'matrices.mulPoseMatrix(').replace('WORLD_POSE_STACK.mulPose(', 'WORLD_POSE_STACK.mulPoseMatrix(')
        if name == 'SpatialCameraMixin.java' and old_camera:
            world_type = 'net.minecraft.world.level.BlockGetter' if legacy else 'net.minecraft.world.level.Level'
            source = replace_once(source, '@WrapMethod(method = "alignWithEntity")', '@WrapMethod(method = "setup")')
            source = replace_once(source, 'float partialTick, Operation<Void> original', f'{world_type} level, net.minecraft.world.entity.Entity entity, boolean detached, boolean reverse, float partialTick, Operation<Void> original')
            source = replace_once(source, 'original.call(partialTick)', 'original.call(level,entity,detached,reverse,partialTick)')
        if name == 'SpatialMouseMixin.java' and mc != '26.3':
            source = replace_once(source, 'double x, double y, double dx, double dy, Operation<Void>', 'double x, double y, Operation<Void>')
            source = replace_once(source, 'original.call(window, x, y, dx, dy)', 'original.call(window, x, y)')
        if name == 'SpatialAvatarMixin.java' and legacy:
            source = source.replace('net.minecraft.client.renderer.entity.player.AvatarRenderer', 'net.minecraft.client.renderer.entity.player.PlayerRenderer')
            source = source.replace('import net.minecraft.client.renderer.entity.state.AvatarRenderState;', '')
            source = source.replace('net.minecraft.world.entity.Avatar', 'net.minecraft.client.player.AbstractClientPlayer').replace('AvatarRenderer.class', 'PlayerRenderer.class')
            source = replace_once(source, 'extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V', 'render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V')
            source = replace_once(source, 'Avatar avatar, AvatarRenderState state, float tick, Operation<Void> original', 'AbstractClientPlayer avatar, float yaw, float tick, com.mojang.blaze3d.vertex.PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource buffers, int light, Operation<Void> original')
            source = replace_once(source, 'original.call(avatar, state, tick)', 'original.call(avatar,yaw,tick,matrices,buffers,light)')
        if name == 'SpatialDesktopMixin.java':
            if wrapped:
                source = source.replace('net.minecraft.client.gui.GuiGraphicsExtractor', 'com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor')
                source = source.replace('salts$extractHotbarAndDecorations(graphics,', 'salts$extractHotbarAndDecorations(graphics.unwrap(),')
            if mc == '1.20.1': source = source.replace('mc.getDeltaTracker()', 'mc.getFrameTime()')
            if mc == '1.21.1': source = source.replace('mc.getDeltaTracker()', 'mc.getTimer()')
            if legacy:
                source = source.replace('@WrapMethod(method = "mouseClicked")', '@WrapMethod(method = "mouseClicked(Lcom/salts_inventory_update/api/client/input/MouseButtonEvent;Z)Z")')
                source = source.replace('@WrapMethod(method = "mouseReleased")', '@WrapMethod(method = "mouseReleased(Lcom/salts_inventory_update/api/client/input/MouseButtonEvent;)Z")')
        if name == 'SpatialHudAccess.java':
            if wrapped: source = source.replace('net.minecraft.client.gui.GuiGraphicsExtractor', 'net.minecraft.client.gui.GuiGraphics').replace('GuiGraphicsExtractor graphics', 'GuiGraphics graphics')
            if mc == '1.20.1': source = source.replace('import net.minecraft.client.DeltaTracker;', '').replace('DeltaTracker tracker', 'float tracker')
        if name == 'SpatialGuiMixinPlugin.java' and loader != 'fabric':
            prefix = 'net.minecraftforge' if loader == 'forge' else 'net.neoforged'
            source = source.replace('import net.fabricmc.api.EnvType;', '').replace('import net.fabricmc.loader.api.FabricLoader;', f'import {prefix}.fml.loading.LoadingModList;')
            begin = source.index('        var loader = FabricLoader.getInstance();')
            end = source.index('\n    }', begin)
            source = source[:begin] + '''        if (org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().getSide() == org.spongepowered.asm.mixin.MixinEnvironment.Side.SERVER) return;
        var list=LoadingModList.get();
        if(list==null)return;
        for(var mod:list.getMods())if(mod.getModId().equals("spatial_gui")) {
            String version=mod.getVersion().toString();
            supported=version.equals("1.8")||version.startsWith("1.8.")||version.startsWith("1.8+");
            if(!supported)LoggerFactory.getLogger("salts_inventory_update").warn("Spatial GUI {} is outside the tested 1.8 integration; hooks disabled",version);
        }''' + source[end:]
        sources[str(path.relative_to(BASE / 'java'))] = source
    sources['com/salts_inventory_update/spatial/mixin/SpatialNativeHudMixin.java'] = native_hud(mc, loader)
    fov = f'(({PACKAGE}.SpatialFovAccess)net.minecraft.client.Minecraft.getInstance().gameRenderer).salts$fov()' if old_camera else f'net.minecraft.client.Minecraft.getInstance().gameRenderer.{"getMainCamera" if mc == "26.1.2" else "mainCamera"}().getFov()'
    sources['com/salts_inventory_update/spatial/SpatialPlatform.java'] = f'package {PACKAGE};\npublic final class SpatialPlatform {{ private SpatialPlatform() {{ }} public static double fov() {{return {fov};}} }}\n'
    if old_camera:
        result_type = 'float' if mc == '1.21.11' else 'double'
        sources['com/salts_inventory_update/spatial/SpatialFovAccess.java'] = f'package {PACKAGE};\npublic interface SpatialFovAccess {{ double salts$fov(); }}\n'
        sources['com/salts_inventory_update/spatial/mixin/SpatialFovMixin.java'] = f'''package {PACKAGE}.mixin;
import {PACKAGE}.SpatialFovAccess;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=GameRenderer.class,priority=500)
public class SpatialFovMixin implements SpatialFovAccess {{
    @Unique private double salts$fov=70;
    public double salts$fov(){{return salts$fov;}}
    @ModifyReturnValue(method="getFov",at=@At("RETURN"))
    private {result_type} salts$actualFov({result_type} value,Camera camera,float tick,boolean setting){{if(setting)salts$fov=value;return value;}}
}}
'''
    sources['com/salts_inventory_update/spatial/SpatialHudPass.java'] = f'''package {PACKAGE};
public final class SpatialHudPass {{
    private SpatialHudPass() {{ }}
    public static boolean capturing;
    public static boolean hotbar(String namespace,String path) {{
        if(!namespace.equals("minecraft")&&!namespace.equals("forge"))return false;
        return switch(path){{case "hotbar","player_health","armor_level","food_level","air_level","vehicle_health","mount_health","jump_meter","jump_bar","experience_bar","experience_level","contextual_info_bar_background","contextual_info_bar","selected_item_name","item_name","spectator_tooltip"->true;default->false;}};
    }}
    public static boolean suppress(boolean hotbar) {{return capturing?!hotbar:SpatialGuiIntegration.active()&&!SpatialGuiIntegration.movement()&&hotbar;}}
}}
'''
    if loader != 'fabric':
        event = 'net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre' if loader == 'forge' else 'net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre'
        identifier = 'event.getOverlay().id()' if loader == 'forge' else 'event.getName()'
        sources['com/salts_inventory_update/spatial/mixin/SpatialHudLayerMixin.java'] = f'''package {PACKAGE}.mixin;
import {PACKAGE}.SpatialHudPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value={event}.class,remap=false)
public class SpatialHudLayerMixin {{
    @Inject(method="<init>",at=@At("TAIL"))
    private void salts$hotbarCaptureScope(CallbackInfo ci) {{
        var event=({event})(Object)this;var id={identifier};
        if(SpatialHudPass.suppress(SpatialHudPass.hotbar(id.getNamespace(),id.getPath())))event.setCanceled(true);
    }}
}}
'''
    config = json.loads((BASE / 'resources/salts_inventory_update.spatial.mixins.json').read_text(encoding='utf-8'))
    config['compatibilityLevel'] = 'JAVA_17' if mc == '1.20.1' else 'JAVA_21' if old_camera else 'JAVA_25'
    if old_camera: config['client'].append('SpatialFovMixin')
    if loader != 'fabric': config['client'].append('SpatialHudLayerMixin')
    if legacy: config['refmap'] = 'salts_inventory_update.refmap.json'
    # Gradle owns this generated source directory. Remove adapters no longer emitted
    # so an incremental build cannot retain a class from a previous generator version.
    java_output = output / 'java'
    if not java_output.resolve().is_relative_to(output):
        raise ValueError('Generated Java directory must stay inside its build output')
    if java_output.exists():
        for stale in java_output.rglob('*.java'):
            if stale.relative_to(java_output).as_posix() not in sources:
                stale.unlink()
    for relative, source in sources.items():
        file = output / 'java' / relative
        file.parent.mkdir(parents=True, exist_ok=True)
        if not file.exists() or file.read_text(encoding='utf-8') != source: file.write_text(source, encoding='utf-8')
    resource = output / 'resources/salts_inventory_update.spatial.mixins.json'
    resource.parent.mkdir(parents=True, exist_ok=True)
    resource.write_text(json.dumps(config, indent=2) + '\n', encoding='utf-8')
    print(f'Prepared {len(sources)} Spatial GUI source adapters for {mc}/{loader}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--minecraft', required=True)
    parser.add_argument('--loader', choices=('fabric','forge','neoforge'), required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    generate(args.minecraft, args.loader, args.output.resolve())
