package com.salts_inventory_update.compat.feedingtrough.mixin;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Inspect optional bytecode without defining upstream classes during loader bootstrap. */
public final class FeedingTroughMixinPlugin implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try {
            var target = MixinService.getService().getBytecodeProvider().getClassNode(targetClassName);
            return target.methods.stream().anyMatch(method ->
                Set.of("stillValid", "method_7597", "m_6875_").contains(method.name));
        } catch (ClassNotFoundException | IOException absent) {
            return false;
        }
    }
    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> other) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
