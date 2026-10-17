package dev.ua.ikeepcalm.nexusveil.client.fx;

import dev.ua.ikeepcalm.nexusveil.client.ship.ClientShip;
import dev.ua.ikeepcalm.nexusveil.core.math.ShipTransform;
import dev.ua.ikeepcalm.nexusveil.core.math.Vec3d;
import dev.ua.ikeepcalm.nexusveil.core.physics.HullRoll;
import dev.ua.ikeepcalm.nexusveil.core.voxel.VoxelModel;
import lombok.Data;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// White water where the hull actually meets the sea: the outside faces of the waterline slice, not her bounding box.
public class HullWake {
    private static final double CRUISE_SPEED = 0.15;
    private static final double REACH = 160.0;
    private static final double FOAM_PACE = 0.4;
    private static final double BOW_FACING = 0.3;
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Map<Integer, Waterline> CACHE = new HashMap<>();

    @Data
    private static class Contact {
        private final Vec3d point;
        private final Vec3d normal;
    }

    @Data
    private static class Waterline {
        private final VoxelModel model;
        private final int layer;
        private final List<Contact> contacts;
    }

    private HullWake() {
    }

    public static void forget(int handle) {
        CACHE.remove(handle);
    }

    public static void clear() {
        CACHE.clear();
    }

    public static void tick(ClientShip ship, double surface) {
        VoxelModel model = ship.model();
        ShipTransform transform = ship.transform();
        if (model == null || !near(transform.getOrigin())) {
            return;
        }
        int layer = (int) Math.floor(surface - transform.getOrigin().getY());
        if (layer < 0 || layer >= model.sizeY()) {
            return;
        }
        Waterline waterline = CACHE.get(ship.handle());
        if (waterline == null || waterline.getModel() != model || waterline.getLayer() != layer) {
            waterline = new Waterline(model, layer, contacts(model, layer));
            CACHE.put(ship.handle(), waterline);
        }
        Vec3d velocity = ship.velocity();
        double pace = Math.min(Math.hypot(velocity.getX(), velocity.getZ()) / CRUISE_SPEED, 1.5);
        Vec3d forward = transform.directionToWorld(HullRoll.forward(ship.bow()));
        for (Contact contact : waterline.getContacts()) {
            wash(contact, transform, forward, pace, surface);
        }
    }

    // A sailing hull is not driven by a screw, so white water is the bow's alone and only once she has real way on.
    private static void wash(Contact contact, ShipTransform transform, Vec3d forward, double pace, double surface) {
        Vec3d normal = transform.directionToWorld(contact.getNormal());
        double facing = normal.dot(forward);
        double drive = Math.min((pace - FOAM_PACE) / (1.0 - FOAM_PACE), 1.2);
        if (drive <= 0 || facing < BOW_FACING) {
            return;
        }
        double bow = drive * (facing - BOW_FACING) / (1.0 - BOW_FACING);
        Vec3d world = transform.toWorld(contact.getPoint());
        Vec3 at = new Vec3(world.getX(), surface + 0.02, world.getZ());
        Vec3 out = new Vec3(normal.getX(), 0, normal.getZ());

        if (RANDOM.nextDouble() < 0.35 * bow) {
            Minecraft.getInstance().level.addParticle(ParticleTypes.BUBBLE, true, true,
                    at.x + out.x * 0.15, surface - 0.25 - RANDOM.nextDouble() * 0.5, at.z + out.z * 0.15, 0, 0.02, 0);
        }
        if (Vfx.crowded()) {
            return;
        }
        float strength = (float) Math.min(bow, 1.0);
        if (RANDOM.nextDouble() < 0.12 * bow) {
            Vfx.add(new WakeFoam(at.add(out.scale(0.45)), out.scale(0.02 + 0.03 * drive), 0.5f + 0.3f * strength,
                    35 + RANDOM.nextInt(25), 0.45f + 0.45f * strength));
        }
        if (RANDOM.nextDouble() < 0.08 * bow) {
            Vfx.add(new WakeCrest(at.add(out.scale(0.5)), out.scale(0.03 * drive), 3 + RANDOM.nextInt(3),
                    0.16f + 0.1f * strength, 25 + RANDOM.nextInt(15)));
        }
        if (drive > 0.3 && RANDOM.nextDouble() < 0.1 * bow) {
            Vec3 thrown = out.scale(0.6).add(forward.getX() * 0.5, 1.0, forward.getZ() * 0.5);
            Vfx.add(new WakeSpray(at.add(0, 0.1, 0), thrown, 4, 0.22 * drive + 0.08, 0.5, surface));
        }
    }

    // Faces of the slice that open onto water reachable from outside; the hold's own walls are not the waterline.
    private static List<Contact> contacts(VoxelModel model, int layer) {
        int w = model.sizeX() + 2;
        int d = model.sizeZ() + 2;
        boolean[] outside = new boolean[w * d];
        ArrayDeque<int[]> open = new ArrayDeque<>();
        open.add(new int[]{0, 0});
        outside[0] = true;
        while (!open.isEmpty()) {
            int[] cell = open.poll();
            for (int[] side : SIDES) {
                int x = cell[0] + side[0];
                int z = cell[1] + side[1];
                if (x < 0 || z < 0 || x >= w || z >= d || outside[x * d + z] || solid(model, x - 1, layer, z - 1)) {
                    continue;
                }
                outside[x * d + z] = true;
                open.add(new int[]{x, z});
            }
        }
        List<Contact> contacts = new ArrayList<>();
        for (int x = 0; x < model.sizeX(); x++) {
            for (int z = 0; z < model.sizeZ(); z++) {
                if (!model.isSolid(x, layer, z)) {
                    continue;
                }
                for (int[] side : SIDES) {
                    if (outside[(x + 1 + side[0]) * d + (z + 1 + side[1])]) {
                        contacts.add(new Contact(new Vec3d(x + 0.5 + side[0] * 0.5, layer, z + 0.5 + side[1] * 0.5),
                                new Vec3d(side[0], 0, side[1])));
                    }
                }
            }
        }
        return contacts;
    }

    private static boolean solid(VoxelModel model, int x, int y, int z) {
        return x >= 0 && z >= 0 && x < model.sizeX() && z < model.sizeZ() && model.isSolid(x, y, z);
    }

    private static boolean near(Vec3d at) {
        var viewer = Minecraft.getInstance().getCameraEntity();
        if (viewer == null) {
            return false;
        }
        double dx = viewer.getX() - at.getX();
        double dz = viewer.getZ() - at.getZ();
        return dx * dx + dz * dz < REACH * REACH;
    }
}
