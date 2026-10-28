package com.salts_inventory_update.compat.alloyforgery.mixin;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

public final class AlloyForgeryMixinPlugin implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target,String mixin){
        try{
            var bytecode=MixinService.getService().getBytecodeProvider();
            bytecode.getClassNode("wraith.alloyforgery.client.AlloyForgeScreen");
            return bytecode.getClassNode(target).methods.stream().anyMatch(m->m.name.equals("applyState"));
        }catch(ClassNotFoundException|IOException absent){return false;}
    }
    @Override public void onLoad(String name){ }
    @Override public String getRefMapperConfig(){return null;}
    @Override public void acceptTargets(Set<String> mine,Set<String> others){ }
    @Override public List<String> getMixins(){return null;}
    @Override public void preApply(String target,ClassNode node,String mixin,IMixinInfo info){ }
    @Override public void postApply(String target,ClassNode node,String mixin,IMixinInfo info){ }
}
