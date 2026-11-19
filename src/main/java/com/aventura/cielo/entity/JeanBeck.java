package com.aventura.cielo.entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
public class JeanBeck extends JefeExpedicion {
    public JeanBeck(EntityType<? extends JeanBeck> type, Level level) { super(type,level); }
    @Override protected int estilo() { return 0; }
}
