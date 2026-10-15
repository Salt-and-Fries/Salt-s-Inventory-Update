package com.salts_inventory_update.controlify.qa.mixin;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads submitted native parameters without resolving a sound or sampling its random pitch/volume providers. */
@Mixin(AbstractSoundInstance.class)
public interface FeatureSoundRequestAccessor {
    @Accessor("volume") float saltsQaRequestVolume();
    @Accessor("pitch") float saltsQaRequestPitch();
}
