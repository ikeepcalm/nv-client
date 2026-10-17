package dev.ua.ikeepcalm.nexusveil.client.render;

import com.mojang.blaze3d.vertex.VertexSorting;
import dev.ua.ikeepcalm.nexusveil.core.math.Vec3d;
import dev.ua.ikeepcalm.nexusveil.core.voxel.ShipLightField;
import dev.ua.ikeepcalm.nexusveil.core.voxel.VoxelModel;
import lombok.Data;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShipMesher {

    private static final int SECTION = 16;

    @Data
    private static class Task {
        private final int localX;
        private final int localY;
        private final int localZ;
        private final SectionPos at;
        private final RenderSectionRegion region;
    }

    @Data
    public static class Job {
        private final List<Task> tasks;
    }

    private final SectionBufferBuilderPack buffers = new SectionBufferBuilderPack();
    private SectionCompiler compiler;
    private LevelChunk air;

    public Job prepare(VoxelModel model, Vec3d worldOrigin) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) {
            return null;
        }
        if (this.compiler == null) {
            this.compiler = new SectionCompiler(
                    client.options.ambientOcclusion().get(),
                    false,
                    client.getModelManager().getBlockStateModelSet(),
                    client.getModelManager().getFluidStateModelSet(),
                    client.getBlockColors());
        }
        BlockState[] states = ShipBlockStates.resolve(model.palette());

        // Anchored at the hull's real position so the sky lights it, and snapped down to a section
        // boundary: an unaligned anchor offsets the whole drawn hull by the remainder.
        int anchorX = align((int) Math.floor(worldOrigin.getX()));
        int anchorY = align(clampY(level, (int) Math.floor(worldOrigin.getY()), model.sizeY()));
        int anchorZ = align((int) Math.floor(worldOrigin.getZ()));
        if (anchorY < level.getMinY()) {
            anchorY += SECTION;
        }

        Map<Long, LevelChunk> chunks = load(level, model, states, anchorX, anchorY, anchorZ);
        ShipLightField light = lightField(model, states);
        int[] anchor = {anchorX, anchorY, anchorZ};
        // One SectionCopy per section, not per neighbourhood: each duplicates a whole PalettedContainer.
        Map<Long, SectionCopy> copies = new HashMap<>();

        List<Task> tasks = new ArrayList<>();
        for (int sz = 0; sz < ceilDiv(model.sizeZ()); sz++) {
            for (int sy = 0; sy < ceilDiv(model.sizeY()); sy++) {
                for (int sx = 0; sx < ceilDiv(model.sizeX()); sx++) {
                    SectionPos at = SectionPos.of(
                            (anchorX >> 4) + sx, (anchorY >> 4) + sy, (anchorZ >> 4) + sz);
                    tasks.add(new Task(sx * SECTION, sy * SECTION, sz * SECTION, at,
                            region(level, chunks, copies, at, light, anchor, model)));
                }
            }
        }
        return new Job(tasks);
    }

    public ShipMesh compile(Job job) {
        ShipMesh mesh = new ShipMesh();
        for (Task task : job.getTasks()) {
            SectionCompiler.Results results = this.compiler.compile(
                    task.getAt(), task.getRegion(), VertexSorting.DISTANCE_TO_ORIGIN, this.buffers);
            mesh.add(task.getLocalX(), task.getLocalY(), task.getLocalZ(), results.renderedLayers);
        }
        return mesh;
    }

    // Neighbours outside the hull must be real air sections: an absent one stops the outer planking faces drawing.
    private RenderSectionRegion region(ClientLevel level, Map<Long, LevelChunk> chunks,
                                       Map<Long, SectionCopy> copies, SectionPos centre,
                                       ShipLightField light, int[] anchor, VoxelModel model) {
        SectionCopy[] sections = new SectionCopy[RenderSectionRegion.SIZE
                * RenderSectionRegion.SIZE * RenderSectionRegion.SIZE];
        int minX = centre.x() - RenderSectionRegion.RADIUS;
        int minY = centre.y() - RenderSectionRegion.RADIUS;
        int minZ = centre.z() - RenderSectionRegion.RADIUS;

        for (int z = 0; z < RenderSectionRegion.SIZE; z++) {
            for (int y = 0; y < RenderSectionRegion.SIZE; y++) {
                for (int x = 0; x < RenderSectionRegion.SIZE; x++) {
                    int secX = minX + x;
                    int secY = minY + y;
                    int secZ = minZ + z;
                    sections[RenderSectionRegion.index(minX, minY, minZ, secX, secY, secZ)] =
                            copies.computeIfAbsent(key(secX, secY, secZ),
                                    ignored -> copy(level, chunks, secX, secY, secZ));
                }
            }
        }
        return new ShipLitRegion(level, minX, minY, minZ, sections, light, anchor[0], anchor[1], anchor[2],
                model.sizeX(), model.sizeY(), model.sizeZ());
    }

    // Same rule as the server's ShipLighting, so both paths light a hull alike: stair and slab roofs count as opaque or daylight leaks into cabins.
    private static ShipLightField lightField(VoxelModel model, BlockState[] states) {
        int[] emission = new int[states.length];
        int[] opacity = new int[states.length];
        for (int id = 1; id < states.length; id++) {
            emission[id] = states[id].getLightEmission();
            opacity[id] = states[id].getBlock() instanceof SlabBlock || states[id].getBlock() instanceof StairBlock
                    ? 15 : states[id].getLightDampening();
        }
        return new ShipLightField(model, emission, opacity);
    }

    private SectionCopy copy(ClientLevel level, Map<Long, LevelChunk> chunks, int secX, int secY, int secZ) {
        LevelChunk chunk = chunks.get(ChunkPos.pack(secX, secZ));
        if (chunk == null) {
            if (this.air == null) {
                this.air = new LevelChunk(level, ChunkPos.ZERO);
            }
            chunk = this.air;
        }
        return new SectionCopy(chunk, sectionIndex(level, chunk, secY));
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }

    private static Map<Long, LevelChunk> load(ClientLevel level, VoxelModel model, BlockState[] states,
                                              int anchorX, int anchorY, int anchorZ) {
        Map<Long, LevelChunk> chunks = new HashMap<>();
        for (int z = 0; z < model.sizeZ(); z++) {
            for (int y = 0; y < model.sizeY(); y++) {
                for (int x = 0; x < model.sizeX(); x++) {
                    int id = model.paletteIndex(x, y, z);
                    if (id == 0) {
                        continue;
                    }
                    int worldX = anchorX + x;
                    int worldY = anchorY + y;
                    int worldZ = anchorZ + z;
                    LevelChunk chunk = chunks.computeIfAbsent(
                            ChunkPos.pack(worldX >> 4, worldZ >> 4),
                            key -> new LevelChunk(level, ChunkPos.unpack(key)));
                    LevelChunkSection section = chunk.getSection(
                            sectionIndex(level, chunk, worldY >> 4));
                    section.setBlockState(worldX & 15, worldY & 15, worldZ & 15, states[id], false);
                }
            }
        }
        return chunks;
    }

    private static int sectionIndex(ClientLevel level, LevelChunk chunk, int sectionY) {
        return Math.clamp(sectionY - level.getMinSectionY(), 0, chunk.getSections().length - 1);
    }

    // Headroom is the hull's own height only: Math.clamp throws when min passes max.
    private static int clampY(ClientLevel level, int y, int modelHeight) {
        int min = level.getMinY();
        int max = level.getMaxY() - modelHeight;
        return max <= min ? min : Math.clamp(y, min, max);
    }

    private static int align(int value) {
        return (value >> 4) << 4;
    }

    private static int ceilDiv(int size) {
        return (size + SECTION - 1) / SECTION;
    }
}
