package dev.ua.ikeepcalm.nexusveil.client.collision;

import dev.ua.ikeepcalm.nexusveil.client.render.ShipRenderer;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientFleet;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientShip;
import dev.ua.ikeepcalm.nexusveil.core.math.Vec3d;
import dev.ua.ikeepcalm.nexusveil.core.physics.ShipPose;
import dev.ua.ikeepcalm.nexusveil.core.voxel.VoxelModel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class ShipCollision {

    public enum Site {
        MOVEMENT,
        QUERY,
        LITHIUM
    }

    private static final int XZ_SUBDIVISIONS = 4;
    private static final int MAX_SHAPES = 4096;
    private static final double CARDINAL_EPSILON = 1.0e-4;
    private static final Map<BlockState, List<AABB>> SHAPE_CACHE = new HashMap<>();
    private static final long[] CALLS = new long[Site.values().length];
    private static final long[] SHAPES = new long[Site.values().length];

    public static long calls(Site site) {
        return CALLS[site.ordinal()];
    }

    public static long shapes(Site site) {
        return SHAPES[site.ordinal()];
    }

    public static void clearCache() {
        SHAPE_CACHE.clear();
    }

    public static List<VoxelShape> shapesFor(Entity entity, AABB query, Site site) {
        if (entity == null || entity != Minecraft.getInstance().player) {
            return List.of();
        }
        CALLS[site.ordinal()]++;
        if (ClientFleet.size() == 0) {
            return List.of();
        }
        List<VoxelShape> shapes = new ArrayList<>();
        for (ClientShip ship : ClientFleet.all()) {
            // Only hulls this client is drawing: colliding with an invisible one is worse than nothing.
            ShipRenderer.Built built = ShipRenderer.built(ship.handle());
            if (built == null || built.getGpu().isEmpty() || ship.model() == null) {
                continue;
            }
            gather(ship, query, shapes);
            if (shapes.size() >= MAX_SHAPES) {
                break;
            }
        }
        SHAPES[site.ordinal()] += shapes.size();
        return shapes;
    }

    private static void gather(ClientShip ship, AABB query, List<VoxelShape> out) {
        VoxelModel model = ship.model();
        ShipPose pose = ship.pose();
        BlockState[] states = ship.blockStates();
        if (states == null) {
            return;
        }

        AABB local = bound(pose::toLocal, query.minX, query.minY, query.minZ, query.maxX, query.maxY, query.maxZ);
        int minX = Math.max(0, (int) Math.floor(local.minX) - 1);
        int minY = Math.max(0, (int) Math.floor(local.minY) - 1);
        int minZ = Math.max(0, (int) Math.floor(local.minZ) - 1);
        int maxX = Math.min(model.sizeX() - 1, (int) Math.floor(local.maxX) + 1);
        int maxY = Math.min(model.sizeY() - 1, (int) Math.floor(local.maxY) + 1);
        int maxZ = Math.min(model.sizeZ() - 1, (int) Math.floor(local.maxZ) + 1);
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return;
        }

        int steps = pose.upright() && isCardinal(pose.getTransform().getYawRadians()) ? 1 : XZ_SUBDIVISIONS;
        // Heeled, one squared-up bound per block bulges a leaning wall into the corridor, so cut in height too.
        int layers = pose.upright() ? 1 : XZ_SUBDIVISIONS;
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    int id = model.paletteIndex(x, y, z);
                    if (id == 0 || id >= states.length) {
                        continue;
                    }
                    for (AABB box : boxesOf(states[id])) {
                        emit(pose, steps, layers, x, y, z, box, query, out);
                        if (out.size() >= MAX_SHAPES) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private static void emit(ShipPose pose, int steps, int layers, int bx, int by, int bz, AABB box,
                             AABB query, List<VoxelShape> out) {
        double spanX = (box.maxX - box.minX) / steps;
        double spanZ = (box.maxZ - box.minZ) / steps;
        double spanY = (box.maxY - box.minY) / layers;
        for (int k = 0; k < layers; k++) {
            double ly0 = by + box.minY + spanY * k;
            for (int i = 0; i < steps; i++) {
                for (int j = 0; j < steps; j++) {
                    double lx0 = bx + box.minX + spanX * i;
                    double lz0 = bz + box.minZ + spanZ * j;
                    AABB world = bound(pose::toWorld, lx0, ly0, lz0, lx0 + spanX, ly0 + spanY, lz0 + spanZ);
                    // noEntityCollision only tests "is the list empty", so shapes missing the query box must not be returned.
                    if (world.intersects(query)) {
                        out.add(Shapes.create(world));
                    }
                }
            }
        }
    }

    private static AABB bound(UnaryOperator<Vec3d> into, double x0, double y0, double z0,
                              double x1, double y1, double z1) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++) {
            Vec3d p = into.apply(new Vec3d((corner & 1) == 0 ? x0 : x1, (corner & 2) == 0 ? y0 : y1,
                    (corner & 4) == 0 ? z0 : z1));
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static BlockState stateAt(Vec3d world) {
        for (ClientShip ship : ClientFleet.all()) {
            ShipRenderer.Built built = ShipRenderer.built(ship.handle());
            BlockState[] states = ship.blockStates();
            if (built == null || built.getGpu().isEmpty() || states == null) {
                continue;
            }
            Vec3d local = ship.pose().toLocal(world);
            int x = (int) Math.floor(local.getX());
            int y = (int) Math.floor(local.getY());
            int z = (int) Math.floor(local.getZ());
            VoxelModel model = ship.model();
            if (x < 0 || y < 0 || z < 0 || x >= model.sizeX() || y >= model.sizeY() || z >= model.sizeZ()) {
                continue;
            }
            int id = model.paletteIndex(x, y, z);
            if (id != 0 && id < states.length) {
                return states[id];
            }
        }
        return null;
    }

    private static boolean isCardinal(double yaw) {
        double quarters = yaw / (Math.PI / 2.0);
        return Math.abs(quarters - Math.round(quarters)) < CARDINAL_EPSILON;
    }

    // EmptyBlockGetter: neighbour-dependent shapes fall back to their default, as the server's CollisionModel does.
    private static List<AABB> boxesOf(BlockState state) {
        return SHAPE_CACHE.computeIfAbsent(state, key -> {
            try {
                return List.copyOf(key.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs());
            } catch (RuntimeException e) {
                return List.of();
            }
        });
    }
}
