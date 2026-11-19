package com.aventura.cielo.item;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
public class HojaDeCenizaItem extends Item {
    public HojaDeCenizaItem(Properties p) { super(p); }
    @Override public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack,target,attacker);
        if(!attacker.level().isClientSide()) {
            target.igniteForSeconds(5);
            attacker.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,80,0));
        }
    }
}
