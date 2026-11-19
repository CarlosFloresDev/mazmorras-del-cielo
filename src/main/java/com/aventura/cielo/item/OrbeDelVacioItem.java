package com.aventura.cielo.item;
import com.aventura.cielo.world.ZonaCiudadela;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Paso de hasta ocho bloques: exige ruta libre, suelo y ausencia de zona protegida en todo el trayecto. */
public class OrbeDelVacioItem extends Item {
    public OrbeDelVacioItem(Properties p) { super(p); }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
        if(level instanceof ServerLevel server) {
            if(player.getCooldowns().isOnCooldown(player.getItemInHand(hand))) return InteractionResult.FAIL;
            Vec3 start=player.position(), dir=player.getLookAngle().multiply(1,0,1).normalize(), safe=null;
            if(dir.lengthSqr()<0.1) return InteractionResult.FAIL;
            for(double d=0.5;d<=8;d+=0.5) {
                Vec3 to=start.add(dir.scale(d)); BlockPos pos=BlockPos.containing(to);
                if(!server.hasChunkAt(pos) || ZonaCiudadela.enZona(level,pos) || !server.noCollision(player,player.getBoundingBox().move(to.subtract(start)))) break;
                if(server.getBlockState(pos.below()).isSolid() && server.getFluidState(pos).isEmpty()) safe=to;
            }
            if(safe==null || ZonaCiudadela.enZona(level,player.blockPosition())) return InteractionResult.FAIL;
            player.teleportTo(safe.x,safe.y,safe.z); player.getCooldowns().addCooldown(player.getItemInHand(hand),120);
            server.playSound(null,player.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1,1);
        }
        return InteractionResult.SUCCESS;
    }
}
