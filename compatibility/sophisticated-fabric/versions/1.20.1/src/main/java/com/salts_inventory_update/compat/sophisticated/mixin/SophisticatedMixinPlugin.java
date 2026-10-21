package com.salts_inventory_update.compat.sophisticated.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.fabricmc.loader.api.FabricLoader;

/** Keeps the optional mixin set completely dormant when neither Sophisticated mod is installed. */
public final class SophisticatedMixinPlugin implements IMixinConfigPlugin {
    private boolean enabled;
    private boolean backpacksEnabled;
    private boolean storageEnabled;

    @Override
    public void onLoad(String mixinPackage) {
        FabricLoader mods = FabricLoader.getInstance();
        this.backpacksEnabled = mods.isModLoaded("sophisticatedcore") && mods.isModLoaded("sophisticatedbackpacks");
        this.storageEnabled = mods.isModLoaded("sophisticatedcore") && mods.isModLoaded("sophisticatedstorage");
        this.enabled = this.backpacksEnabled || this.storageEnabled;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (targetClassName.startsWith("net.p3pp3rf1y.sophisticatedbackpacks.")) return this.backpacksEnabled;
        if (targetClassName.startsWith("net.p3pp3rf1y.sophisticatedstorage.")) return this.storageEnabled;
        if (mixinClassName.endsWith(".BackpackContextItemAccessor")
            || mixinClassName.endsWith(".ServerGamePacketListenerItemLockMixin")) {
            return this.backpacksEnabled;
        }
        if (mixinClassName.endsWith(".SophisticatedStorageOpenersMixin")) {
            return this.storageEnabled;
        }
        return this.enabled;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
