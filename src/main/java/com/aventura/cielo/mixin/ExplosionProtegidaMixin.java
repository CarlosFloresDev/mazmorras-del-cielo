package com.aventura.cielo.mixin;
import com.aventura.cielo.world.ZonaCiudadela;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Filtra tambien explosiones con calculadores propios; conserva el daño a entidades. */
@Mixin(ServerExplosion.class)
public class ExplosionProtegidaMixin {
    @Inject(method="calculateExplodedPositions",at=@At("RETURN"),cancellable=true)
    private void cielo$proteger(CallbackInfoReturnable<List<BlockPos>> cir) {
        var level=((ServerExplosion)(Object)this).level();
        List<BlockPos> positions=new ArrayList<>(cir.getReturnValue());
        positions.removeIf(p -> ZonaCiudadela.enZona(level,p));
        cir.setReturnValue(positions);
    }
}
