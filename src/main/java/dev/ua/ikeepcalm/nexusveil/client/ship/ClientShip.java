package dev.ua.ikeepcalm.nexusveil.client.ship;

import dev.ua.ikeepcalm.nexusveil.client.config.NvSettings;
import dev.ua.ikeepcalm.nexusveil.client.render.ShipBlockStates;
import dev.ua.ikeepcalm.nexusveil.core.math.ShipTransform;
import dev.ua.ikeepcalm.nexusveil.core.math.Vec3d;
import dev.ua.ikeepcalm.nexusveil.core.net.NvShipAdd;
import dev.ua.ikeepcalm.nexusveil.core.net.NvShipMove;
import dev.ua.ikeepcalm.nexusveil.core.physics.HullRoll;
import dev.ua.ikeepcalm.nexusveil.core.physics.ShipPose;
import dev.ua.ikeepcalm.nexusveil.core.voxel.VoxelModel;
import lombok.Data;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;

public class ClientShip {

    private static final double REPLAY_DELAY = 2.0;
    private static final int BUFFERED = 8;

    @Data
    private static class Received {
        private final long tick;
        private final ShipPose pose;
        private final Vec3d velocity;
    }

    private final NvShipAdd identity;
    private VoxelModel model;
    private BlockState[] blockStates;
    private final ArrayDeque<Received> received = new ArrayDeque<>(BUFFERED);
    private double clock = Double.NaN;
    private ShipPose previous;
    private ShipPose current = ShipPose.upright(ShipTransform.IDENTITY);
    private float roll;
    private boolean announced;

    public ClientShip(NvShipAdd identity) {
        this.identity = identity;
    }

    public int handle() {
        return this.identity.getHandle();
    }

    public NvShipAdd identity() {
        return this.identity;
    }

    public VoxelModel model() {
        return this.model;
    }

    public void model(VoxelModel model) {
        this.model = model;
        this.blockStates = null;
    }

    public BlockState[] blockStates() {
        if (this.blockStates == null && this.model != null) {
            this.blockStates = ShipBlockStates.resolve(this.model.palette());
        }
        return this.blockStates;
    }

    public boolean drawable() {
        return this.model != null;
    }

    public boolean takeAnnouncement() {
        if (this.announced || !drawable()) {
            return false;
        }
        this.announced = true;
        return true;
    }

    public void move(NvShipMove.Entry entry, long tick) {
        this.roll = entry.getRoll();
        ShipPose pose = new ShipPose(new ShipTransform(entry.getOrigin(), entry.getYaw()),
                entry.getRoll() * NvSettings.get().getHeelScale(), HullRoll.forward(entry.getBow()), entry.getRollCentre());
        if (!this.received.isEmpty() && tick <= this.received.peekLast().getTick()) {
            this.received.clear();
        }
        if (this.received.size() == BUFFERED) {
            this.received.removeFirst();
        }
        this.received.addLast(new Received(tick, pose, entry.getVelocity()));
    }

    public void latch() {
        if (this.received.isEmpty()) {
            return;
        }
        double target = this.received.peekLast().getTick() - REPLAY_DELAY;
        if (Double.isNaN(this.clock) || Math.abs(target - this.clock) > 6.0) {
            this.clock = target;
        } else {
            this.clock += 1.0 + Math.clamp((target - this.clock) * 0.1, -0.2, 0.2);
        }
        this.previous = this.previous == null ? sample(this.clock) : this.current;
        this.current = sample(this.clock);
    }

    private ShipPose sample(double at) {
        Received before = null;
        for (Received entry : this.received) {
            if (entry.getTick() > at) {
                if (before == null) {
                    return entry.getPose();
                }
                return ShipPose.lerp(before.getPose(), entry.getPose(), (at - before.getTick()) / (entry.getTick() - before.getTick()));
            }
            before = entry;
        }
        return before.getPose();
    }

    public ShipPose pose() {
        return this.current;
    }

    public ShipPose previousPose() {
        return this.previous;
    }

    public ShipPose renderPose(float partialTick) {
        return this.previous == null ? this.current : ShipPose.lerp(this.previous, this.current, partialTick);
    }

    public ShipTransform transform() {
        return this.current.getTransform();
    }

    public float roll() {
        return this.roll;
    }

    public Vec3d velocity() {
        return this.received.isEmpty() ? Vec3d.ZERO : this.received.peekLast().getVelocity();
    }

    public long tick() {
        return this.received.isEmpty() ? 0L : this.received.peekLast().getTick();
    }
}
