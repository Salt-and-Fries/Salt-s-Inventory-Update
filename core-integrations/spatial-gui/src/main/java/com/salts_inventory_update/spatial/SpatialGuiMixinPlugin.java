package com.salts_inventory_update.spatial;

import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Runs before game classes load; never resolves Spatial GUI on a server or an absent-mod client. */
public final class SpatialGuiMixinPlugin implements IMixinConfigPlugin {
    private boolean supported;
    @Override public void onLoad(String pkg) {
        var loader = FabricLoader.getInstance();
        if (loader.getEnvironmentType() != EnvType.CLIENT) return;
        loader.getModContainer("spatial-gui").ifPresent(mod -> {
            String version = mod.getMetadata().getVersion().getFriendlyString();
            supported = version.equals("1.8") || version.startsWith("1.8.") || version.startsWith("1.8+");
            if (!supported) LoggerFactory.getLogger("salts_inventory_update").warn(
                "Spatial GUI {} is outside the tested 1.8 integration. Salt's Spatial GUI hooks are disabled.", version);
        });
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) { return supported; }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
