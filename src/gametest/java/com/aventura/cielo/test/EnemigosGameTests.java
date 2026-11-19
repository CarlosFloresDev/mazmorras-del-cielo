package com.aventura.cielo.test;

import com.aventura.cielo.CieloMod;
import com.aventura.cielo.entity.*;
import com.aventura.cielo.registry.CieloEntities;
import com.aventura.cielo.world.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;

/** Regresion: IA real, SIN llamar doHurtTarget, SIN fijar objetivo, SIN resistencia V. */
public class EnemigosGameTests {
    public static List<EntityType<? extends Monster>> hostiles() {
        return List.of(CieloEntities.PIRATA_DEL_CIELO,CieloEntities.ARCABUCERO_DEL_CIELO,
            CieloEntities.MECANICO_DEL_DIRIGIBLE,CieloEntities.GUARDIAN_CELESTE,CieloEntities.BRUJO_DEL_VIENTO,
            CieloEntities.CAPITAN_BARBANUBE,CieloEntities.CONDOR_DE_ACERO,
            CieloEntities.JEAN_BECK,CieloEntities.SENOR_DE_LA_CENIZA,CieloEntities.HERALDO_DEL_VACIO,
            CieloEntities.AYUDANTE_COCINA,CieloEntities.BOMBARDERO_CENIZA,CieloEntities.FUEGO_FATUO,
            CieloEntities.DUELISTA_COCINA,CieloEntities.MAESTRO_RACIONES,CieloEntities.GUARDIA_ESCORIA,
            CieloEntities.CANTOR_BRASAS,CieloEntities.ACECHADOR_VACIO,CieloEntities.TEJEDOR_VACIO);
    }
    @GameTest(maxTicks=620) public void pirataAbierto(GameTestHelper h) { combate(h,0,false); }
    @GameTest(maxTicks=620) public void pirataProtegido(GameTestHelper h) { combate(h,0,true); }
    @GameTest(maxTicks=620) public void arcabuceroAbierto(GameTestHelper h) { combate(h,1,false); }
    @GameTest(maxTicks=620) public void arcabuceroProtegido(GameTestHelper h) { combate(h,1,true); }
    @GameTest(maxTicks=620) public void mecanicoAbierto(GameTestHelper h) { combate(h,2,false); }
    @GameTest(maxTicks=620) public void mecanicoProtegido(GameTestHelper h) { combate(h,2,true); }
    @GameTest(maxTicks=620) public void guardianAbierto(GameTestHelper h) { combate(h,3,false); }
    @GameTest(maxTicks=620) public void guardianProtegido(GameTestHelper h) { combate(h,3,true); }
    @GameTest(maxTicks=620) public void brujoAbierto(GameTestHelper h) { combate(h,4,false); }
    @GameTest(maxTicks=620) public void brujoProtegido(GameTestHelper h) { combate(h,4,true); }
    @GameTest(maxTicks=620) public void capitanAbierto(GameTestHelper h) { combate(h,5,false); }
    @GameTest(maxTicks=620) public void capitanProtegido(GameTestHelper h) { combate(h,5,true); }
    @GameTest(maxTicks=620) public void condorAbierto(GameTestHelper h) { combate(h,6,false); }
    @GameTest(maxTicks=620) public void condorProtegido(GameTestHelper h) { combate(h,6,true); }
    @GameTest(maxTicks=620) public void jeanAbierto(GameTestHelper h) { combate(h,7,false); }
    @GameTest(maxTicks=620) public void jeanProtegido(GameTestHelper h) { combate(h,7,true); }
    @GameTest(maxTicks=620) public void cenizaAbierto(GameTestHelper h) { combate(h,8,false); }
    @GameTest(maxTicks=620) public void cenizaProtegido(GameTestHelper h) { combate(h,8,true); }
    @GameTest(maxTicks=620) public void heraldoAbierto(GameTestHelper h) { combate(h,9,false); }
    @GameTest(maxTicks=620) public void heraldoProtegido(GameTestHelper h) { combate(h,9,true); }
    @GameTest(maxTicks=620) public void ayudanteAbierto(GameTestHelper h) { combate(h,10,false); }
    @GameTest(maxTicks=620) public void ayudanteProtegido(GameTestHelper h) { combate(h,10,true); }
    @GameTest(maxTicks=620) public void bombarderoAbierto(GameTestHelper h) { combate(h,11,false); }
    @GameTest(maxTicks=620) public void bombarderoProtegido(GameTestHelper h) { combate(h,11,true); }
    @GameTest(maxTicks=620) public void fatuoAbierto(GameTestHelper h) { combate(h,12,false); }
    @GameTest(maxTicks=620) public void fatuoProtegido(GameTestHelper h) { combate(h,12,true); }
    @GameTest(maxTicks=620) public void duelistaAbierto(GameTestHelper h) { combate(h,13,false); }
    @GameTest(maxTicks=620) public void duelistaProtegido(GameTestHelper h) { combate(h,13,true); }
    @GameTest(maxTicks=620) public void racionesAbierto(GameTestHelper h) { combate(h,14,false); }
    @GameTest(maxTicks=620) public void racionesProtegido(GameTestHelper h) { combate(h,14,true); }
    @GameTest(maxTicks=620) public void escoriaAbierto(GameTestHelper h) { combate(h,15,false); }
    @GameTest(maxTicks=620) public void escoriaProtegido(GameTestHelper h) { combate(h,15,true); }
    @GameTest(maxTicks=620) public void cantorAbierto(GameTestHelper h) { combate(h,16,false); }
    @GameTest(maxTicks=620) public void cantorProtegido(GameTestHelper h) { combate(h,16,true); }
    @GameTest(maxTicks=620) public void acechadorAbierto(GameTestHelper h) { combate(h,17,false); }
    @GameTest(maxTicks=620) public void acechadorProtegido(GameTestHelper h) { combate(h,17,true); }
    @GameTest(maxTicks=620) public void tejedorAbierto(GameTestHelper h) { combate(h,18,false); }
    @GameTest(maxTicks=620) public void tejedorProtegido(GameTestHelper h) { combate(h,18,true); }
    private void combate(GameTestHelper h,int index,boolean protegida) {
        ServerLevel level=h.getLevel();
        h.assertTrue(level.getDifficulty()!=Difficulty.PEACEFUL,"El entorno de combate requiere dificultad distinta de pacifico");
        int caso=index*2+(protegida?1:0);
        BlockPos center=new BlockPos(100000+caso*256,220,100000);
        List<ChunkPos> chunks=new ArrayList<>();
        for(int x=(center.getX()-16)>>4;x<=(center.getX()+16)>>4;x++) for(int z=(center.getZ()-16)>>4;z<=(center.getZ()+16)>>4;z++) {
            chunks.add(new ChunkPos(x,z)); level.setChunkForced(x,z,true); level.getChunk(x,z);
        }
        // Un inicio REAL registrado activa ZonaCiudadela; poner bloques por si solo no lo hace.
        Structure structure;
        if(protegida) {
            int theme=index<7?-1:(index==7||index==10||index==13||index==14)?0:(index==8||index==11||index==15||index==16)?1:2;
            String id=theme<0?"ciudadela":MazmorraStructure.Tema.values()[theme].getSerializedName();
            structure=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(CieloMod.id(id));
            h.assertTrue(structure!=null,"Falta estructura registrada: "+id);
            StructurePiece piece=theme<0?new CiudadelaPiece(center,35):new MazmorraPiece(center,35,MazmorraStructure.Tema.values()[theme]);
            var start=new StructureStart(structure,ChunkPos.containing(center),0,new PiecesContainer(List.of(piece)));
            level.getChunk(center).setStartForStructure(structure,start);
            for(var chunk:chunks) level.getChunk(chunk.x(),chunk.z()).addReferenceForStructure(structure,ChunkPos.containing(center).pack());
        } else structure=null;
        for(BlockPos p:BlockPos.betweenClosed(center.offset(-16,-1,-16),center.offset(16,16,16))) {
            boolean wall=(Math.abs(p.getX()-center.getX())==16||Math.abs(p.getZ()-center.getZ())==16)&&p.getY()<center.getY()+5;
            level.setBlock(p,p.getY()==center.getY()-1||wall?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        }
        h.runAfterDelay(20,() -> {
            ServerPlayer player=CieloGameTests.jugadorSupervivencia(h,false);
            Monster mob=hostiles().get(index).create(level,EntitySpawnReason.SPAWNER);
            boolean[] done={false};
            Runnable cleanup=() -> {
                if(done[0]) return;
                done[0]=true;
                level.getEntitiesOfClass(Entity.class,new AABB(center).inflate(40),e -> !(e instanceof ServerPlayer)).forEach(Entity::discard);
                level.getServer().getPlayerList().remove(player);
                if(structure!=null) {
                    level.getChunk(center).setStartForStructure(structure,StructureStart.INVALID_START);
                    for(var chunk:chunks) level.getChunk(chunk.x(),chunk.z()).getReferencesForStructure(structure).remove(ChunkPos.containing(center).pack());
                }
                for(var chunk:chunks) level.setChunkForced(chunk.x(),chunk.z(),false);
            };
            try {
                h.assertTrue(mob!=null,"No se creo el enemigo");
                player.teleportTo(center.getX()+0.5,center.getY(),center.getZ()+0.5);
                player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
                player.setHealth(100); player.getFoodData().setFoodLevel(10);
                player.getAbilities().invulnerable=false;
                h.assertTrue(player.getActiveEffects().isEmpty()&&!player.isCreative(),"Jugador de dano no debe tener efectos ni creativo");
                h.assertTrue(!player.isInvulnerableTo(level,player.damageSources().generic()),"Jugador aun protegido por carga del cliente");
                h.assertTrue(ZonaCiudadela.enZona(level,player.blockPosition())==protegida,"La prueba no esta en la zona esperada");
                // Deliberadamente sin finalizeSpawn: cubre spawners con NBT adicional y /summon.
                mob.snapTo(center.getX()+2.5,center.getY(),center.getZ()+0.5);
                mob.setPersistenceRequired(); mob.getRandom().setSeed(537+index);
                level.addFreshEntity(mob);
                h.onEachTick(() -> {
                    if(done[0]) return;
                    if(player.getHealth()<100) {
                        // Sin suelo peligroso ni otros mobs: no aceptar hambre/caida como un acierto.
                        var source=player.getLastDamageSource();
                        if(source!=null&&source.getEntity()==mob) { cleanup.run(); h.succeed(); }
                    }
                });
                h.runAfterDelay(560,() -> {
                    if(done[0]) return;
                    String info=BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType())+" zona="+protegida+" vida="+player.getHealth()
                        +" objetivo="+(mob.getTarget()==player)+" distancia="+mob.distanceTo(player)+" alcance="+mob.isWithinMeleeAttackRange(player)
                        +" vision="+mob.getSensing().hasLineOfSight(player)+" noAI="+mob.isNoAi();
                    cleanup.run(); h.fail("No hubo dano de IA: "+info);
                });
            } catch(RuntimeException error) { cleanup.run(); throw error; }
        });
    }

