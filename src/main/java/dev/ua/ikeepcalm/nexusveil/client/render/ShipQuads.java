package dev.ua.ikeepcalm.nexusveil.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;
import java.util.Map;

// A CPU copy of the hull mesh in hull-local coordinates, so it can be fed through a vertex consumer that Iris captures.
public class ShipQuads {

    private static class Layer {
        private final FloatArrayList positions = new FloatArrayList();
        private final FloatArrayList uvs = new FloatArrayList();
        private final FloatArrayList normals = new FloatArrayList();
        private final IntArrayList colours = new IntArrayList();
        private final IntArrayList lights = new IntArrayList();
    }

    private final Map<ChunkSectionLayer, Layer> layers = new EnumMap<>(ChunkSectionLayer.class);

    public static ShipQuads of(ShipMesh mesh) {
        ShipQuads quads = new ShipQuads();
        for (ShipMesh.Section section : mesh.sections()) {
            section.getLayers().forEach((layer, data) -> quads.read(section, layer, data));
        }
        return quads;
    }

    public Iterable<ChunkSectionLayer> layers() {
        return this.layers.keySet();
    }

    private void read(ShipMesh.Section section, ChunkSectionLayer layer, ShipMesh.Layer data) {
        VertexFormat format = data.getFormat();
        int stride = format.getVertexSize();
        int position = format.getElement("Position").offset();
        int colour = format.getElement("Color").offset();
        int uv = format.getElement("UV0").offset();
        int light = format.getElement("UV2").offset();
        ByteBuffer buffer = data.getVertices().duplicate().order(ByteOrder.nativeOrder());
        int base = buffer.position();
        int vertices = buffer.remaining() / stride / 4 * 4;
        if (vertices == 0) {
            return;
        }
        Layer out = this.layers.computeIfAbsent(layer, ignored -> new Layer());
        for (int i = 0; i < vertices; i++) {
            int at = base + i * stride;
            out.positions.add(buffer.getFloat(at + position) + section.getLocalX());
            out.positions.add(buffer.getFloat(at + position + 4) + section.getLocalY());
            out.positions.add(buffer.getFloat(at + position + 8) + section.getLocalZ());
            out.colours.add(buffer.getInt(at + colour));
            out.uvs.add(buffer.getFloat(at + uv));
            out.uvs.add(buffer.getFloat(at + uv + 4));
            out.lights.add(buffer.getInt(at + light));
            if (i % 4 == 3) {
                normal(out, i - 3);
            }
        }
    }

    private static void normal(Layer layer, int first) {
        float[] p = layer.positions.elements();
        int a = first * 3;
        float ux = p[a + 3] - p[a], uy = p[a + 4] - p[a + 1], uz = p[a + 5] - p[a + 2];
        float vx = p[a + 6] - p[a], vy = p[a + 7] - p[a + 1], vz = p[a + 8] - p[a + 2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 1.0e-6f) {
            nx = 0f;
            ny = 1f;
            nz = 0f;
            length = 1f;
        }
        layer.normals.add(nx / length);
        layer.normals.add(ny / length);
        layer.normals.add(nz / length);
    }

    // The mesh stores RGBA bytes in memory order, which reads back little-endian as ABGR.
    public void emit(ChunkSectionLayer which, PoseStack.Pose pose, VertexConsumer consumer) {
        Layer layer = this.layers.get(which);
        if (layer == null) {
            return;
        }
        float[] p = layer.positions.elements();
        float[] t = layer.uvs.elements();
        float[] n = layer.normals.elements();
        int[] c = layer.colours.elements();
        int[] l = layer.lights.elements();
        int vertices = layer.colours.size();
        for (int i = 0; i < vertices; i++) {
            int quad = i / 4 * 3;
            int rgba = c[i];
            consumer.addVertex(pose, p[i * 3], p[i * 3 + 1], p[i * 3 + 2])
                    .setColor(rgba & 0xFF, (rgba >> 8) & 0xFF, (rgba >> 16) & 0xFF, (rgba >>> 24) & 0xFF)
                    .setUv(t[i * 2], t[i * 2 + 1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(l[i])
                    .setNormal(pose, n[quad], n[quad + 1], n[quad + 2]);
        }
    }
}
