package dev.ua.ikeepcalm.nexusveil.client.render;

import com.mojang.blaze3d.vertex.MeshData;
import lombok.Data;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ShipMesh implements AutoCloseable {

    @Data
    public static class Section implements AutoCloseable {
        private final int localX;
        private final int localY;
        private final int localZ;
        private final Map<ChunkSectionLayer, MeshData> layers;
        private final int quads;

        @Override
        public void close() {
            this.layers.values().forEach(MeshData::close);
        }
    }

    private final List<Section> sections = new ArrayList<>();
    private int quads;

    public void add(int localX, int localY, int localZ, Map<ChunkSectionLayer, MeshData> layers) {
        if (layers.isEmpty()) {
            return;
        }
        int count = 0;
        for (MeshData mesh : layers.values()) {
            count += mesh.drawState().indexCount() / 6;
        }
        this.sections.add(new Section(localX, localY, localZ, new EnumMap<>(layers), count));
        this.quads += count;
    }

    public List<Section> sections() {
        return this.sections;
    }

    public int sectionCount() {
        return this.sections.size();
    }

    public int quads() {
        return this.quads;
    }

    @Override
    public void close() {
        this.sections.forEach(Section::close);
        this.sections.clear();
        this.quads = 0;
    }
}
