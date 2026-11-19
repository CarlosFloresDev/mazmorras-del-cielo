package com.aventura.cielo.mixin;
import com.aventura.cielo.world.ZonaCiudadela;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class GriefProtegidoMixin {
    @Inject(method="destroyBlock",at=@At("HEAD"),cancellable=true)
    private void cielo$noDemoler(BlockPos pos,boolean drops,Entity entity,int recursion,CallbackInfoReturnable<Boolean> cir) {
        if(entity instanceof Mob && ZonaCiudadela.enZona((Level)(Object)this,pos)) cir.setReturnValue(false);
    }
}
