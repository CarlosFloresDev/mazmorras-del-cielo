package com.aventura.cielo.entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
public class HeraldoDelVacio extends JefeExpedicion {
    public HeraldoDelVacio(EntityType<? extends HeraldoDelVacio> type, Level level) { super(type,level); }
    @Override protected int estilo() { return 2; }
}
