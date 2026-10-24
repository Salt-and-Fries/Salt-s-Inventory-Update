package com.salts_inventory_update.compat.trinkets.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Do not resolve any native Trinkets class unless this tested ABI is installed. */
public final class TrinketsMixinPlugin implements IMixinConfigPlugin {
    private boolean enabled;
    @Override public void onLoad(String pkg) {
        enabled = FabricLoader.getInstance().getModContainer("trinkets")
            .map(m -> m.getMetadata().getVersion().getFriendlyString().equals("3.10.0")).orElse(false);
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) { return enabled; }
    @Override public String getRefMapperConfig() { return null; }
    @Override public List<String> getMixins() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) { }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
