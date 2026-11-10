package com.salts_inventory_update.compat.zenith.mixin;

import com.salts_inventory_update.compat.zenith.ZenithClient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Preserve native widgets, narration and navigation; return only SIU-opened views to their owner. */
@Pseudo
@Mixin(targets="dev.shadowsoffire.apotheosis.ench.table.EnchantingInfoScreen",remap=false)
public abstract class ZenithInformationReturnMixin extends Screen {
    protected ZenithInformationReturnMixin(Component title){super(title);}
    @Override public void onClose(){if(!ZenithClient.returnInformation(this))super.onClose();}
    @Override public void tick(){super.tick();ZenithClient.tickInformation(this);}
}
