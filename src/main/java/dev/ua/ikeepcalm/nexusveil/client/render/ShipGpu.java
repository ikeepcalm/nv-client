package dev.ua.ikeepcalm.nexusveil.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ua.ikeepcalm.nexusveil.client.NvLog;
import lombok.Data;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ShipGpu implements AutoCloseable {

    @Data
    public static class Piece implements AutoCloseable {
        private final int localX;
        private final int localY;
        private final int localZ;
        private final GpuBuffer vertices;
        private final int indexCount;

        @Override
        public void close() {
            this.vertices.close();
        }
    }

    private final Map<ChunkSectionLayer, List<Piece>> byLayer = new EnumMap<>(ChunkSectionLayer.class);
    private int maxIndexCount;
    private long bytes;

    public static ShipGpu upload(ShipMesh mesh, String name) {
        ShipGpu gpu = new ShipGpu();
        for (ShipMesh.Section section : mesh.sections()) {
            section.getLayers().forEach((layer, data) -> gpu.add(section, layer, data, name));
        }
        return gpu;
    }

    private void add(ShipMesh.Section section, ChunkSectionLayer layer, ShipMesh.Layer data, String name) {
        int size = data.getVertices().remaining();
        GpuBuffer vertices = RenderSystem.getDevice().createBuffer(
                () -> name + "/" + layer.label(), GpuBuffer.USAGE_VERTEX, data.getVertices().duplicate());

        this.byLayer.computeIfAbsent(layer, ignored -> new ArrayList<>())
                .add(new Piece(section.getLocalX(), section.getLocalY(), section.getLocalZ(), vertices, data.getIndexCount()));
        this.maxIndexCount = Math.max(this.maxIndexCount, data.getIndexCount());
        this.bytes += size;
    }

    public List<Piece> pieces(ChunkSectionLayer layer) {
        return this.byLayer.getOrDefault(layer, List.of());
    }

    public boolean isEmpty() {
        return this.byLayer.isEmpty();
    }

    public int maxIndexCount() {
        return this.maxIndexCount;
    }

    public int pieceCount() {
        int count = 0;
        for (List<Piece> pieces : this.byLayer.values()) {
            count += pieces.size();
        }
        return count;
    }

    public long bytes() {
        return this.bytes;
    }

    @Override
    public void close() {
        this.byLayer.values().forEach(pieces -> pieces.forEach(Piece::close));
        this.byLayer.clear();
        this.maxIndexCount = 0;
        this.bytes = 0;
    }

    public static void closeQuietly(ShipGpu gpu) {
        if (gpu == null) {
            return;
        }
        try {
            gpu.close();
        } catch (RuntimeException e) {
            NvLog.LOG.warn("Could not release hull buffers: {}", e.getMessage());
        }
    }
}
