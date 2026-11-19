package com.aventura.cielo.world;

import com.aventura.cielo.CieloMod;
import com.aventura.cielo.block.NucleoDelJefeBlock;
import com.aventura.cielo.registry.*;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** 129 x 33 bloques: atrio, dos alas, sala ritual y arena. Cada ala custodia la llave siguiente.
 * El plano se reconstruye desde la semilla, nunca desde el random del chunk. */
public class MazmorraPiece extends StructurePiece {
    public static final int LARGO = 112, RADIO = 16;
    private final BlockPos origen;
    private final long semilla;
    private final MazmorraStructure.Tema tema;
    public MazmorraPiece(BlockPos o, long seed, MazmorraStructure.Tema tema) {
        super(CieloWorld.PIEZA_MAZMORRA, 0, new BoundingBox(o.getX()-16, -64, o.getZ()-16, o.getX()+112, o.getY()+18, o.getZ()+16));
        this.origen = o; this.semilla = seed; this.tema = tema;
    }
    public MazmorraPiece(CompoundTag t) {
        super(CieloWorld.PIEZA_MAZMORRA, t);
        origen = new BlockPos(t.getIntOr("OX",0),t.getIntOr("OY",100),t.getIntOr("OZ",0));
        semilla = t.getLongOr("Semilla",0); tema = MazmorraStructure.Tema.valueOf(t.getStringOr("Tema","FORTIN"));
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c, CompoundTag t) {
        t.putInt("OX",origen.getX()); t.putInt("OY",origen.getY()); t.putInt("OZ",origen.getZ());
        t.putLong("Semilla",semilla); t.putString("Tema",tema.name());
    }
    public BlockPos entrada() { return origen.offset(-14,1,0); }
    public BlockPos nucleo() { return origen.offset(96,0,0); }
    private int puertaX(int i) { return 12+i*24+new Random(semilla^(i+1)*104729L).nextInt(5)-2; }
    private int puertaZ(int i) { return (new Random(semilla^(i+1)*15485863L).nextInt(3)-1)*4; }
    public BlockPos sello(int i) { return origen.offset(puertaX(i),1,puertaZ(i)); }
    public BlockPos llave(int i) { return origen.offset(i*24-7,1, lado(i)*8); }
    private int lado(int i) { return new Random(semilla + i*7919L).nextBoolean() ? 1 : -1; }
    public NucleoDelJefeBlock.Jefe jefe() {
        return switch(tema) { case FORTIN -> NucleoDelJefeBlock.Jefe.JEAN; case CENIZA -> NucleoDelJefeBlock.Jefe.CENIZA; case VACIO -> NucleoDelJefeBlock.Jefe.VACIO; };
    }
    @Override public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
        RandomSource random, BoundingBox chunk, ChunkPos cp, BlockPos pivot) {
        Constructor c = new Constructor(level,chunk);
        BlockPos acceso=origen.offset(-16,0,0);
        int terrenoEntrada=chunk.isInside(acceso)?level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,acceso.getX(),acceso.getZ()):0;
        Block suelo = switch(tema) { case FORTIN -> Blocks.DARK_OAK_PLANKS; case CENIZA -> Blocks.POLISHED_BLACKSTONE_BRICKS; case VACIO -> Blocks.END_STONE_BRICKS; };
        Block muro = switch(tema) { case FORTIN -> Blocks.STONE_BRICKS; case CENIZA -> Blocks.BASALT; case VACIO -> Blocks.PURPUR_BLOCK; };
        Block acento = switch(tema) { case FORTIN -> Blocks.STRIPPED_SPRUCE_LOG; case CENIZA -> Blocks.CHISELED_POLISHED_BLACKSTONE; case VACIO -> Blocks.OBSIDIAN; };
        // Envolvente continua impide entrar por los laterales, el techo o el subsuelo.
        c.fill(-16,-3,-16,112,14,16,muro);
        c.fill(-15,1,-15,111,13,15,Blocks.AIR);
        c.fill(-15,0,-15,111,0,15,suelo);
        for(int x=-12;x<=108;x+=12) {
            c.fill(x,1,-16,x,14,-16,acento);
            c.fill(x,1,16,x,14,16,acento);
            // Ventanas cerradas: luz y vistas, sin accesos laterales que eviten los sellos.
            c.fill(x+2,5,-16,Math.min(x+5,111),8,-16,Blocks.TINTED_GLASS);
            c.fill(x+2,5,16,Math.min(x+5,111),8,16,Blocks.TINTED_GLASS);
        }
        for(int x:new int[]{-13,109}) for(int z:new int[]{-13,13}) {
            c.fill(x-2,15,z-2,x+2,16,z+2,acento);
            c.fill(x-1,17,z-1,x+1,18,z+1,muro);
            c.set(x,18,z,tema==MazmorraStructure.Tema.VACIO?Blocks.END_ROD:Blocks.SHROOMLIGHT);
        }
        for(int i=0;i<5;i++) {
            int x=i*24;
            for(int dx : new int[]{-10,10}) for(int z : new int[]{-12,12}) {
                c.fill(x+dx,1,z,x+dx,12,z,acento);
                c.set(x+dx,10,z-Integer.signum(z),Blocks.SHROOMLIGHT);
            }
            // Contrafuertes, almenas y tragaluces con cristal: sin huecos explotables.
            c.fill(x-10,15,-16,x+10,15,-14,acento);
            c.fill(x-10,15,14,x+10,15,16,acento);
            c.fill(x-7,14,-6,x+7,14,6,Blocks.TINTED_GLASS);
            c.chest(x+7,1,-lado(i)*8,tema.getSerializedName()+"_"+(i<2?1:i<4?2:3),false);
            if(i<4) {
                c.chest(x-7,1,lado(i)*8,"",true);
                c.fill(puertaX(i),1,-15,puertaX(i),13,15,muro);
                c.fill(puertaX(i),1,puertaZ(i)-1,puertaX(i),3,puertaZ(i)+1,CieloBlocks.SELLO_CELESTIAL);
                c.spawner(x+5,1,9);
                c.spawner(x-5,1,-9);
            }
            decorar(c,x,i,lado(i),suelo);
            // Mosaico distinto por semilla y sala, sin alterar la transitabilidad.
            Random detalles=new Random(semilla+i*65537L);
            for(int n=0;n<18;n++) c.set(x+detalles.nextInt(17)-8,0,detalles.nextInt(25)-12,acento);
        }
        // La entrada solo comunica con la primera seccion.
        c.fill(-16,1,-1,-16,3,1,Blocks.AIR);
        c.fill(-16,0,-2,-14,0,2,suelo);
        c.set(96,0,0,CieloBlocks.NUCLEO_DEL_JEFE.defaultBlockState().setValue(NucleoDelJefeBlock.JEFE,jefe()));
        // Acceso desde terreno/isla/lago: cuerda situada en el extremo de la entrada.
        BlockPos p=origen.offset(-16,0,0);
        if(chunk.isInside(p)) {
            for(int y=origen.getY();y>=Math.max(level.getMinY()+1,Math.min(terrenoEntrada,origen.getY()-4));y--)
                c.set(-16,y-origen.getY(),0,CieloBlocks.ESCALERA_DE_CUERDA);
        }
    }
    private void decorar(Constructor c,int x,int sala,int lado,Block suelo) {
        if(tema==MazmorraStructure.Tema.FORTIN) {
            // Mesas, barriles de taberna, despensa y fogones; pasillo central libre.
            for(int z:new int[]{-5,5}) for(int dx=-6;dx<=6;dx+=6) {
                c.set(x+dx,1,z,Blocks.OAK_FENCE); c.set(x+dx,2,z,Blocks.OAK_PRESSURE_PLATE);
                c.set(x+dx,1,z+Integer.signum(z)*2,Blocks.SPRUCE_STAIRS);
            }
            for(int dz=-10;dz<=10;dz+=4) c.set(x-9,1,dz,sala==2?Blocks.SMOKER:Blocks.BARREL);
            c.fill(x-3,1,12,x+3,1,12,Blocks.HAY_BLOCK);
        } else if(tema==MazmorraStructure.Tema.CENIZA) {
            for(int z:new int[]{-6,6}) {
                c.fill(x-8,0,z,x+8,0,z,Blocks.MAGMA_BLOCK);
                c.fill(x-8,1,z,x-8,4,z,Blocks.IRON_CHAIN);
                c.set(x-8,5,z,Blocks.SOUL_LANTERN);
            }
            c.fill(x-3,1,12,x+3,2,12,Blocks.BLACKSTONE);
            c.set(x,3,12,Blocks.SOUL_FIRE);
        } else {
            for(int dx:new int[]{-6,6}) for(int z:new int[]{-5,5}) {
                c.set(x+dx,0,z,Blocks.END_STONE);
                c.set(x+dx,1,z,Blocks.CHORUS_PLANT);
                c.set(x+dx,2,z,Blocks.CHORUS_FLOWER);
                c.set(x+dx+1,1,z,Blocks.END_ROD);
            }
            c.fill(x-3,1,12,x+3,2,12,Blocks.PURPUR_STAIRS);
        }
    }
    private final class Constructor {
        final WorldGenLevel level; final BoundingBox chunk;
        Constructor(WorldGenLevel l,BoundingBox b) { level=l; chunk=b; }
        void set(int x,int y,int z,Block b) { set(x,y,z,b.defaultBlockState()); }
        void set(int x,int y,int z,BlockState s) {
            BlockPos p=origen.offset(x,y,z);
            if(chunk.isInside(p)) level.setBlock(p,s,2);
        }
        void fill(int x1,int y1,int z1,int x2,int y2,int z2,Block b) {
            // Limitar los bucles tambien, no solo las escrituras.
            for(int x=Math.max(x1,chunk.minX()-origen.getX());x<=Math.min(x2,chunk.maxX()-origen.getX());x++)
                for(int z=Math.max(z1,chunk.minZ()-origen.getZ());z<=Math.min(z2,chunk.maxZ()-origen.getZ());z++)
                    for(int y=y1;y<=y2;y++) set(x,y,z,b);
        }
        void chest(int x,int y,int z,String loot,boolean key) {
            BlockPos p=origen.offset(x,y,z); if(!chunk.isInside(p)) return;
            set(x,y,z,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.SOUTH));
            if(level.getBlockEntity(p) instanceof ChestBlockEntity chest) {
                if(key) chest.setItem(13,new ItemStack(CieloItems.LLAVE_CELESTIAL));
                else chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,CieloMod.id("chests/"+loot)),semilla^p.asLong());
            }
        }
        void spawner(int x,int y,int z) {
            BlockPos p=origen.offset(x,y,z); if(!chunk.isInside(p)) return;
            set(x,y,z,Blocks.SPAWNER);
            if(level.getBlockEntity(p) instanceof SpawnerBlockEntity spawner) spawner.setEntityId(switch(tema) {
                case FORTIN -> CieloEntities.AYUDANTE_COCINA; case CENIZA -> CieloEntities.BOMBARDERO_CENIZA; case VACIO -> CieloEntities.FUEGO_FATUO;
            },RandomSource.create(semilla^p.asLong()));
        }
    }
}
