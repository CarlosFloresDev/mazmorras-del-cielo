package com.aventura.cielo.entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
public class SenorDeLaCeniza extends JefeExpedicion {
    public SenorDeLaCeniza(EntityType<? extends SenorDeLaCeniza> type, Level level) { super(type,level); }
    @Override protected int estilo() { return 1; }
}
