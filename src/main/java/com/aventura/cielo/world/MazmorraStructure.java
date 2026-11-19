package com.aventura.cielo.world;

import com.aventura.cielo.registry.CieloWorld;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Grandes expediciones de cinco secciones, con semilla guardada en su pieza. */
public class MazmorraStructure extends Structure {
    public enum Tema implements StringRepresentable {
        FORTIN("fortin_jean_beck"), CENIZA("bastion_ceniza"), VACIO("santuario_vacio");
        private final String id;
        Tema(String id) { this.id = id; }
        public String getSerializedName() { return id; }
    }
    public static final MapCodec<MazmorraStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        settingsCodec(i), StringRepresentable.fromEnum(Tema::values).fieldOf("tema").forGetter(s -> s.tema)
    ).apply(i, MazmorraStructure::new));
    private final Tema tema;
    public MazmorraStructure(StructureSettings settings, Tema tema) { super(settings); this.tema = tema; }
    public static boolean permitido(Tema tema, int x, int z) {
        return (long)x * x + (long)z * z >= (tema == Tema.VACIO ? 1600L * 1600 : 700L * 700);
    }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext c) {
        int x = c.chunkPos().getMiddleBlockX(), z = c.chunkPos().getMiddleBlockZ();
        if (!permitido(tema, x, z)) return Optional.empty();
        int y;
        if (tema == Tema.CENIZA) y = 40 + c.random().nextInt(12);
        else {
            int ground = c.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                c.heightAccessor(), c.randomState());
            // Fortin: 24 bloques sobre la superficie; Ciudadela: 40. End: sobre islas exteriores.
            y = tema == Tema.FORTIN ? Math.max(96, ground + 24) : Math.max(82, ground + 18);
            if(y+18>=c.heightAccessor().getMaxY()) return Optional.empty();
        }
        BlockPos o = new BlockPos(x, y, z);
        long seed = c.random().nextLong();
        return Optional.of(new GenerationStub(o, b -> b.addPiece(new MazmorraPiece(o, seed, tema))));
    }
    @Override public StructureType<?> type() { return CieloWorld.MAZMORRA; }
}
