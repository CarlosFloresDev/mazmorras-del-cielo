package com.aventura.cielo.entity;

import com.aventura.cielo.CieloMod;
import com.aventura.cielo.world.MazmorraStructure.Tema;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;

/**
 * API para estructuras, sin dependencia obligatoria de Friends & Foes:
 * tipos(Tema) devuelve candidatos inmutables; no instancia ni modifica registros.
 * crear(level,pos,tema) elige, finaliza y mejora un candidato SIN anadirlo al mundo;
 * el llamador comprueba colisiones y llama addFreshEntity. Para spawners u otros
 * tipos, llamar aplicarElite(mob) DESPUES de finalizeSpawn/cargar NBT.
 * aplicarElite es idempotente y persistente: +50% PV, +4 dano, +6 armadura,
 * +10% velocidad, etiqueta cielo:elite y persistencia. Atributos ausentes se omiten
 * (no se inventa melee para shulkers); PV/armadura tambien mejoran tiradores.
 */
public final class EnemigosExternos {
    public static final String ETIQUETA="cielo:elite";
    private EnemigosExternos() { }
    public static List<EntityType<?>> tipos(Tema tema) {
        List<EntityType<?>> result=new ArrayList<>(switch(tema) {
            case FORTIN -> List.of(EntityTypes.VINDICATOR,EntityTypes.PILLAGER,EntityTypes.EVOKER);
            case CENIZA -> List.of(EntityTypes.WITHER_SKELETON,EntityTypes.BLAZE,EntityTypes.PIGLIN_BRUTE);
            case VACIO -> List.of(EntityTypes.ENDERMAN,EntityTypes.SHULKER,EntityTypes.ENDERMITE);
        });
        if(FabricLoader.getInstance().isModLoaded("friendsandfoes")) {
            String[] ids=switch(tema) {
                case FORTIN -> new String[]{"iceologer","illusioner"};
                case CENIZA -> new String[]{"wildfire"};
                case VACIO -> new String[]{"illusioner"};
            };
            for(String id:ids) BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.fromNamespaceAndPath("friendsandfoes",id))
                .ifPresent(result::add);
        }
        return List.copyOf(result);
    }
    public static Mob crear(ServerLevel level,BlockPos pos,Tema tema) {
        var types=tipos(tema);
        var entity=types.get(level.getRandom().nextInt(types.size())).create(level,EntitySpawnReason.STRUCTURE);
        if(!(entity instanceof Mob mob)) return null;
        mob.snapTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);
        mob.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),EntitySpawnReason.STRUCTURE,null);
        aplicarElite(mob);
        return mob;
    }
    public static void aplicarElite(Mob mob) {
        if(mob.entityTags().contains(ETIQUETA)) return;
        float fraction=mob.getHealth()/mob.getMaxHealth();
        mejorar(mob,Attributes.MAX_HEALTH,"vida",0.5,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        mejorar(mob,Attributes.ATTACK_DAMAGE,"dano",4,AttributeModifier.Operation.ADD_VALUE);
        mejorar(mob,Attributes.ARMOR,"armadura",6,AttributeModifier.Operation.ADD_VALUE);
        mejorar(mob,Attributes.MOVEMENT_SPEED,"velocidad",0.1,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        mob.setHealth(mob.getMaxHealth()*fraction);
        mob.setPersistenceRequired(); mob.addTag(ETIQUETA);
    }
    private static void mejorar(Mob mob,Holder<Attribute> attribute,String name,double amount,AttributeModifier.Operation op) {
        var instance=mob.getAttribute(attribute);
        var id=CieloMod.id("elite_"+name);
        if(instance!=null&&!instance.hasModifier(id)) instance.addPermanentModifier(new AttributeModifier(id,amount,op));
    }
}