    @GameTest public void nuevosTienenHuevosYBotin(GameTestHelper h) {
        for(var type:hostiles().subList(13,19)) {
            h.assertTrue(SpawnEggItem.byId(type).isPresent(),"Falta huevo "+type);
            Monster mob=type.create(h.getLevel(),EntitySpawnReason.COMMAND);
            var table=h.getLevel().getServer().reloadableRegistries().getLootTable(mob.getLootTable().orElseThrow());
            var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.THIS_ENTITY,mob)
                .withParameter(LootContextParams.ORIGIN,Vec3.ZERO).withParameter(LootContextParams.DAMAGE_SOURCE,mob.damageSources().generic()).create(LootContextParamSets.ENTITY);
            h.assertTrue(!table.getRandomItems(params,42L).isEmpty(),"Falta botin "+type);
        }
        h.succeed();
    }
    @GameTest public void eliteEsIdempotente(GameTestHelper h) {
        for(var theme:MazmorraStructure.Tema.values()) {
            h.assertTrue(EnemigosExternos.tipos(theme).size()>=3,"Faltan alternativas vanilla");
            for(var type:EnemigosExternos.tipos(theme)) {
                var entity=type.create(h.getLevel(),EntitySpawnReason.STRUCTURE);
                h.assertTrue(entity instanceof Mob,"Tipo externo no es mob: "+type);
                Mob mob=(Mob)entity;
                float health=mob.getMaxHealth();
                EnemigosExternos.aplicarElite(mob);
                h.assertTrue(mob.getMaxHealth()>health&&mob.isPersistenceRequired()&&mob.entityTags().contains(EnemigosExternos.ETIQUETA),"Elite no aplicado");
                float elite=mob.getMaxHealth();
                double armor=mob.getAttributeValue(Attributes.ARMOR);
                EnemigosExternos.aplicarElite(mob);
                h.assertTrue(mob.getMaxHealth()==elite&&mob.getAttributeValue(Attributes.ARMOR)==armor,"Elite duplicado");
            }
        }
        h.succeed();
    }
}
