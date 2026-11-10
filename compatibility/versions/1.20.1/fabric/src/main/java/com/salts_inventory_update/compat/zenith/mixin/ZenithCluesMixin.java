package com.salts_inventory_update.compat.zenith.mixin;

import com.salts_inventory_update.compat.zenith.ZenithWindows;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="dev.shadowsoffire.apotheosis.ench.table.ClueMessage",remap=false)
public abstract class ZenithCluesMixin {
    @Inject(method="sendTo",at=@At("HEAD"),cancellable=true,remap=false)
    private static void salts$sessionClues(int offer,List<EnchantmentInstance> clues,boolean all,Player player,CallbackInfo ci){
        var data=new FriendlyByteBuf(Unpooled.buffer());
        try{
            data.writeByte(clues.size());
            for(var clue:clues){data.writeShort(BuiltInRegistries.ENCHANTMENT.getId(clue.enchantment));data.writeByte(clue.level);}
            data.writeByte(offer);data.writeBoolean(all);byte[] bytes=new byte[data.readableBytes()];data.getBytes(data.readerIndex(),bytes);
            if(ZenithWindows.sendEnchanting(player,ZenithWindows.CLUES,bytes))ci.cancel();
        }finally{data.release();}
    }
}
