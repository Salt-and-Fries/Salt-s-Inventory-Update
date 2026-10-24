package com.salts_inventory_update.compat.sophisticated.mixin;

import java.util.List;
import java.util.Set;
import java.io.IOException;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import net.neoforged.fml.loading.LoadingModList;

/** Keeps the optional mixin set completely dormant when neither Sophisticated mod is installed. */
public final class SophisticatedMixinPlugin implements IMixinConfigPlugin {
    private boolean enabled;
    private boolean backpacksEnabled;
    private boolean storageEnabled;

    @Override
    public void onLoad(String mixinPackage) {
        LoadingModList mods = LoadingModList.get();
        this.backpacksEnabled = mods != null && mods.getModFileById("sophisticatedbackpacks") != null;
        this.storageEnabled = mods != null && mods.getModFileById("sophisticatedstorage") != null;
        this.enabled = this.backpacksEnabled || this.storageEnabled;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(".CraftingUpgradeTabMixin")
            || mixinClassName.endsWith(".CraftingUpgradeTupleTabMixin")) {
            if (!this.enabled) return false;
            try {
                String descriptor = mixinClassName.endsWith(".CraftingUpgradeTupleTabMixin")
                    ? "Lnet/minecraft/util/Tuple;" : "Lcom/mojang/datafixers/util/Pair;";
                return matchesCraftingBounds(MixinService.getService().getBytecodeProvider()
                    .getClassNode(targetClassName), descriptor);
            } catch (IOException | ClassNotFoundException failure) {
                throw new IllegalStateException("Unable to inspect native crafting popup layout", failure);
            }
        }
        if (mixinClassName.endsWith(".BackpackContextItemAccessor")
            || mixinClassName.endsWith(".ServerGamePacketListenerItemLockMixin")) {
            return this.backpacksEnabled;
        }
        if (mixinClassName.endsWith(".SophisticatedStorageOpenersMixin")) {
            return this.storageEnabled;
        }
        return this.enabled;
    }

    /** Select a native ABI without initializing any client classes. */
    static boolean matchesCraftingBounds(ClassNode target, String expected) {
        String actual = target.fields.stream().filter(field -> field.name.equals("resultListPositionDimensions"))
            .map(field -> field.desc).findFirst().orElseThrow(() ->
                new IllegalStateException("Native crafting popup bounds field is missing"));
        if (!actual.equals("Lcom/mojang/datafixers/util/Pair;") && !actual.equals("Lnet/minecraft/util/Tuple;")) {
            throw new IllegalStateException("Unsupported native crafting popup bounds: " + actual);
        }
        return actual.equals(expected);
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
