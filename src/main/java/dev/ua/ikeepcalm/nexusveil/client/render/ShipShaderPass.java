package dev.ua.ikeepcalm.nexusveil.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.nexusveil.client.NvLog;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientFleet;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientShip;
import dev.ua.ikeepcalm.nexusveil.core.physics.ShipPose;
import dev.ua.ikeepcalm.nexusveil.core.render.ShipDrawTransform;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// Under a shader pack the hull goes through the entity path, which Iris draws with the pack's own programs.
public class ShipShaderPass {
    private static final double REACH = 256.0;

    private static boolean broken;

    private ShipShaderPass() {
    }

    public static boolean usable() {
        return !broken;
    }

    public static void fail(String what, Throwable e) {
        if (!broken) {
            broken = true;
            NvLog.LOG.error("Hull rendering under shaders failed ({}); the server draws the ships instead", what, e);
        }
    }

    public static void draw(LevelRenderContext context) {
        if (broken || ClientFleet.size() == 0 || !Shaders.active()) {
            return;
        }
        try {
            Vec3 camera = context.levelState().cameraRenderState.pos;
            float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
            for (ClientShip ship : ClientFleet.all()) {
                ShipRenderer.Built built = ShipRenderer.built(ship.handle());
                if (built == null || built.getCpu() == null) {
                    continue;
                }
                submit(context, built.getCpu(), ship.renderPose(partialTick), camera);
            }
        } catch (RuntimeException | LinkageError e) {
            fail("draw", e);
        }
    }

    private static void submit(LevelRenderContext context, ShipQuads quads, ShipPose pose, Vec3 camera) {
        Matrix4f rotation = ShipDrawTransform.hullRotation(pose);
        Vector3f origin = ShipDrawTransform.pivotedOrigin(pose.getTransform(), rotation, pose.getRollCentre());
        double dx = origin.x - camera.x;
        double dy = origin.y - camera.y;
        double dz = origin.z - camera.z;
        if (dx * dx + dz * dz > REACH * REACH) {
            return;
        }
        PoseStack stack = context.poseStack();
        stack.pushPose();
        stack.translate(dx, dy, dz);
        stack.last().pose().mul(rotation);
        stack.last().normal().mul(new Matrix3f(rotation));
        for (ChunkSectionLayer layer : quads.layers()) {
            context.submitNodeCollector().order(0).submitCustomGeometry(stack, type(layer),
                    (drawn, consumer) -> quads.emit(layer, drawn, consumer));
        }
        stack.popPose();
    }

    private static RenderType type(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> RenderTypes.entitySolid(TextureAtlas.LOCATION_BLOCKS);
            case CUTOUT -> RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS);
            case TRANSLUCENT -> RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
        };
    }
}
