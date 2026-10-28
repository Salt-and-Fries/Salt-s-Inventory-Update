package com.salts_inventory_update.compat.chipped.mixin;
import com.salts_inventory_update.compat.chipped.ChippedIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo @Mixin(targets="earth.terrarium.chipped.common.blocks.WorkbenchBlock",remap=false)
abstract class ChippedOpeningMixin {
    @Inject(method={"useWithoutItem","method_55766"},at=@At("HEAD"),cancellable=true,remap=false,require=1)
    private void salts$opening(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){
        if(player instanceof ServerPlayer server){
            boolean side=state.getProperties().stream().filter(p->p.getName().equals("model")).anyMatch(p->salts$value(state,p).equals("side"));
            var main=side?pos.relative(state.getValue(HorizontalDirectionalBlock.FACING).getCounterClockWise()):pos;
            if(ChippedIntegration.open(server,main,state.getBlock().getName()))ci.setReturnValue(InteractionResult.CONSUME);
        }
    }
    @Unique @SuppressWarnings({"rawtypes","unchecked"})
    private static String salts$value(BlockState state,net.minecraft.world.level.block.state.properties.Property p){return state.getValue(p).toString();}
}
