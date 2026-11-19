package com.aventura.cielo.mixin;
import com.aventura.cielo.CieloMod;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** La Ciudadela conserva sus trampas de pistones; las nuevas expediciones no necesitan bloques moviles. */
@Mixin(PistonBaseBlock.class)
public class PistonProtegidoMixin {
    @Inject(method="isPushable",at=@At("HEAD"),cancellable=true)
    private static void cielo$noEmpujar(BlockState state,Level level,BlockPos pos,Direction movement,boolean destroy,
        Direction piston,CallbackInfoReturnable<Boolean> cir) {
        TagKey<Structure> tag=TagKey.create(Registries.STRUCTURE,CieloMod.id("expediciones"));
        if(level instanceof ServerLevel server && server.structureManager().getStructureWithPieceAt(pos,h -> h.is(tag)).isValid())
            cir.setReturnValue(false);
    }
}
