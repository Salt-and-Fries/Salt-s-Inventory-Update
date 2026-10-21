package com.salts_inventory_update.mixin.client.mousetweaks;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Detect optional API bytecode without loading client/game classes during mixin discovery. */
public final class MouseTweaksMixinPlugin implements IMixinConfigPlugin {
    private boolean present;
    @Override public void onLoad(String mixinPackage) {
        try {
            MixinService.getService().getBytecodeProvider().getClassNode("yalter.mousetweaks.api.IMTModGuiContainer3Ex");
            this.present = true;
        } catch (ClassNotFoundException | IOException absent) { this.present = false; }
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        return !mixin.contains(".mousetweaks.") || this.present;
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
