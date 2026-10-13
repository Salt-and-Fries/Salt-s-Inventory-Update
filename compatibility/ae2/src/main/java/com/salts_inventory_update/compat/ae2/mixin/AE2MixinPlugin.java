package com.salts_inventory_update.compat.ae2.mixin;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/**
 * Checks bytecode without defining optional AE2 or loader classes. Missing AE2
 * disables this mixin set on both clients and dedicated servers. Old builders
 * use their original build(String) path and do not receive the newer hook.
 */
public final class AE2MixinPlugin implements IMixinConfigPlugin {
    private boolean enabled;

    @Override
    public void onLoad(String mixinPackage) {
        try {
            MixinService.getService().getBytecodeProvider().getClassNode("appeng.core.AppEng");
            enabled = true;
        } catch (ClassNotFoundException | IOException missing) {
            enabled = false;
        }
    }
    @Override public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!enabled) return false;
        try {
            ClassNode target = MixinService.getService().getBytecodeProvider().getClassNode(targetClassName);
            if (mixinClassName.endsWith(".UnregisteredMenuTypeBuilderMixin")) {
                return target.methods.stream().anyMatch(method -> method.name.equals("buildUnregistered"));
            }
            return true;
        } catch (ClassNotFoundException | IOException missing) {
            return false;
        }
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
