package dev.ua.ikeepcalm.nexusveil.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientFleet;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientShip;
import dev.ua.ikeepcalm.nexusveil.core.math.Vec3d;
import dev.ua.ikeepcalm.nexusveil.core.physics.ShipPose;
import dev.ua.ikeepcalm.nexusveil.core.render.ShipDrawTransform;
import dev.ua.ikeepcalm.nexusveil.core.voxel.VoxelModel;
import lombok.Data;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ShipPick {

    @Data
    private static class Hit {
        private final int handle;
        private final BlockPos local;
        private final VoxelShape shape;
    }

    private static Hit hit;

    public static void update(Minecraft minecraft, float partialTick) {
        hit = null;
        LocalPlayer player = minecraft.player;
        if (player == null || ClientFleet.size() == 0) {
            return;
        }
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 reach = eye.add(player.getViewVector(partialTick).scale(player.blockInteractionRange()));
        HitResult vanilla = minecraft.hitResult;
        double best = vanilla == null || vanilla.getType() == HitResult.Type.MISS
                ? Double.POSITIVE_INFINITY : vanilla.getLocation().distanceToSqr(eye);

        Vec3d worldHit = null;
        for (ClientShip ship : ClientFleet.all()) {
            ShipRenderer.Built built = ShipRenderer.built(ship.handle());
            BlockState[] states = ship.blockStates();
            if (built == null || built.getGpu().isEmpty() || states == null) {
                continue;
            }
            ShipPose pose = ship.renderPose(partialTick);
            HullGetter hull = new HullGetter(ship.model(), states);
            BlockHitResult local = hull.clip(new ClipContext(vec(pose.toLocal(of(eye))), vec(pose.toLocal(of(reach))),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (local.getType() != HitResult.Type.BLOCK) {
                continue;
            }
            Vec3d at = pose.toWorld(of(local.getLocation()));
            double distance = vec(at).distanceToSqr(eye);
            if (distance < best) {
                best = distance;
                worldHit = at;
                BlockPos pos = local.getBlockPos();
                hit = new Hit(ship.handle(), pos, hull.getBlockState(pos).getShape(hull, pos));
            }
        }
        if (worldHit != null && !(vanilla instanceof EntityHitResult)) {
            Vec3 at = vec(worldHit);
            minecraft.hitResult = BlockHitResult.miss(at, player.getDirection(), BlockPos.containing(at));
        } else if (worldHit != null) {
            hit = null;
        }
    }

    public static void draw(LevelRenderContext context) {
        Hit current = hit;
        ClientShip ship = current == null ? null : ClientFleet.byHandle(current.getHandle());
        if (ship == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ShipPose pose = ship.renderPose(minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        Vec3 camera = context.levelState().cameraRenderState.pos;
        Matrix4f rotation = ShipDrawTransform.hullRotation(pose);
        Vector3f origin = ShipDrawTransform.pivotedOrigin(pose.getTransform(), rotation, pose.getRollCentre());

        PoseStack stack = context.poseStack();
        stack.pushPose();
        stack.translate(origin.x - camera.x, origin.y - camera.y, origin.z - camera.z);
        stack.mulPose(rotation);
        stack.translate(current.getLocal().getX(), current.getLocal().getY(), current.getLocal().getZ());
        context.submitNodeCollector().submitShapeOutline(stack, current.getShape(), RenderTypes.lines(),
                ARGB.black(102), context.gameRenderer().gameRenderState().windowRenderState.appropriateLineWidth,
                false);
        stack.popPose();
    }

    private static Vec3d of(Vec3 v) {
        return new Vec3d(v.x, v.y, v.z);
    }

    private static Vec3 vec(Vec3d v) {
        return new Vec3(v.getX(), v.getY(), v.getZ());
    }

    @Data
    private static class HullGetter implements BlockGetter {
        private final VoxelModel model;
        private final BlockState[] states;

        @Override
        public BlockState getBlockState(BlockPos pos) {
            if (pos.getX() < 0 || pos.getY() < 0 || pos.getZ() < 0 || pos.getX() >= this.model.sizeX()
                    || pos.getY() >= this.model.sizeY() || pos.getZ() >= this.model.sizeZ()) {
                return Blocks.AIR.defaultBlockState();
            }
            int id = this.model.paletteIndex(pos.getX(), pos.getY(), pos.getZ());
            return id == 0 || id >= this.states.length ? Blocks.AIR.defaultBlockState() : this.states[id];
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return Fluids.EMPTY.defaultFluidState();
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public int getHeight() {
            return this.model.sizeY();
        }

        @Override
        public int getMinY() {
            return 0;
        }
    }
}
