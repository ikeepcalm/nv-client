package dev.ua.ikeepcalm.nexusveil.client.render;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.ua.ikeepcalm.nexusveil.client.NvLog;
import dev.ua.ikeepcalm.nexusveil.client.net.NvNetworking;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientFleet;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientShip;
import dev.ua.ikeepcalm.nexusveil.core.math.Vec3d;
import dev.ua.ikeepcalm.nexusveil.core.physics.ShipPose;
import dev.ua.ikeepcalm.nexusveil.core.render.ShipDrawTransform;
import lombok.Data;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public class ShipDrawPass {

    private static boolean broken;
    private static int piecesLastFrame;
    private static int batchesLastFrame;

    public static boolean usable() {
        return !broken;
    }

    public static int piecesLastFrame() {
        return piecesLastFrame;
    }

    public static int batchesLastFrame() {
        return batchesLastFrame;
    }

    public static void draw(CameraRenderState camera, ChunkSectionLayerGroup group) {
        if (broken || ClientFleet.size() == 0) {
            return;
        }
        try {
            if (group == ChunkSectionLayerGroup.OPAQUE) {
                piecesLastFrame = 0;
                batchesLastFrame = 0;
            }
            render(camera, group);
        } catch (RuntimeException | LinkageError e) {
            // Standing down stops a stack trace per frame and makes the server put its own rig back.
            broken = true;
            NvLog.LOG.error("Hull rendering failed; handing the ships back to the server", e);
            NvNetworking.stand("renderer threw: " + e);
        }
    }

    private static void render(CameraRenderState camera, ChunkSectionLayerGroup group) {
        List<Section> sections = collect(camera, group);
        if (sections.isEmpty()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        GpuTextureView atlas = client.getTextureManager()
                .getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();

        DynamicUniforms.ChunkSectionInfo[] infos = new DynamicUniforms.ChunkSectionInfo[sections.size()];
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            infos[i] = new DynamicUniforms.ChunkSectionInfo(section.getModelView(),
                    camera.blockPos.getX(), camera.blockPos.getY(), camera.blockPos.getZ(),
                    1.0f, atlas.getWidth(0), atlas.getHeight(0));
        }
        GpuBufferSlice[] slices = RenderSystem.getDynamicUniforms().writeChunkSections(infos);

        int maxIndices = 0;
        for (Section section : sections) {
            maxIndices = Math.max(maxIndices, section.getPiece().getIndexCount());
        }
        RenderSystem.AutoStorageIndexBuffer shared = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer indices = shared.getBuffer(maxIndices);
        IndexType indexType = shared.type();

        var target = group.outputTarget();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "nexusveil/hulls/" + group.name(),
                target.getColorTextureView(), Optional.empty(),
                target.getDepthTextureView(), OptionalDouble.empty())) {

            RenderSystem.bindDefaultUniforms(pass);
            pass.bindTexture("Sampler0", atlas,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.bindTexture("Sampler2", client.gameRenderer.lightmap(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));

            for (ChunkSectionLayer layer : group.layers()) {
                List<RenderPass.Draw<GpuBufferSlice[]>> draws = new ArrayList<>();
                for (int i = 0; i < sections.size(); i++) {
                    Section section = sections.get(i);
                    if (section.getLayer() != layer) {
                        continue;
                    }
                    int slot = i;
                    draws.add(new RenderPass.Draw<>(0, section.getPiece().getVertices(), null, null,
                            0, section.getPiece().getIndexCount(), 0,
                            (uniforms, uploader) -> uploader.upload("ChunkSection", uniforms[slot])));
                }
                if (draws.isEmpty()) {
                    continue;
                }
                pass.setPipeline(layer.pipeline());
                // "ChunkSection" must be named as a per-draw uniform or the pass refuses the draw.
                pass.drawMultipleIndexed(draws, indices, indexType, List.of("ChunkSection"), slices);
                piecesLastFrame += draws.size();
                batchesLastFrame++;
            }
        }
    }

    @Data
    private static class Section {
        private final ChunkSectionLayer layer;
        private final ShipGpu.Piece piece;
        private final Matrix4f modelView;
    }

    private static List<Section> collect(CameraRenderState camera, ChunkSectionLayerGroup group) {
        List<Section> sections = new ArrayList<>();
        Matrix4fc view = camera.viewRotationMatrix;
        Vec3d cameraWorld = new Vec3d(camera.pos.x, camera.pos.y, camera.pos.z);

        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        for (ClientShip ship : ClientFleet.all()) {
            ShipRenderer.Built built = ShipRenderer.built(ship.handle());
            if (built == null || built.getGpu().isEmpty()) {
                continue;
            }
            ShipPose pose = ship.renderPose(partialTick);
            Matrix4f rotation = ShipDrawTransform.hullRotation(pose);
            Vector3f origin = ShipDrawTransform.pivotedOrigin(pose.getTransform(), rotation, pose.getRollCentre());

            for (ChunkSectionLayer layer : group.layers()) {
                for (ShipGpu.Piece piece : built.getGpu().pieces(layer)) {
                    Matrix4f model = ShipDrawTransform.sectionModel(rotation, origin,
                            new Vec3d(piece.getLocalX(), piece.getLocalY(), piece.getLocalZ()),
                            cameraWorld, camera.blockPos.getX(), camera.blockPos.getY(), camera.blockPos.getZ());
                    // Fresh matrix per section: ChunkSectionInfo keeps it by reference until the uniforms are written.
                    sections.add(new Section(layer, piece, new Matrix4f(view).mul(model)));
                }
            }
        }
        return sections;
    }
}
