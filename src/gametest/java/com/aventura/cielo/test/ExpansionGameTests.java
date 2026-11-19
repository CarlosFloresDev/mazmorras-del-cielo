package com.aventura.cielo.test;

import com.aventura.cielo.CieloMod;
import com.aventura.cielo.block.*;
import com.aventura.cielo.entity.*;
import com.aventura.cielo.registry.*;
import com.aventura.cielo.world.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;

public class ExpansionGameTests {
    @GameTest(maxTicks=1200) public void fortinGeneraRuta(GameTestHelper h) { estructura(h,0); }
    @GameTest(maxTicks=1200) public void bastionGeneraRuta(GameTestHelper h) { estructura(h,1); }
    @GameTest(maxTicks=1200) public void santuarioGeneraRuta(GameTestHelper h) { estructura(h,2); }

    public static void cargar(ServerLevel l,BlockPos o,boolean cargar) {
        for(int x=(o.getX()-16)>>4;x<=(o.getX()+112)>>4;x++) for(int z=(o.getZ()-16)>>4;z<=(o.getZ()+16)>>4;z++) {
            l.setChunkForced(x,z,cargar); if(cargar) l.getChunk(x,z);
        }
    }
    public static MazmorraPiece construir(ServerLevel l,BlockPos o,long seed,MazmorraStructure.Tema tema) {
        MazmorraPiece p=new MazmorraPiece(o,seed,tema);
        // Orden inverso al natural: revela dependencias accidentales entre chunks.
        for(int x=(o.getX()+112)>>4;x>=(o.getX()-16)>>4;x--) for(int z=(o.getZ()+16)>>4;z>=(o.getZ()-16)>>4;z--)
            p.postProcess(l,l.structureManager(),l.getChunkSource().getGenerator(),l.getRandom(),
                new BoundingBox(x*16,l.getMinY(),z*16,x*16+15,l.getMaxY(),z*16+15),new ChunkPos(x,z),o);
        return p;
    }
    private void estructura(GameTestHelper h,int i) {
        ServerLevel l=h.getLevel(); BlockPos o=new BlockPos(30000+i*384,150,30000);
        cargar(l,o,true);
        h.runAfterDelay(20,() -> {
            try {
                var tema=MazmorraStructure.Tema.values()[i];
                BlockPos sentinel=o.offset(0,8,0); l.setBlock(sentinel,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
                ChunkPos last=ChunkPos.containing(o.offset(112,0,0));
                new MazmorraPiece(o,740+i,tema).postProcess(l,l.structureManager(),l.getChunkSource().getGenerator(),l.getRandom(),
                    new BoundingBox(last.getMinBlockX(),l.getMinY(),last.getMinBlockZ(),last.getMaxBlockX(),l.getMaxY(),last.getMaxBlockZ()),last,o);
                h.assertTrue(l.getBlockState(sentinel).is(Blocks.DIAMOND_BLOCK),"Generacion escribio fuera del chunk");
                MazmorraPiece p=construir(l,o,740+i,tema);
                h.assertTrue(l.getBlockState(p.nucleo()).is(CieloBlocks.NUCLEO_DEL_JEFE),"Falta nucleo");
                h.assertTrue(l.getBlockState(p.nucleo()).getValue(NucleoDelJefeBlock.JEFE)==p.jefe(),"Jefe incorrecto");
                int loot=0,keys=0;
                for(BlockPos pos:BlockPos.betweenClosed(o.offset(-15,1,-15),o.offset(111,1,15))) {
                    if(l.getBlockEntity(pos) instanceof ChestBlockEntity c) {
                        if(c.getLootTable()!=null) loot++;
                        else if(c.countItem(CieloItems.LLAVE_CELESTIAL)==1) keys++;
                    }
                }
                h.assertTrue(loot==5 && keys==4,"Se esperan cinco cofres de botin y cuatro llaves");
                h.assertTrue(!caminar(l,o,p.entrada()).contains(p.nucleo().above()),"Se puede saltar los sellos");
                for(int gate=0;gate<4;gate++) {
                    var reachable=caminar(l,o,p.entrada());
                    BlockPos key=p.llave(gate);
                    h.assertTrue(reachable.contains(key.north())||reachable.contains(key.south())||reachable.contains(key.east())||reachable.contains(key.west()),"Llave inaccesible: "+gate);
                    h.assertTrue(!reachable.contains(p.sello(gate).east()),"Sello evitable: "+gate);
                    SelloCelestialBlock.abrir(l,p.sello(gate));
                    h.assertTrue(l.getBlockState(p.sello(gate).above(2).north()).isAir(),"Sello parcialmente abierto");
                }
                h.assertTrue(caminar(l,o,p.entrada()).contains(p.nucleo().above()),"Arena inaccesible tras abrir sellos");
                h.assertTrue(!MazmorraStructure.permitido(MazmorraStructure.Tema.VACIO,1000,0),"Santuario demasiado cerca de isla principal");
                // Igual semilla y distinto origen: mismas llaves y variantes.
                var copia=new MazmorraPiece(o.offset(512,0,0),740+i,tema);
                for(int k=0;k<4;k++) {
                    h.assertTrue(copia.llave(k).equals(p.llave(k).offset(512,0,0)),"Plan de llaves no determinista");
                    h.assertTrue(copia.sello(k).equals(p.sello(k).offset(512,0,0)),"Plan de puertas no determinista");
                }
            } finally { cargar(l,o,false); }
            h.succeed();
        });
    }
    private static Set<BlockPos> caminar(ServerLevel l,BlockPos o,BlockPos start) {
        Set<BlockPos> seen=new HashSet<>(); ArrayDeque<BlockPos> q=new ArrayDeque<>(); seen.add(start); q.add(start);
        while(!q.isEmpty()) {
          BlockPos current=q.remove();
          for(Direction d:Direction.Plane.HORIZONTAL) {
            BlockPos next=current.relative(d);
            if(next.getX()<o.getX()-15||next.getX()>o.getX()+111||Math.abs(next.getZ()-o.getZ())>15) continue;
            if(!seen.contains(next)&&l.getBlockState(next.below()).isSolid()&&!l.getBlockState(next).isSolid()&&!l.getBlockState(next.above()).isSolid()) {
                seen.add(next); q.add(next);
            }
          }
        }
        return seen;
    }

    @GameTest(maxTicks=200) public void jeanNaceYFases(GameTestHelper h) { jefe(h,0); }
    @GameTest(maxTicks=200) public void cenizaNaceYFases(GameTestHelper h) { jefe(h,1); }
    @GameTest(maxTicks=200) public void vacioNaceYFases(GameTestHelper h) { jefe(h,2); }
    private void jefe(GameTestHelper h,int i) {
        ServerLevel l=h.getLevel(); BlockPos o=new BlockPos(34000+i*384,150,34000); cargar(l,o,true);
        h.runAfterDelay(20,() -> {
            MazmorraPiece piece=construir(l,o,123,MazmorraStructure.Tema.values()[i]);
            ServerPlayer player=CieloGameTests.jugadorSupervivencia(h); BlockPos core=piece.nucleo();
            player.teleportTo(core.getX()+5,core.getY()+1,core.getZ());
            NucleoDelJefeBlockEntity.serverTick(l,core,l.getBlockState(core));
            h.runAfterDelay(5,() -> {
                try {
                    var bosses=l.getEntitiesOfClass(JefeExpedicion.class,new AABB(core).inflate(15));
                    h.assertTrue(bosses.size()==1,"El nucleo debe invocar exactamente un jefe");
                    JefeExpedicion boss=bosses.getFirst(); boss.setNoAi(true);
                    h.assertTrue(boss.getType()==tipos().get(i),"Tipo de jefe incorrecto");
                    boss.startSeenByPlayer(player);
                    h.assertTrue(boss.barra().getPlayers().contains(player),"Falta barra de jefe");
                    h.assertTrue(boss.fase()==1,"Fase inicial incorrecta");
                    boss.hurtServer(l,boss.damageSources().generic(),boss.getMaxHealth()*0.44F);
                    h.assertTrue(boss.fase()==2,"No cambia a fase 2 al sufrir daño");
                    // el enfriamiento de daño depende del tiempo de juego, congelado en gametests: segundo tramo por vida directa
                    boss.setHealth(boss.getMaxHealth()*0.25F);
                    h.assertTrue(boss.fase()==3,"No cambia a fase 3 con poca vida");
                    h.assertTrue(!boss.hurtServer(l,boss.damageSources().inWall(),10),"Vulnerable a asfixia");
                    h.assertTrue(!boss.hurtServer(l,boss.damageSources().fall(),10),"Vulnerable a caidas");
                    h.assertTrue(boss.isPersistenceRequired()&&!boss.removeWhenFarAway(100000),"El jefe desaparece");
                    NucleoDelJefeBlockEntity.serverTick(l,core,l.getBlockState(core));
                    h.assertTrue(!l.getBlockState(core).getValue(NucleoDelJefeBlock.ACTIVO),"El nucleo sigue activo");
                    h.assertTrue(l.getEntitiesOfClass(JefeExpedicion.class,new AABB(core).inflate(15)).size()==1,"Nucleo duplico jefe");
                } finally {
                    l.getEntitiesOfClass(Monster.class,new AABB(core).inflate(30)).forEach(Entity::discard);
                    l.getServer().getPlayerList().remove(player); cargar(l,o,false);
                }
                h.succeed();
            });
        });
    }
    private static List<EntityType<? extends JefeExpedicion>> tipos() {
        return List.of(CieloEntities.JEAN_BECK,CieloEntities.SENOR_DE_LA_CENIZA,CieloEntities.HERALDO_DEL_VACIO);
    }
    @GameTest public void escudoDeCenizaTieneVentana(GameTestHelper h) {
        var boss=CieloEntities.SENOR_DE_LA_CENIZA.create(h.getLevel(),EntitySpawnReason.COMMAND);
        boss.setHealth(boss.getMaxHealth()*0.3F);
        boss.tickCount=0;
        h.assertTrue(boss.escudoActivo()&&!boss.hurtServer(h.getLevel(),boss.damageSources().generic(),5),"El escudo no bloquea");
        boss.tickCount=75;
        h.assertTrue(!boss.escudoActivo()&&boss.hurtServer(h.getLevel(),boss.damageSources().generic(),5),"El escudo no ofrece ventana de ataque");
        boss.tickCount=120;
        h.assertTrue(boss.escudoActivo(),"El escudo no vuelve a activarse");
        h.succeed();
    }
    @GameTest public void recompensasEnTablas(GameTestHelper h) {
        ServerLevel l=h.getLevel();
        Item[][] expected={{CieloItems.CINTURON_JEAN_BECK,CieloItems.RACION_DE_COCINA},{CieloItems.HOJA_DE_CENIZA,Items.NETHERITE_SCRAP},{CieloItems.FRAGMENTO_DEL_VACIO,CieloItems.ORBE_DEL_VACIO}};
        for(int i=0;i<3;i++) {
            var boss=tipos().get(i).create(l,EntitySpawnReason.COMMAND);
            var key=boss.getLootTable().orElseThrow();
            var table=l.getServer().reloadableRegistries().getLootTable(key);
            var params=new LootParams.Builder(l).withParameter(LootContextParams.THIS_ENTITY,boss)
                .withParameter(LootContextParams.ORIGIN,Vec3.ZERO).withParameter(LootContextParams.DAMAGE_SOURCE,boss.damageSources().generic()).create(LootContextParamSets.ENTITY);
            var stacks=table.getRandomItems(params,42L);
            for(Item item:expected[i]) h.assertTrue(stacks.stream().anyMatch(s -> s.is(item)),"Falta recompensa "+item+" en "+key);
        }
        for(var tema:MazmorraStructure.Tema.values()) for(int tier=1;tier<=3;tier++) {
            var key=ResourceKey.create(Registries.LOOT_TABLE,CieloMod.id("chests/"+tema.getSerializedName()+"_"+tier));
            var table=l.getServer().reloadableRegistries().getLootTable(key);
            var params=new LootParams.Builder(l).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).create(LootContextParamSets.CHEST);
            h.assertTrue(!table.getRandomItems(params,42L).isEmpty(),"Cofre sin botin: "+key);
        }
        h.succeed();
    }
}
