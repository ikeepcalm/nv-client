package dev.ua.ikeepcalm.nexusveil.client.render;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.Data;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ShipMesh {

    // Owned copies: a MeshData lives in the compiler's reused builder and goes invalid the moment it is reset.
    @Data
    public static class Layer {
        private final ByteBuffer vertices;
        private final VertexFormat format;
        private final int indexCount;
    }

    @Data
    public static class Section {
        private final int localX;
        private final int localY;
        private final int localZ;
        private final Map<ChunkSectionLayer, Layer> layers;
        private final int quads;
    }

    private final List<Section> sections = new ArrayList<>();
    private int quads;
    private ShipQuads cpu;

    public ShipQuads cpu() {
        return this.cpu;
    }

    public void cpu(ShipQuads cpu) {
        this.cpu = cpu;
    }

    public void add(int localX, int localY, int localZ, Map<ChunkSectionLayer, MeshData> rendered) {
        Map<ChunkSectionLayer, Layer> layers = new EnumMap<>(ChunkSectionLayer.class);
        int count = 0;
        for (Map.Entry<ChunkSectionLayer, MeshData> entry : rendered.entrySet()) {
            MeshData.DrawState state = entry.getValue().drawState();
            ByteBuffer source = entry.getValue().vertexBuffer();
            if (state.indexCount() == 0 || !source.hasRemaining()) {
                continue;
            }
            ByteBuffer copy = ByteBuffer.allocateDirect(source.remaining()).order(ByteOrder.nativeOrder());
            copy.put(source.duplicate()).flip();
            layers.put(entry.getKey(), new Layer(copy, state.format(), state.indexCount()));
            count += state.indexCount() / 6;
        }
        if (layers.isEmpty()) {
            return;
        }
        this.sections.add(new Section(localX, localY, localZ, layers, count));
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
}
