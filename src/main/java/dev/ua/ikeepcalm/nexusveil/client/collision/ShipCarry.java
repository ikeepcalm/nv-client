package dev.ua.ikeepcalm.nexusveil.client.collision;

import dev.ua.ikeepcalm.nexusveil.client.render.ShipRenderer;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientFleet;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientShip;
import dev.ua.ikeepcalm.nexusveil.core.math.MathUtil;
import dev.ua.ikeepcalm.nexusveil.core.math.Vec3d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class ShipCarry {

    private static int frameHandle = -1;
    private static double frameYaw;
    private static long carries;

    private ShipCarry() {
    }

    public static void forget() {
        frameHandle = -1;
    }

    public static long carries() {
        return carries;
    }

    public static void tick(LocalPlayer player) {
        ClientShip ship = aboard(player);
        if (ship == null || ship.previousPose() == null) {
            return;
        }
        Vec3d at = new Vec3d(player.getX(), player.getY(), player.getZ());
        Vec3d carried = ship.pose().toWorld(ship.previousPose().toLocal(at));
        if (carried.sub(at).length() < 1.0e-9) {
            return;
        }
        // setPos, not move(): move() would collide the carry against the deck doing the carrying.
        player.setPos(carried.getX(), carried.getY(), carried.getZ());
        carries++;
    }

    public static void frame(float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        ClientShip ship = player == null ? null : aboard(player);
        if (ship == null) {
            frameHandle = -1;
            return;
        }
        double yaw = ship.renderPose(partialTick).getTransform().getYawRadians();
        if (ship.handle() == frameHandle) {
            float turn = (float) Math.toDegrees(MathUtil.wrapRadians(yaw - frameYaw));
            if (turn != 0.0f) {
                player.setYRot(player.getYRot() + turn);
                player.yRotO += turn;
            }
        }
        frameHandle = ship.handle();
        frameYaw = yaw;
    }

    private static ClientShip aboard(LocalPlayer player) {
        if (player.getAbilities().flying || player.isSpectator() || player.isPassenger()
                || ClientFleet.size() == 0) {
            return null;
        }
        Vec3d at = new Vec3d(player.getX(), player.getY(), player.getZ());
        for (ClientShip ship : ClientFleet.all()) {
            ShipRenderer.Built built = ShipRenderer.built(ship.handle());
            if (built == null || built.getGpu().isEmpty() || ship.model() == null) {
                continue;
            }
            Vec3d local = ship.pose().toLocal(at);
            if (local.getX() >= -0.6 && local.getX() <= ship.model().sizeX() + 0.6
                    && local.getZ() >= -0.6 && local.getZ() <= ship.model().sizeZ() + 0.6
                    && local.getY() >= -1.0 && local.getY() <= ship.model().sizeY() + 2.0) {
                return ship;
            }
        }
        return null;
    }
}
