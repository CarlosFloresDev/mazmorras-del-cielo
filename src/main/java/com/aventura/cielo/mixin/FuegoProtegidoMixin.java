package com.aventura.cielo.mixin;
import com.aventura.cielo.world.ZonaCiudadela;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public class FuegoProtegidoMixin {
    @Inject(method="checkBurnOut",at=@At("HEAD"),cancellable=true)
    private void cielo$noQuemar(Level level,BlockPos pos,int chance,RandomSource random,int age,CallbackInfo ci) {
        if(ZonaCiudadela.enZona(level,pos)) ci.cancel();
    }
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void cielo$noPropagar(BlockState state,ServerLevel level,BlockPos pos,RandomSource random,CallbackInfo ci) {
        if(ZonaCiudadela.enZona(level,pos)) { level.removeBlock(pos,false); ci.cancel(); }
    }
}
