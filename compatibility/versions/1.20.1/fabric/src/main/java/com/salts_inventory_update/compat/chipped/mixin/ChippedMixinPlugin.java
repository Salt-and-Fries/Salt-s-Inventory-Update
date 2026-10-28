package com.salts_inventory_update.compat.chipped.mixin;
import java.io.IOException;
import java.util.*;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
import org.spongepowered.asm.service.MixinService;
public final class ChippedMixinPlugin implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target,String mixin){
        try{MixinService.getService().getBytecodeProvider().getClassNode(target);return true;}
        catch(ClassNotFoundException|IOException absent){return false;}
    }
    @Override public void onLoad(String name){}
    @Override public String getRefMapperConfig(){return null;}
    @Override public void acceptTargets(Set<String> mine,Set<String> others){}
    @Override public List<String> getMixins(){return null;}
    @Override public void preApply(String t,ClassNode n,String m,IMixinInfo i){}
    @Override public void postApply(String t,ClassNode n,String m,IMixinInfo i){}
}
